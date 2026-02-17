package com.example.petmarket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DatabaseMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateAllVarcharToNvarchar();
        restoreCategoryIcons();
        restoreSubcategoryIcons();
        restoreProductImages();
    }

    private void migrateAllVarcharToNvarchar() {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE DATA_TYPE = 'varchar' AND TABLE_SCHEMA = 'dbo'"
            );

            for (Map<String, Object> col : columns) {
                String table = (String) col.get("TABLE_NAME");
                String column = (String) col.get("COLUMN_NAME");
                Object maxLenObj = col.get("CHARACTER_MAXIMUM_LENGTH");
                String nullable = (String) col.get("IS_NULLABLE");

                int maxLen = (maxLenObj != null) ? ((Number) maxLenObj).intValue() : 255;
                String lenStr = (maxLen == -1) ? "max" : String.valueOf(maxLen);
                String nullConstraint = "YES".equals(nullable) ? "NULL" : "NOT NULL";

                try {
                    jdbcTemplate.execute(
                            "ALTER TABLE [" + table + "] ALTER COLUMN [" + column + "] nvarchar(" + lenStr + ") " + nullConstraint
                    );
                    log.info("Migrated {}.{} varchar({}) -> nvarchar({})", table, column, lenStr, lenStr);
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("5074")) {
                        migrateColumnWithDependencies(table, column, lenStr, nullConstraint);
                    } else {
                        log.warn("Could not migrate {}.{}: {}", table, column, e.getMessage());
                    }
                }
            }

            if (columns.isEmpty()) {
                log.info("No varchar columns to migrate - all already nvarchar");
            }
        } catch (Exception e) {
            log.warn("Could not query varchar columns: {}", e.getMessage());
        }
    }

    private void migrateColumnWithDependencies(String table, String column, String lenStr, String nullConstraint) {
        List<String[]> droppedIndexes = new ArrayList<>();
        List<String[]> droppedCheckConstraints = new ArrayList<>();
        List<String> droppedDefaultConstraints = new ArrayList<>();

        try {
            // 1. Find and drop UNIQUE indexes/constraints that depend on this column
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                    "SELECT DISTINCT i.name AS index_name, i.is_unique, i.is_unique_constraint " +
                            "FROM sys.indexes i " +
                            "JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id " +
                            "JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id " +
                            "WHERE i.object_id = OBJECT_ID(?) AND c.name = ? AND i.is_primary_key = 0 AND i.name IS NOT NULL",
                    table, column
            );

            for (Map<String, Object> idx : indexes) {
                String indexName = (String) idx.get("index_name");
                boolean isUnique = (boolean) idx.get("is_unique");
                boolean isUniqueConstraint = (boolean) idx.get("is_unique_constraint");

                // Get all columns in this index (for recreation)
                List<Map<String, Object>> indexCols = jdbcTemplate.queryForList(
                        "SELECT c.name " +
                                "FROM sys.index_columns ic " +
                                "JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id " +
                                "WHERE ic.object_id = OBJECT_ID(?) AND ic.index_id = " +
                                "(SELECT index_id FROM sys.indexes WHERE object_id = OBJECT_ID(?) AND name = ?) " +
                                "ORDER BY ic.key_ordinal",
                        table, table, indexName
                );
                String colList = indexCols.stream()
                        .map(c -> "[" + c.get("name") + "]")
                        .collect(Collectors.joining(", "));

                // Drop
                if (isUniqueConstraint) {
                    jdbcTemplate.execute("ALTER TABLE [" + table + "] DROP CONSTRAINT [" + indexName + "]");
                } else {
                    jdbcTemplate.execute("DROP INDEX [" + indexName + "] ON [" + table + "]");
                }

                // Save for recreation: [indexName, isUnique, isUniqueConstraint, colList]
                droppedIndexes.add(new String[]{indexName, String.valueOf(isUnique), String.valueOf(isUniqueConstraint), colList});
                log.debug("Dropped index/constraint [{}] on [{}] for migration", indexName, table);
            }

            // 2. Find and drop CHECK constraints on this column
            List<Map<String, Object>> checkConstraints = jdbcTemplate.queryForList(
                    "SELECT cc.name, cc.definition " +
                            "FROM sys.check_constraints cc " +
                            "JOIN sys.columns c ON cc.parent_object_id = c.object_id AND cc.parent_column_id = c.column_id " +
                            "WHERE cc.parent_object_id = OBJECT_ID(?) AND c.name = ?",
                    table, column
            );

            for (Map<String, Object> cc : checkConstraints) {
                String ccName = (String) cc.get("name");
                String ccDef = (String) cc.get("definition");
                jdbcTemplate.execute("ALTER TABLE [" + table + "] DROP CONSTRAINT [" + ccName + "]");
                droppedCheckConstraints.add(new String[]{ccName, ccDef});
                log.debug("Dropped check constraint [{}] on [{}] for migration", ccName, table);
            }

            // 3. Find and drop DEFAULT constraints on this column
            List<Map<String, Object>> defaultConstraints = jdbcTemplate.queryForList(
                    "SELECT dc.name, dc.definition " +
                            "FROM sys.default_constraints dc " +
                            "JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id " +
                            "WHERE dc.parent_object_id = OBJECT_ID(?) AND c.name = ?",
                    table, column
            );

            for (Map<String, Object> dc : defaultConstraints) {
                String dcName = (String) dc.get("name");
                jdbcTemplate.execute("ALTER TABLE [" + table + "] DROP CONSTRAINT [" + dcName + "]");
                droppedDefaultConstraints.add(dcName);
                log.debug("Dropped default constraint [{}] on [{}] for migration", dcName, table);
            }

            // 4. ALTER COLUMN
            jdbcTemplate.execute(
                    "ALTER TABLE [" + table + "] ALTER COLUMN [" + column + "] nvarchar(" + lenStr + ") " + nullConstraint
            );

            // 5. Recreate indexes
            for (String[] idx : droppedIndexes) {
                String indexName = idx[0];
                boolean isUnique = Boolean.parseBoolean(idx[1]);
                boolean isUniqueConstraint = Boolean.parseBoolean(idx[2]);
                String colList = idx[3];

                if (isUniqueConstraint) {
                    jdbcTemplate.execute(
                            "ALTER TABLE [" + table + "] ADD CONSTRAINT [" + indexName + "] UNIQUE (" + colList + ")"
                    );
                } else {
                    String unique = isUnique ? "UNIQUE " : "";
                    jdbcTemplate.execute(
                            "CREATE " + unique + "INDEX [" + indexName + "] ON [" + table + "] (" + colList + ")"
                    );
                }
            }

            // 6. Recreate check constraints
            for (String[] cc : droppedCheckConstraints) {
                jdbcTemplate.execute(
                        "ALTER TABLE [" + table + "] ADD CONSTRAINT [" + cc[0] + "] CHECK " + cc[1]
                );
            }

            // 7. Recreate default constraints
            for (String dcName : droppedDefaultConstraints) {
                // Default constraints reference the column but we don't have the original definition easily
                // They will be recreated by Hibernate on next schema update
                log.debug("Default constraint [{}] will be recreated by Hibernate", dcName);
            }

            log.info("Migrated {}.{} varchar({}) -> nvarchar({}) (dropped/recreated {} indexes, {} check constraints)",
                    table, column, lenStr, lenStr, droppedIndexes.size(), droppedCheckConstraints.size());

        } catch (Exception e) {
            log.warn("Could not migrate {}.{} with dependencies: {}", table, column, e.getMessage());
            // Try to recreate anything we dropped before the failure
            tryRecreateDroppedObjects(table, droppedIndexes, droppedCheckConstraints);
        }
    }

    private void tryRecreateDroppedObjects(String table, List<String[]> droppedIndexes, List<String[]> droppedCheckConstraints) {
        for (String[] idx : droppedIndexes) {
            try {
                String indexName = idx[0];
                boolean isUniqueConstraint = Boolean.parseBoolean(idx[2]);
                boolean isUnique = Boolean.parseBoolean(idx[1]);
                String colList = idx[3];

                if (isUniqueConstraint) {
                    jdbcTemplate.execute("ALTER TABLE [" + table + "] ADD CONSTRAINT [" + indexName + "] UNIQUE (" + colList + ")");
                } else {
                    String unique = isUnique ? "UNIQUE " : "";
                    jdbcTemplate.execute("CREATE " + unique + "INDEX [" + indexName + "] ON [" + table + "] (" + colList + ")");
                }
            } catch (Exception ignored) {
                // Best effort recovery
            }
        }
        for (String[] cc : droppedCheckConstraints) {
            try {
                jdbcTemplate.execute("ALTER TABLE [" + table + "] ADD CONSTRAINT [" + cc[0] + "] CHECK " + cc[1]);
            } catch (Exception ignored) {
                // Best effort recovery
            }
        }
    }

    private void restoreCategoryIcons() {
        updateIconBySlug("categories", "psy", "\uD83D\uDC15");
        updateIconBySlug("categories", "koty", "\uD83D\uDC08");
        updateIconBySlug("categories", "gryzonie", "\uD83D\uDC39");
        updateIconBySlug("categories", "ptaki", "\uD83E\uDD9C");
        updateIconBySlug("categories", "ryby", "\uD83D\uDC20");
        updateIconBySlug("categories", "gady", "\uD83E\uDD8E");
    }

    private void restoreSubcategoryIcons() {
        updateIconBySlug("subcategories", "psy-karma", "\uD83E\uDDB4");
        updateIconBySlug("subcategories", "psy-zabawki", "\uD83C\uDFBE");
        updateIconBySlug("subcategories", "psy-akcesoria", "\uD83E\uDDAE");
        updateIconBySlug("subcategories", "psy-suplementy", "\uD83D\uDC8A");

        updateIconBySlug("subcategories", "koty-karma", "\uD83D\uDC31");
        updateIconBySlug("subcategories", "koty-zabawki", "\uD83E\uDDF6");
        updateIconBySlug("subcategories", "koty-akcesoria", "\uD83D\uDC3E");
        updateIconBySlug("subcategories", "koty-suplementy", "\uD83D\uDC8A");

        updateIconBySlug("subcategories", "gryzonie-karma", "\uD83C\uDF3E");
        updateIconBySlug("subcategories", "gryzonie-zabawki", "\uD83C\uDFA1");
        updateIconBySlug("subcategories", "gryzonie-akcesoria", "\uD83C\uDFE0");
        updateIconBySlug("subcategories", "gryzonie-suplementy", "\uD83D\uDC8A");

        updateIconBySlug("subcategories", "ptaki-karma", "\uD83C\uDF3E");
        updateIconBySlug("subcategories", "ptaki-zabawki", "\uD83E\uDEB6");
        updateIconBySlug("subcategories", "ptaki-akcesoria", "\uD83C\uDFE0");
        updateIconBySlug("subcategories", "ptaki-suplementy", "\uD83D\uDC8A");

        updateIconBySlug("subcategories", "ryby-karma", "\uD83D\uDC1F");
        updateIconBySlug("subcategories", "ryby-akcesoria", "\uD83C\uDFCA");
        updateIconBySlug("subcategories", "ryby-suplementy", "\uD83D\uDC8A");

        updateIconBySlug("subcategories", "gady-karma", "\uD83E\uDD97");
        updateIconBySlug("subcategories", "gady-akcesoria", "\uD83C\uDFDC\uFE0F");
        updateIconBySlug("subcategories", "gady-suplementy", "\uD83D\uDC8A");
    }

    private void restoreProductImages() {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE p SET p.image_url = s.icon " +
                            "FROM products p " +
                            "JOIN subcategories s ON p.subcategory_id = s.id " +
                            "WHERE (p.image_url IS NULL OR p.image_url LIKE '%?%' OR LEN(p.image_url) = 0) " +
                            "AND p.image_url NOT LIKE '/%' AND p.image_url NOT LIKE 'http%'"
            );
            log.info("Restored {} product image emojis from subcategory icons", updated);
        } catch (Exception e) {
            log.warn("Could not restore product images: {}", e.getMessage());
        }
    }

    private void updateIconBySlug(String table, String slug, String icon) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE " + table + " SET icon = ? WHERE slug = ? AND (icon IS NULL OR icon LIKE '%?%' OR LEN(icon) = 0)",
                    icon, slug
            );
            if (updated > 0) {
                log.info("Restored icon for {}.slug={}", table, slug);
            }
        } catch (Exception e) {
            log.warn("Could not restore icon for {}.slug={}: {}", table, slug, e.getMessage());
        }
    }
}
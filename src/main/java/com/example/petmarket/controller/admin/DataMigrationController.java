package com.example.petmarket.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.Clob;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/data")
@RequiredArgsConstructor
public class DataMigrationController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private String q(String name) {
        return "\"" + name + "\"";
    }

    @GetMapping
    public String showMigrationPage(org.springframework.ui.Model model) {
        List<String> tables = getTableNames();
        Map<String, Long> tableInfo = new LinkedHashMap<>();
        for (String table : tables) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + q(table), Long.class);
                tableInfo.put(table, count != null ? count : 0L);
            } catch (Exception e) {
                tableInfo.put(table, -1L);
            }
        }
        model.addAttribute("tableInfo", tableInfo);

        String dbUrl = "";
        try {
            dbUrl = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection().getMetaData().getURL();
        } catch (Exception ignored) {}
        model.addAttribute("dbUrl", dbUrl);

        return "admin/data-migration";
    }

    @GetMapping("/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportData() throws Exception {
        List<String> tables = getTableNames();
        log.info("=== EKSPORT: znaleziono {} tabel: {} ===", tables.size(), tables);

        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();

        for (String table : tables) {
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + q(table));
                log.info("Tabela {}: {} wierszy", table, rows.size());
                List<Map<String, Object>> normalized = rows.stream()
                        .map(this::normalizeRow)
                        .collect(Collectors.toList());
                if (!normalized.isEmpty()) {
                    data.put(table.toLowerCase(), normalized);
                }
            } catch (Exception e) {
                log.warn("Pomijam tabele {}: {}", table, e.getMessage());
            }
        }

        log.info("=== EKSPORT: {} tabel z danymi ===", data.size());

        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zoo-data-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @PostMapping("/import")
    public String importData(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            Map<String, List<Map<String, Object>>> data = objectMapper.readValue(
                    file.getInputStream(), new TypeReference<>() {});

            disableForeignKeys();

            // Delete all existing data from all database tables (retry to handle FK order)
            List<String> allTables = getTableNames();
            for (int attempt = 0; attempt < 3; attempt++) {
                List<String> failed = new ArrayList<>();
                for (String table : allTables) {
                    try {
                        jdbcTemplate.execute("DELETE FROM " + q(table));
                    } catch (Exception e) {
                        failed.add(table);
                    }
                }
                if (failed.isEmpty()) break;
                allTables = failed;
            }

            int totalRows = 0;
            List<String> importedTables = new ArrayList<>();

            for (var entry : data.entrySet()) {
                String table = entry.getKey();
                List<Map<String, Object>> rows = entry.getValue();
                if (rows == null || rows.isEmpty()) continue;

                // SQL Server: allow explicit ID insert
                tryExecute("SET IDENTITY_INSERT " + q(table) + " ON");

                for (Map<String, Object> row : rows) {
                    try {
                        insertRow(table, row);
                        totalRows++;
                    } catch (Exception e) {
                        log.warn("Blad importu wiersza do {}: {}", table, e.getMessage());
                    }
                }

                tryExecute("SET IDENTITY_INSERT " + q(table) + " OFF");
                importedTables.add(table);
            }

            enableForeignKeys();
            resetSequences(data.keySet());

            redirectAttributes.addFlashAttribute("success",
                    "Zaimportowano " + totalRows + " wierszy do " + importedTables.size() + " tabel");

        } catch (Exception e) {
            log.error("Blad importu: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Blad importu: " + e.getMessage());
        }

        return "redirect:/admin/data";
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            if (value instanceof java.sql.Timestamp ts) {
                value = ts.toLocalDateTime().toString();
            } else if (value instanceof java.sql.Date d) {
                value = d.toLocalDate().toString();
            } else if (value instanceof Clob clob) {
                try { value = clob.getSubString(1, (int) clob.length()); }
                catch (Exception e) { value = null; }
            }
            normalized.put(key.toLowerCase(), value);
        });
        return normalized;
    }

    private void insertRow(String table, Map<String, Object> row) {
        String columns = row.keySet().stream().map(this::q).collect(Collectors.joining(", "));
        String placeholders = row.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        jdbcTemplate.update(
                "INSERT INTO " + q(table) + " (" + columns + ") VALUES (" + placeholders + ")",
                row.values().toArray());
    }

    private List<String> getTableNames() {
        List<String> tables;
        // H2 (2.x uses 'BASE TABLE', 1.x uses 'TABLE')
        try {
            tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE IN ('TABLE', 'BASE TABLE')",
                    String.class);
            if (!tables.isEmpty()) return tables;
        } catch (Exception e) { /* not H2 */ }
        // SQL Server / Azure SQL
        try {
            tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA = 'dbo'",
                    String.class);
            if (!tables.isEmpty()) return tables;
        } catch (Exception e) { /* not SQL Server */ }
        // MySQL
        return jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'",
                String.class);
    }

    private void disableForeignKeys() {
        tryExecute("SET REFERENTIAL_INTEGRITY FALSE");              // H2
        disableSqlServerForeignKeys();                              // SQL Server
    }

    private void enableForeignKeys() {
        tryExecute("SET REFERENTIAL_INTEGRITY TRUE");               // H2
        enableSqlServerForeignKeys();                               // SQL Server
    }

    private void disableSqlServerForeignKeys() {
        try {
            List<String> tables = getTableNames();
            for (String table : tables) {
                tryExecute("ALTER TABLE " + q(table) + " NOCHECK CONSTRAINT ALL");
            }
        } catch (Exception ignored) {}
    }

    private void enableSqlServerForeignKeys() {
        try {
            List<String> tables = getTableNames();
            for (String table : tables) {
                tryExecute("ALTER TABLE " + q(table) + " WITH CHECK CHECK CONSTRAINT ALL");
            }
        } catch (Exception ignored) {}
    }

    private void tryExecute(String sql) {
        try { jdbcTemplate.execute(sql); }
        catch (Exception ignored) {}
    }

    private void resetSequences(Set<String> tables) {
        for (String table : tables) {
            // SQL Server: reseed identity
            try {
                Long maxId = jdbcTemplate.queryForObject(
                        "SELECT ISNULL(MAX(id), 0) FROM " + q(table), Long.class);
                jdbcTemplate.execute("DBCC CHECKIDENT ('" + table + "', RESEED, " + maxId + ")");
            } catch (Exception ignored) {}
        }
    }
}
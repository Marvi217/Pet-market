package com.example.petmarket.service.export;

import com.example.petmarket.entity.Product;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

@Component
public class CsvProductExporter implements ProductExporter {

    @Override
    public String getFormat() {return "CSV";}

    @Override
    public String getContentType() {
        return "text/csv; charset=UTF-8";
    }

    @Override
    public String getFileExtension() {
        return ".csv";
    }

    @Override
    public void export(List<Product> products, Writer writer) {
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.print("\uFEFF");
        printWriter.println("ID,Nazwa,SKU,Kategoria,Podkategoria,Marka,Cena,Cena promocyjna,Stan magazynowy,Status,Opis");

        for (Product product : products) {
            printWriter.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                    product.getId(),
                    escapeCSV(product.getName()),
                    escapeCSV(product.getSku()),
                    escapeCSV(product.getCategory() != null ? product.getCategory().getName() : ""),
                    escapeCSV(product.getSubcategory() != null ? product.getSubcategory().getName() : ""),
                    escapeCSV(product.getBrand() != null ? product.getBrand().getName() : ""),
                    product.getPrice() != null ? product.getPrice().toString() : "",
                    product.getDiscountedPrice() != null ? product.getDiscountedPrice().toString() : "",
                    product.getStockQuantity() != null ? product.getStockQuantity() : 0,
                    product.getStatus() != null ? product.getStatus().name() : "",
                    escapeCSV(sanitizeDescription(product.getDescription()))
            ));
        }

        printWriter.flush();
    }

    private String sanitizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description
                .replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
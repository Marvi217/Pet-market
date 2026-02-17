package com.example.petmarket.service.export;

import com.example.petmarket.entity.Product;

import java.io.Writer;
import java.util.List;

public interface ProductExporter {

    String getFormat();
    String getContentType();
    String getFileExtension();
    void export(List<Product> products, Writer writer);
}
package com.example.petmarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.nio.file.Paths;
import java.util.Locale;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.products.dir:uploads/products/}")
    private String productsUploadDir;

    @Value("${file.upload.categories.dir:uploads/categories/}")
    private String categoriesUploadDir;

    @Value("${file.upload.brands.dir:uploads/brands/}")
    private String brandsUploadDir;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/error/403").setViewName("error/403");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String productsPath = Paths.get(productsUploadDir).toAbsolutePath().normalize().toUri().toString();
        String categoriesPath = Paths.get(categoriesUploadDir).toAbsolutePath().normalize().toUri().toString();
        String brandsPath = Paths.get(brandsUploadDir).toAbsolutePath().normalize().toUri().toString();

        registry.addResourceHandler("/products/**")
                .addResourceLocations(productsPath);

        registry.addResourceHandler("/categories/**")
                .addResourceLocations(categoriesPath);

        registry.addResourceHandler("/brands/**")
                .addResourceLocations(brandsPath);
    }


    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(new Locale("pl"));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
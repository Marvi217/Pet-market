package com.example.petmarket.service.tracking;

import com.example.petmarket.enums.DeliveryMethod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrackingNumberService {

    private final Map<DeliveryMethod, TrackingNumberGenerator> generators;
    private final TrackingNumberGenerator defaultGenerator;

    public TrackingNumberService(List<TrackingNumberGenerator> generatorList) {
        this.generators = generatorList.stream()
                .filter(g -> g.getSupportedMethod() != null)
                .collect(Collectors.toMap(
                        TrackingNumberGenerator::getSupportedMethod,
                        Function.identity()
                ));

        this.defaultGenerator = generatorList.stream()
                .filter(g -> g.getSupportedMethod() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Domyślny generator nie znaleziony"));
    }

    public String generateTrackingNumber(DeliveryMethod deliveryMethod) {
        if (deliveryMethod == null) {
            return defaultGenerator.generate();
        }
        return generators.getOrDefault(deliveryMethod, defaultGenerator).generate();
    }
}
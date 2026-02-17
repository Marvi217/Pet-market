package com.example.petmarket.service.product;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ProfanityFilterService {

    private static final Map<String, String> PROFANITY_FILTER = Map.ofEntries(
            Map.entry("chuj", "c***"),
            Map.entry("kurwa", "k***a"),
            Map.entry("kurwy", "k***y"),
            Map.entry("kurwą", "k***ą"),
            Map.entry("kurwie", "k***ie"),
            Map.entry("kurwę", "k***ę"),
            Map.entry("pierdol", "p*****l"),
            Map.entry("pierdolić", "p*****lić"),
            Map.entry("pierdolę", "p*****lę"),
            Map.entry("pierdoli", "p*****li"),
            Map.entry("jebać", "j***ć"),
            Map.entry("jebany", "j***ny"),
            Map.entry("jebana", "j***na"),
            Map.entry("jebane", "j***ne"),
            Map.entry("jebie", "j***e"),
            Map.entry("skurwysyn", "s*********n"),
            Map.entry("skurwiel", "s******l"),
            Map.entry("dupa", "d**a"),
            Map.entry("dupą", "d**ą"),
            Map.entry("dupę", "d**ę"),
            Map.entry("gówno", "g***o"),
            Map.entry("gówna", "g***a"),
            Map.entry("gównem", "g***em"),
            Map.entry("cholera", "ch***ra"),
            Map.entry("cholerny", "ch***rny"),
            Map.entry("cholerna", "ch***rna"),
            Map.entry("suka", "s**a"),
            Map.entry("suki", "s**i"),
            Map.entry("suką", "s**ą")
    );

    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String filteredText = text;
        for (Map.Entry<String, String> entry : PROFANITY_FILTER.entrySet()) {
            filteredText = filteredText.replaceAll(
                    "(?i)" + Pattern.quote(entry.getKey()),
                    entry.getValue()
            );
        }
        return filteredText;
    }
}
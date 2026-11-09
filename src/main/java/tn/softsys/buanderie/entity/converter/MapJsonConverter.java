package tn.softsys.buanderie.entity.converter;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MapJsonConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> valeur) {
        try {
            return valeur == null ? null : MAPPER.writeValueAsString(valeur);
        } catch (Exception e) {
            throw new IllegalArgumentException("Correspondance des champs invalide", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String colonne) {
        try {
            if (colonne == null || colonne.isBlank()) {
                return new LinkedHashMap<>();
            }
            return MAPPER.readValue(colonne, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
package com.oneff.customer.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oneff.customer.core.model.AttributeValue;
import com.oneff.customer.core.model.Attributes;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JPA converter that serializes/deserializes {@link Attributes} to/from
 * a JSON string for PostgreSQL JSONB storage.
 *
 * <p>The JSON structure includes the schema version and a map of typed values:
 * <pre>
 * {
 *   "schemaVersion": 1,
 *   "values": {
 *     "loyaltyTier": { "type": "STRING", "value": "GOLD" },
 *     "signupYear":  { "type": "INTEGER", "value": 2024 }
 *   }
 * }
 * </pre>
 */
@Converter
class AttributesJsonConverter implements AttributeConverter<String, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // Already JSON string — pass through
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Already JSON string — pass through
        return dbData;
    }

    /**
     * Serializes an {@link Attributes} domain object to JSON string.
     */
    static String toJson(Attributes attributes) {
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("schemaVersion", attributes.schemaVersion());

            Map<String, Map<String, Object>> valuesMap = new LinkedHashMap<>();
            for (var entry : attributes.values().entrySet()) {
                valuesMap.put(entry.getKey(), serializeValue(entry.getValue()));
            }
            envelope.put("values", valuesMap);

            return MAPPER.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize attributes to JSON", e);
        }
    }

    /**
     * Deserializes a JSON string back to an {@link Attributes} domain object.
     */
    @SuppressWarnings("unchecked")
    static Attributes fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Attributes.empty();
        }
        try {
            Map<String, Object> envelope = MAPPER.readValue(json,
                new TypeReference<Map<String, Object>>() {});

            int schemaVersion = ((Number) envelope.getOrDefault("schemaVersion", 1)).intValue();
            Map<String, Map<String, Object>> valuesMap =
                (Map<String, Map<String, Object>>) envelope.getOrDefault("values", Map.of());

            Map<String, AttributeValue> attributes = new LinkedHashMap<>();
            for (var entry : valuesMap.entrySet()) {
                attributes.put(entry.getKey(), deserializeValue(entry.getValue()));
            }

            return Attributes.of(schemaVersion, attributes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize attributes from JSON", e);
        }
    }

    private static Map<String, Object> serializeValue(AttributeValue value) {
        Map<String, Object> map = new LinkedHashMap<>();
        switch (value) {
            case AttributeValue.StringValue v -> {
                map.put("type", "STRING");
                map.put("value", v.value());
            }
            case AttributeValue.IntegerValue v -> {
                map.put("type", "INTEGER");
                map.put("value", v.value());
            }
            case AttributeValue.BooleanValue v -> {
                map.put("type", "BOOLEAN");
                map.put("value", v.value());
            }
            case AttributeValue.DecimalValue v -> {
                map.put("type", "DECIMAL");
                map.put("value", v.value().toPlainString());
            }
            case AttributeValue.DateValue v -> {
                map.put("type", "DATE");
                map.put("value", v.value().toString());
            }
        }
        return map;
    }

    private static AttributeValue deserializeValue(Map<String, Object> map) {
        String type = (String) map.get("type");
        Object raw = map.get("value");

        return switch (type) {
            case "STRING" -> new AttributeValue.StringValue((String) raw);
            case "INTEGER" -> new AttributeValue.IntegerValue(((Number) raw).intValue());
            case "BOOLEAN" -> new AttributeValue.BooleanValue((Boolean) raw);
            case "DECIMAL" -> new AttributeValue.DecimalValue(new java.math.BigDecimal(raw.toString()));
            case "DATE" -> new AttributeValue.DateValue(java.time.LocalDate.parse(raw.toString()));
            default -> new AttributeValue.StringValue(raw.toString());
        };
    }
}

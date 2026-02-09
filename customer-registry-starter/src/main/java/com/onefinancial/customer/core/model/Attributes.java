package com.onefinancial.customer.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable typed wrapper around the extensible customer attributes map.
 *
 * <p>Each instance carries a {@code schemaVersion} indicating the format generation,
 * enabling forward-compatible JSONB migrations. Unknown keys encountered during
 * deserialization are preserved — they are never silently dropped.</p>
 *
 * <p>Usage:
 * <pre>
 * Attributes attrs = Attributes.empty()
 *     .with("loyaltyTier", new StringValue("GOLD"))
 *     .with("signupYear", new IntegerValue(2024));
 *
 * String tier = attrs.get("loyaltyTier", StringValue.class).value();
 * </pre>
 */
public final class Attributes {

    private final int schemaVersion;
    private final Map<String, AttributeValue> values;

    private Attributes(int schemaVersion, Map<String, AttributeValue> values) {
        this.schemaVersion = schemaVersion;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static Attributes empty() {
        return new Attributes(1, Map.of());
    }

    public static Attributes of(int schemaVersion, Map<String, AttributeValue> values) {
        Objects.requireNonNull(values, "Attribute values map must not be null");
        return new Attributes(schemaVersion, values);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public Map<String, AttributeValue> values() {
        return values;
    }

    /**
     * Returns the attribute value for the given key, cast to the expected type.
     *
     * @throws IllegalArgumentException if the key is missing or the value is not of the expected type
     */
    @SuppressWarnings("unchecked")
    public <T extends AttributeValue> T get(String key, Class<T> expectedType) {
        AttributeValue value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Attribute '%s' not found".formatted(key));
        }
        if (!expectedType.isInstance(value)) {
            throw new IllegalArgumentException(
                "Attribute '%s' is %s, expected %s".formatted(
                    key, value.getClass().getSimpleName(), expectedType.getSimpleName()));
        }
        return (T) value;
    }

    /**
     * Returns a new {@code Attributes} instance with the given key-value pair added or replaced.
     * All other entries are preserved. The schema version is unchanged.
     */
    public Attributes with(String key, AttributeValue value) {
        Objects.requireNonNull(key, "Attribute key must not be null");
        Objects.requireNonNull(value, "Attribute value must not be null");
        var newValues = new LinkedHashMap<>(this.values);
        newValues.put(key, value);
        return new Attributes(this.schemaVersion, newValues);
    }

    /**
     * Returns a new {@code Attributes} instance with the specified schema version.
     */
    public Attributes withSchemaVersion(int newVersion) {
        return new Attributes(newVersion, this.values);
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public int size() {
        return values.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Attributes other)) return false;
        return schemaVersion == other.schemaVersion && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, values);
    }

    @Override
    public String toString() {
        return "Attributes{schemaVersion=%d, size=%d}".formatted(schemaVersion, values.size());
    }
}

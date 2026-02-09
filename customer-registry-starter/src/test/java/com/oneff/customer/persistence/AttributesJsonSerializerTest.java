package com.oneff.customer.persistence;

import com.oneff.customer.core.model.AttributeValue;
import com.oneff.customer.core.model.Attributes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributesJsonSerializerTest {

    @Nested
    @DisplayName("Round-trip serialization")
    class RoundTrip {

        @Test
        @DisplayName("should round-trip STRING attribute value")
        void roundTripStringValue() {
            Attributes original = Attributes.empty()
                .with("tier", new AttributeValue.StringValue("GOLD"));

            String json = AttributesJsonSerializer.toJson(original);
            Attributes deserialized = AttributesJsonSerializer.fromJson(json);

            assertThat(deserialized.size()).isEqualTo(1);
            assertThat(deserialized.get("tier", AttributeValue.StringValue.class).value())
                .isEqualTo("GOLD");
            assertThat(deserialized.schemaVersion()).isEqualTo(original.schemaVersion());
        }

        @Test
        @DisplayName("should round-trip INTEGER attribute value")
        void roundTripIntegerValue() {
            Attributes original = Attributes.empty()
                .with("signupYear", new AttributeValue.IntegerValue(2024));

            String json = AttributesJsonSerializer.toJson(original);
            Attributes deserialized = AttributesJsonSerializer.fromJson(json);

            assertThat(deserialized.get("signupYear", AttributeValue.IntegerValue.class).value())
                .isEqualTo(2024);
        }

        @Test
        @DisplayName("should round-trip BOOLEAN attribute value")
        void roundTripBooleanValue() {
            Attributes original = Attributes.empty()
                .with("vip", new AttributeValue.BooleanValue(true));

            String json = AttributesJsonSerializer.toJson(original);
            Attributes deserialized = AttributesJsonSerializer.fromJson(json);

            assertThat(deserialized.get("vip", AttributeValue.BooleanValue.class).value())
                .isTrue();
        }

        @Test
        @DisplayName("should round-trip DECIMAL attribute value")
        void roundTripDecimalValue() {
            Attributes original = Attributes.empty()
                .with("balance", new AttributeValue.DecimalValue(new BigDecimal("1234.56")));

            String json = AttributesJsonSerializer.toJson(original);
            Attributes deserialized = AttributesJsonSerializer.fromJson(json);

            assertThat(deserialized.get("balance", AttributeValue.DecimalValue.class).value())
                .isEqualByComparingTo(new BigDecimal("1234.56"));
        }

        @Test
        @DisplayName("should round-trip DATE attribute value")
        void roundTripDateValue() {
            LocalDate date = LocalDate.of(2025, 6, 15);
            Attributes original = Attributes.empty()
                .with("joinDate", new AttributeValue.DateValue(date));

            String json = AttributesJsonSerializer.toJson(original);
            Attributes deserialized = AttributesJsonSerializer.fromJson(json);

            assertThat(deserialized.get("joinDate", AttributeValue.DateValue.class).value())
                .isEqualTo(date);
        }

        @Test
        @DisplayName("should round-trip all five types together")
        void roundTripAllTypes() {
            Attributes original = Attributes.of(2, Map.of(
                "name", new AttributeValue.StringValue("Test"),
                "age", new AttributeValue.IntegerValue(30),
                "active", new AttributeValue.BooleanValue(false),
                "score", new AttributeValue.DecimalValue(new BigDecimal("99.5")),
                "dob", new AttributeValue.DateValue(LocalDate.of(1995, 3, 21))
            ));

            String json = AttributesJsonSerializer.toJson(original);
            Attributes deserialized = AttributesJsonSerializer.fromJson(json);

            assertThat(deserialized.size()).isEqualTo(5);
            assertThat(deserialized.schemaVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Deserialization edge cases")
    class DeserializationEdgeCases {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("should return empty attributes for null/blank JSON")
        void nullOrBlankJsonReturnsEmpty(String input) {
            Attributes result = AttributesJsonSerializer.fromJson(input);

            assertThat(result.size()).isZero();
            assertThat(result.schemaVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("should fallback to StringValue for unknown type")
        void unknownTypeFallsBackToStringValue() {
            String json = """
                {"schemaVersion":1,"values":{"custom":{"type":"UNKNOWN","value":"hello"}}}
                """;

            Attributes result = AttributesJsonSerializer.fromJson(json);

            assertThat(result.get("custom", AttributeValue.StringValue.class).value())
                .isEqualTo("hello");
        }

        @Test
        @DisplayName("should throw for malformed JSON")
        void malformedJsonThrows() {
            assertThatThrownBy(() -> AttributesJsonSerializer.fromJson("{not valid json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to deserialize");
        }

        @Test
        @DisplayName("should handle missing values map gracefully")
        void missingValuesMap() {
            String json = """
                {"schemaVersion":2}
                """;

            Attributes result = AttributesJsonSerializer.fromJson(json);

            assertThat(result.size()).isZero();
            assertThat(result.schemaVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("should default schemaVersion to 1 when absent")
        void missingSchemaVersion() {
            String json = """
                {"values":{"key":{"type":"STRING","value":"val"}}}
                """;

            Attributes result = AttributesJsonSerializer.fromJson(json);

            assertThat(result.schemaVersion()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle empty attributes map")
        void emptyAttributesMap() {
            Attributes empty = Attributes.empty();

            String json = AttributesJsonSerializer.toJson(empty);
            Attributes result = AttributesJsonSerializer.fromJson(json);

            assertThat(result.size()).isZero();
            assertThat(json).contains("\"values\"");
        }
    }

    @Nested
    @DisplayName("Serialization")
    class Serialization {

        @Test
        @DisplayName("should produce valid JSON structure")
        void producesValidJsonStructure() {
            Attributes attrs = Attributes.empty()
                .with("tier", new AttributeValue.StringValue("GOLD"));

            String json = AttributesJsonSerializer.toJson(attrs);

            assertThat(json).contains("\"schemaVersion\"");
            assertThat(json).contains("\"values\"");
            assertThat(json).contains("\"tier\"");
            assertThat(json).contains("\"type\":\"STRING\"");
            assertThat(json).contains("\"value\":\"GOLD\"");
        }
    }
}

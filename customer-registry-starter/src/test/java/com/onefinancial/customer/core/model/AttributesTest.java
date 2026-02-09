package com.onefinancial.customer.core.model;

import com.onefinancial.customer.core.model.AttributeValue.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributesTest {

    @Test
    void shouldCreateEmptyAttributesWithSchemaVersionOne() {
        Attributes attrs = Attributes.empty();

        assertThat(attrs.schemaVersion()).isEqualTo(1);
        assertThat(attrs.size()).isZero();
        assertThat(attrs.values()).isEmpty();
    }

    @Test
    void shouldAddAttributeImmutably() {
        Attributes original = Attributes.empty();
        Attributes updated = original.with("tier", new StringValue("GOLD"));

        assertThat(original.size()).isZero();
        assertThat(updated.size()).isEqualTo(1);
        assertThat(updated.get("tier", StringValue.class).value()).isEqualTo("GOLD");
    }

    @Test
    void shouldReplaceExistingAttribute() {
        Attributes attrs = Attributes.empty()
            .with("tier", new StringValue("SILVER"))
            .with("tier", new StringValue("GOLD"));

        assertThat(attrs.size()).isEqualTo(1);
        assertThat(attrs.get("tier", StringValue.class).value()).isEqualTo("GOLD");
    }

    @Test
    void shouldSupportAllValueTypes() {
        Attributes attrs = Attributes.empty()
            .with("name", new StringValue("Test"))
            .with("age", new IntegerValue(30))
            .with("active", new BooleanValue(true))
            .with("balance", new DecimalValue(new BigDecimal("100.50")))
            .with("birthday", new DateValue(LocalDate.of(1994, 3, 15)));

        assertThat(attrs.get("name", StringValue.class).value()).isEqualTo("Test");
        assertThat(attrs.get("age", IntegerValue.class).value()).isEqualTo(30);
        assertThat(attrs.get("active", BooleanValue.class).value()).isTrue();
        assertThat(attrs.get("balance", DecimalValue.class).value())
            .isEqualByComparingTo("100.50");
        assertThat(attrs.get("birthday", DateValue.class).value())
            .isEqualTo(LocalDate.of(1994, 3, 15));
    }

    @Test
    void shouldThrowWhenKeyNotFound() {
        Attributes attrs = Attributes.empty();

        assertThatThrownBy(() -> attrs.get("missing", StringValue.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void shouldThrowWhenTypeMismatch() {
        Attributes attrs = Attributes.empty()
            .with("tier", new StringValue("GOLD"));

        assertThatThrownBy(() -> attrs.get("tier", IntegerValue.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expected IntegerValue");
    }

    @Test
    void shouldCreateFromMap() {
        Map<String, AttributeValue> map = Map.of(
            "key1", new StringValue("val1"),
            "key2", new IntegerValue(42)
        );
        Attributes attrs = Attributes.of(2, map);

        assertThat(attrs.schemaVersion()).isEqualTo(2);
        assertThat(attrs.size()).isEqualTo(2);
    }

    @Test
    void shouldUpgradeSchemaVersion() {
        Attributes v1 = Attributes.empty().with("key", new StringValue("val"));
        Attributes v2 = v1.withSchemaVersion(2);

        assertThat(v2.schemaVersion()).isEqualTo(2);
        assertThat(v2.get("key", StringValue.class).value()).isEqualTo("val");
    }

    @Test
    void shouldPreserveUnknownKeysOnSchemaUpgrade() {
        Attributes attrs = Attributes.of(1, Map.of(
            "known", new StringValue("yes"),
            "unknown_future_key", new StringValue("preserved")
        ));

        Attributes upgraded = attrs.withSchemaVersion(2);

        assertThat(upgraded.containsKey("known")).isTrue();
        assertThat(upgraded.containsKey("unknown_future_key")).isTrue();
        assertThat(upgraded.get("unknown_future_key", StringValue.class).value())
            .isEqualTo("preserved");
    }

    @Test
    void shouldCheckContainsKey() {
        Attributes attrs = Attributes.empty().with("exists", new BooleanValue(true));

        assertThat(attrs.containsKey("exists")).isTrue();
        assertThat(attrs.containsKey("missing")).isFalse();
    }

    @Test
    void shouldBeImmutable() {
        Attributes attrs = Attributes.empty().with("key", new StringValue("val"));

        assertThatThrownBy(() -> attrs.values().put("hack", new StringValue("nope")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldHaveValueEquality() {
        Attributes a = Attributes.of(1, Map.of("k", new StringValue("v")));
        Attributes b = Attributes.of(1, Map.of("k", new StringValue("v")));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}

package com.onefinancial.customer.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Sealed hierarchy of typed attribute values for the customer's extensible
 * attributes map. Each variant carries a single typed value.
 *
 * <p>Forward-compatible: unknown keys are preserved during serialization/deserialization,
 * and new variants can be added in future versions without breaking existing consumers.</p>
 */
public sealed interface AttributeValue
        permits AttributeValue.StringValue,
                AttributeValue.IntegerValue,
                AttributeValue.BooleanValue,
                AttributeValue.DecimalValue,
                AttributeValue.DateValue {

    record StringValue(String value) implements AttributeValue {}
    record IntegerValue(int value) implements AttributeValue {}
    record BooleanValue(boolean value) implements AttributeValue {}
    record DecimalValue(BigDecimal value) implements AttributeValue {}
    record DateValue(LocalDate value) implements AttributeValue {}
}

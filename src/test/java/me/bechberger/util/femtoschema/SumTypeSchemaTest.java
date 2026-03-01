package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SumTypeSchema Tests")
class SumTypeSchemaTest {

    @Test
    @DisplayName("should validate sum types with correct discriminator")
    void testBasicSumTypeValidation() {
        var emailVariant = Schemas.object()
            .required("type", Schemas.enumOf("email"))
            .required("address", Schemas.string());

        var smsVariant = Schemas.object()
            .required("type", Schemas.enumOf("sms"))
            .required("phoneNumber", Schemas.string());

        var schema = Schemas.sumType("type")
            .variant("email", emailVariant)
            .variant("sms", smsVariant);

        var emailData = Map.of(
            "type", "email",
            "address", "user@example.com"
        );
        assertTrue(schema.validate(emailData).isValid());

        var smsData = Map.of(
            "type", "sms",
            "phoneNumber", "+1234567890"
        );
        assertTrue(schema.validate(smsData).isValid());
    }

    @Test
    @DisplayName("should reject missing discriminator")
    void testMissingDiscriminator() {
        var schema = Schemas.sumType("type")
            .variant("a", Schemas.object().required("type", Schemas.enumOf("a")))
            .variant("b", Schemas.object().required("type", Schemas.enumOf("b")));

        var data = Map.of("data", "value");
        var result = schema.validate(data);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.message().contains("Discriminator field")));
    }

    @Test
    @DisplayName("should reject unknown discriminator value")
    void testUnknownDiscriminatorValue() {
        var schema = Schemas.sumType("type")
            .variant("email", Schemas.object().required("type", Schemas.enumOf("email")))
            .variant("sms", Schemas.object().required("type", Schemas.enumOf("sms")));

        var data = Map.of("type", "slack");
        var result = schema.validate(data);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.message().contains("Unknown discriminator value")));
    }

    @Test
    @DisplayName("should validate variant schema constraints")
    void testVariantSchemaValidation() {
        var variant1 = Schemas.object()
            .required("type", Schemas.enumOf("v1"))
            .required("field1", Schemas.string());

        var variant2 = Schemas.object()
            .required("type", Schemas.enumOf("v2"))
            .required("field2", Schemas.number().withMinimum(0));

        var schema = Schemas.sumType("type")
            .variant("v1", variant1)
            .variant("v2", variant2);

        // Valid v1
        assertTrue(schema.validate(Map.of(
            "type", "v1",
            "field1", "value"
        )).isValid());

        // Invalid v1 - wrong field type
        assertFalse(schema.validate(Map.of(
            "type", "v1",
            "field1", 123
        )).isValid());

        // Invalid v2 - negative number
        assertFalse(schema.validate(Map.of(
            "type", "v2",
            "field2", -5.0
        )).isValid());
    }

    @Test
    @DisplayName("should support multiple discriminator values per variant")
    void testMultipleVariants() {
        var successVariant = Schemas.object()
            .required("status", Schemas.enumOf("success"))
            .required("data", Schemas.string());

        var errorVariant = Schemas.object()
            .required("status", Schemas.enumOf("error"))
            .required("error", Schemas.string());

        var warningVariant = Schemas.object()
            .required("status", Schemas.enumOf("warning"))
            .required("warning", Schemas.string());

        var schema = Schemas.sumType("status")
            .variant("success", successVariant)
            .variant("error", errorVariant)
            .variant("warning", warningVariant);

        assertTrue(schema.validate(Map.of("status", "success", "data", "ok")).isValid());
        assertTrue(schema.validate(Map.of("status", "error", "error", "failed")).isValid());
        assertTrue(schema.validate(Map.of("status", "warning", "warning", "deprecated")).isValid());
    }

    @Test
    @DisplayName("should support complex nested structures")
    void testComplexSumType() {
        var userCreatedEvent = Schemas.object()
            .required("eventType", Schemas.enumOf("user.created"))
            .required("userId", Schemas.string())
            .required("metadata", Schemas.object()
                .required("email", Schemas.string())
                .required("createdAt", Schemas.string()));

        var userDeletedEvent = Schemas.object()
            .required("eventType", Schemas.enumOf("user.deleted"))
            .required("userId", Schemas.string());

        var schema = Schemas.sumType("eventType")
            .variant("user.created", userCreatedEvent)
            .variant("user.deleted", userDeletedEvent);

        var createEvent = Map.of(
            "eventType", "user.created",
            "userId", "123",
            "metadata", Map.of(
                "email", "user@example.com",
                "createdAt", "2026-02-24T10:00:00Z"
            )
        );
        assertTrue(schema.validate(createEvent).isValid());

        var deleteEvent = Map.of(
            "eventType", "user.deleted",
            "userId", "123"
        );
        assertTrue(schema.validate(deleteEvent).isValid());
    }

    @Test
    @DisplayName("should export to JSON Schema with discriminator")
    void testJsonSchemaExport() {
        var schema = Schemas.sumType("type")
            .variant("a", Schemas.object().required("type", Schemas.enumOf("a")))
            .variant("b", Schemas.object().required("type", Schemas.enumOf("b")));

        var jsonSchema = schema.toJsonSchema();

        assertTrue(jsonSchema.containsKey("oneOf"));
        assertTrue(jsonSchema.containsKey("discriminator"));
    }

    @Test
    @DisplayName("should support description")
    void testDescription() {
        var schema = Schemas.sumType("type")
            .withDescription("Payment method selector");

        assertEquals("Payment method selector", schema.getDescription());
    }
}
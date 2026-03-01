package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EnumSchema Tests")
class EnumSchemaTest {

    @Test
    @DisplayName("should validate enum values")
    void testEnumValidation() {
        var schema = Schemas.enumOf("RED", "GREEN", "BLUE");

        assertTrue(schema.validate("RED").isValid());
        assertTrue(schema.validate("GREEN").isValid());
        assertTrue(schema.validate("BLUE").isValid());
    }

    @Test
    @DisplayName("should reject values not in enum")
    void testRejectInvalidEnumValues() {
        var schema = Schemas.enumOf("ACTIVE", "INACTIVE");

        var result = schema.validate("DELETED");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("one of"));
    }

    @Test
    @DisplayName("should support numeric enums")
    void testNumericEnums() {
        var schema = Schemas.enumOf(200, 201, 400, 404, 500);

        assertTrue(schema.validate(200).isValid());
        assertTrue(schema.validate(404).isValid());

        assertFalse(schema.validate(999).isValid());
    }

    @Test
    @DisplayName("should support mixed type enums")
    void testMixedTypeEnums() {
        var schema = Schemas.enumOf("success", "error", 0, 1);

        assertTrue(schema.validate("success").isValid());
        assertTrue(schema.validate(0).isValid());
        assertTrue(schema.validate(1).isValid());

        assertFalse(schema.validate("unknown").isValid());
    }

    @Test
    @DisplayName("should support description")
    void testDescription() {
        var schema = Schemas.enumOf("DRAFT", "PUBLISHED")
            .withDescription("Document status");

        assertEquals("Document status", schema.getDescription());
    }

    @Test
    @DisplayName("should export to JSON Schema")
    void testJsonSchemaExport() {
        var schema = Schemas.enumOf("ACTIVE", "INACTIVE", "SUSPENDED");

        var jsonSchema = schema.toJsonSchema();

        assertTrue(jsonSchema.containsKey("enum"));
        var enumValues = (List<?>) jsonSchema.get("enum");
        assertEquals(3, enumValues.size());
    }

    @Test
    @DisplayName("should work in object schemas")
    void testInObjectSchema() {
        var schema = Schemas.object()
            .required("status", Schemas.enumOf("PENDING", "APPROVED", "REJECTED"))
            .required("priority", Schemas.enumOf("LOW", "MEDIUM", "HIGH"));

        var validData = Map.of(
            "status", "APPROVED",
            "priority", "HIGH"
        );
        assertTrue(schema.validate(validData).isValid());

        var invalidData = Map.of(
            "status", "APPROVED",
            "priority", "URGENT"
        );
        assertFalse(schema.validate(invalidData).isValid());
    }

    @Test
    @DisplayName("should work in array schemas")
    void testInArraySchema() {
        var schema = Schemas.array(Schemas.enumOf("READ", "WRITE", "EXECUTE"));

        var validData = List.of("READ", "WRITE", "EXECUTE");
        assertTrue(schema.validate(validData).isValid());

        var invalidData = List.of("READ", "WRITE", "DELETE");
        assertFalse(schema.validate(invalidData).isValid());
    }
}
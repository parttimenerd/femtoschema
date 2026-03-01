package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BooleanSchema Tests")
class BooleanSchemaTest {

    @Test
    @DisplayName("should validate boolean values")
    void testBooleanValidation() {
        var schema = Schemas.bool();

        assertTrue(schema.validate(true).isValid());
        assertTrue(schema.validate(false).isValid());
    }

    @Test
    @DisplayName("should reject non-boolean values")
    void testRejectNonBooleans() {
        var schema = Schemas.bool();

        var trueString = schema.validate("true");
        assertFalse(trueString.isValid());

        var trueNumber = schema.validate(1);
        assertFalse(trueNumber.isValid());

        var nullValue = schema.validate(null);
        assertFalse(nullValue.isValid());
    }

    @Test
    @DisplayName("should support description")
    void testDescription() {
        var schema = Schemas.bool().withDescription("Feature flag");
        assertEquals("Feature flag", schema.getDescription());
    }

    @Test
    @DisplayName("should export to JSON Schema")
    void testJsonSchemaExport() {
        var schema = Schemas.bool();

        var jsonSchema = schema.toJsonSchema();

        assertEquals("boolean", jsonSchema.get("type"));
    }

    @Test
    @DisplayName("should work in object schemas")
    void testInObjectSchema() {
        var schema = Schemas.object()
            .required("active", Schemas.bool())
            .required("premium", Schemas.bool());

        var validData = java.util.Map.of("active", true, "premium", false);
        assertTrue(schema.validate(validData).isValid());

        var invalidData = java.util.Map.of("active", "yes", "premium", true);
        assertFalse(schema.validate(invalidData).isValid());
    }
}
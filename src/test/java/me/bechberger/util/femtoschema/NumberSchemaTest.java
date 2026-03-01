package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NumberSchema Tests")
class NumberSchemaTest {

    @Test
    @DisplayName("should validate numbers")
    void testNumberValidation() {
        var schema = Schemas.number();

        assertTrue(schema.validate(42).isValid());
        assertTrue(schema.validate(3.14).isValid());
        assertTrue(schema.validate(0).isValid());
        assertTrue(schema.validate(-100).isValid());
        assertTrue(schema.validate(1L).isValid());
    }

    @Test
    @DisplayName("should reject non-number values")
    void testRejectNonNumbers() {
        var schema = Schemas.number();

        var result = schema.validate("42");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("Expected number"));
    }

    @Test
    @DisplayName("should enforce minimum value")
    void testMinimumValue() {
        var schema = Schemas.number().withMinimum(0);

        assertTrue(schema.validate(0).isValid());
        assertTrue(schema.validate(100).isValid());

        var result = schema.validate(-1);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("less than minimum"));
    }

    @Test
    @DisplayName("should enforce maximum value")
    void testMaximumValue() {
        var schema = Schemas.number().withMaximum(100);

        assertTrue(schema.validate(100).isValid());
        assertTrue(schema.validate(50).isValid());

        var result = schema.validate(101);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("greater than maximum"));
    }

    @Test
    @DisplayName("should enforce both min and max values")
    void testMinMaxValues() {
        var schema = Schemas.number().withMinimum(10).withMaximum(100);

        assertTrue(schema.validate(10).isValid());
        assertTrue(schema.validate(50).isValid());
        assertTrue(schema.validate(100).isValid());

        assertFalse(schema.validate(9).isValid());
        assertFalse(schema.validate(101).isValid());
    }

    @Test
    @DisplayName("should enforce exclusive minimum")
    void testExclusiveMinimum() {
        var schema = Schemas.number().withExclusiveMinimum(0);

        assertTrue(schema.validate(0.1).isValid());
        assertTrue(schema.validate(100).isValid());

        var result = schema.validate(0);
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("should enforce exclusive maximum")
    void testExclusiveMaximum() {
        var schema = Schemas.number().withExclusiveMaximum(100);

        assertTrue(schema.validate(99.9).isValid());
        assertTrue(schema.validate(0).isValid());

        var result = schema.validate(100);
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("should support description")
    void testDescription() {
        var schema = Schemas.number().withDescription("User age");
        assertEquals("User age", schema.getDescription());
    }

    @Test
    @DisplayName("should export to JSON Schema")
    void testJsonSchemaExport() {
        var schema = Schemas.number()
            .withMinimum(0)
            .withMaximum(100);

        var jsonSchema = schema.toJsonSchema();

        assertEquals("number", jsonSchema.get("type"));
        assertEquals(0.0, jsonSchema.get("minimum"));
        assertEquals(100.0, jsonSchema.get("maximum"));
    }

    @Test
    @DisplayName("should handle floating point precision")
    void testFloatingPointPrecision() {
        var schema = Schemas.number().withMinimum(0.5).withMaximum(10.5);

        assertTrue(schema.validate(0.5).isValid());
        assertTrue(schema.validate(5.5).isValid());
        assertTrue(schema.validate(10.5).isValid());

        assertFalse(schema.validate(0.4).isValid());
        assertFalse(schema.validate(10.6).isValid());
    }
}
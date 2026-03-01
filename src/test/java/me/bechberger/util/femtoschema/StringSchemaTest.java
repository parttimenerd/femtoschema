package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StringSchema Tests")
class StringSchemaTest {

    @Test
    @DisplayName("should validate simple strings")
    void testSimpleStringValidation() {
        var schema = Schemas.string();

        assertTrue(schema.validate("hello").isValid());
        assertTrue(schema.validate("").isValid());
        assertTrue(schema.validate("with spaces and 123").isValid());
    }

    @Test
    @DisplayName("should reject non-string values")
    void testRejectNonStringValues() {
        var schema = Schemas.string();

        var result = schema.validate(123);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).message().contains("Expected string"));
    }

    @Test
    @DisplayName("should reject null values")
    void testRejectNullValues() {
        var schema = Schemas.string();

        var result = schema.validate(null);
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("should enforce minimum length")
    void testMinimumLength() {
        var schema = Schemas.string().withMinLength(5);

        assertTrue(schema.validate("hello").isValid());
        assertTrue(schema.validate("hello world").isValid());

        var result = schema.validate("hi");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("too short"));
    }

    @Test
    @DisplayName("should enforce maximum length")
    void testMaximumLength() {
        var schema = Schemas.string().withMaxLength(10);

        assertTrue(schema.validate("hello").isValid());
        assertTrue(schema.validate("0123456789").isValid());

        var result = schema.validate("01234567890");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("too long"));
    }

    @Test
    @DisplayName("should enforce both min and max length")
    void testMinMaxLength() {
        var schema = Schemas.string().withMinLength(3).withMaxLength(10);

        assertTrue(schema.validate("abc").isValid());
        assertTrue(schema.validate("1234567890").isValid());

        assertFalse(schema.validate("ab").isValid());
        assertFalse(schema.validate("12345678901").isValid());
    }

    @Test
    @DisplayName("should validate string patterns")
    void testStringPattern() {
        var emailSchema = Schemas.string()
            .withPattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        assertTrue(emailSchema.validate("user@example.com").isValid());
        assertTrue(emailSchema.validate("test.email+tag@domain.co.uk").isValid());

        var result = emailSchema.validate("invalid-email");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("pattern"));
    }

    @Test
    @DisplayName("should support description")
    void testDescription() {
        var schema = Schemas.string().withDescription("User's email address");
        assertEquals("User's email address", schema.getDescription());
    }

    @Test
    @DisplayName("should export to JSON Schema")
    void testJsonSchemaExport() {
        var schema = Schemas.string()
            .withMinLength(5)
            .withMaxLength(100)
            .withPattern("[A-Z].*");

        var jsonSchema = schema.toJsonSchema();

        assertEquals("string", jsonSchema.get("type"));
        assertEquals(5, jsonSchema.get("minLength"));
        assertEquals(100, jsonSchema.get("maxLength"));
        assertEquals("[A-Z].*", jsonSchema.get("pattern"));
    }

    @Test
    @DisplayName("should chain builder methods")
    void testBuilderChaining() {
        var schema = Schemas.string()
            .withMinLength(1)
            .withMaxLength(50)
            .withPattern("[a-z]+")
            .withDescription("Lowercase string");

        assertTrue(schema.validate("abc").isValid());
        assertFalse(schema.validate("ABC").isValid());
    }
}
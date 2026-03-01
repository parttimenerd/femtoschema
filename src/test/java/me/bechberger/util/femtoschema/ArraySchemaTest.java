package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArraySchema Tests")
class ArraySchemaTest {

    @Test
    @DisplayName("should validate arrays of strings")
    void testArrayOfStrings() {
        var schema = Schemas.array(Schemas.string());

        assertTrue(schema.validate(List.of()).isValid());
        assertTrue(schema.validate(List.of("a", "b", "c")).isValid());

        var result = schema.validate(List.of("a", 123, "c"));
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("should validate arrays of numbers")
    void testArrayOfNumbers() {
        var schema = Schemas.array(Schemas.number());

        assertTrue(schema.validate(List.of()).isValid());
        assertTrue(schema.validate(List.of(1, 2.5, 3)).isValid());

        var result = schema.validate(List.of(1, "two", 3));
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("should enforce minimum items")
    void testMinimumItems() {
        var schema = Schemas.array(Schemas.string()).withMinItems(2);

        assertTrue(schema.validate(List.of("a", "b")).isValid());

        var result = schema.validate(List.of("a"));
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("too few"));
    }

    @Test
    @DisplayName("should enforce maximum items")
    void testMaximumItems() {
        var schema = Schemas.array(Schemas.string()).withMaxItems(2);

        assertTrue(schema.validate(List.of("a", "b")).isValid());

        var result = schema.validate(List.of("a", "b", "c"));
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("too many"));
    }

    @Test
    @DisplayName("should enforce unique items")
    void testUniqueItems() {
        var schema = Schemas.array(Schemas.string()).withUniqueItems(true);

        assertTrue(schema.validate(List.of("a", "b", "c")).isValid());

        var result = schema.validate(List.of("a", "b", "a"));
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.message().contains("unique")));
    }

    @Test
    @DisplayName("should validate arrays of objects")
    void testArrayOfObjects() {
        var personSchema = Schemas.object()
            .required("name", Schemas.string())
            .required("age", Schemas.number());

        var schema = Schemas.array(personSchema);

        var validData = List.of(
            Map.of("name", "Alice", "age", 30.0),
            Map.of("name", "Bob", "age", 25.0)
        );
        assertTrue(schema.validate(validData).isValid());

        var invalidData = List.of(
            Map.of("name", "Alice", "age", 30.0),
            Map.of("name", "Bob", "age", "invalid")
        );
        assertFalse(schema.validate(invalidData).isValid());
    }

    @Test
    @DisplayName("should reject non-array values")
    void testRejectNonArrays() {
        var schema = Schemas.array(Schemas.string());

        var result = schema.validate("not an array");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("Expected array"));
    }

    @Test
    @DisplayName("should report array item error paths")
    void testArrayItemErrorPaths() {
        var schema = Schemas.array(Schemas.number().withMinimum(0));

        var result = schema.validate(List.of(10, -5, 20));
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.path().contains("[1]")));
    }

    @Test
    @DisplayName("should export to JSON Schema")
    void testJsonSchemaExport() {
        var schema = Schemas.array(Schemas.string())
            .withMinItems(1)
            .withMaxItems(10);

        var jsonSchema = schema.toJsonSchema();

        assertEquals("array", jsonSchema.get("type"));
        assertEquals(1, jsonSchema.get("minItems"));
        assertEquals(10, jsonSchema.get("maxItems"));
    }
}
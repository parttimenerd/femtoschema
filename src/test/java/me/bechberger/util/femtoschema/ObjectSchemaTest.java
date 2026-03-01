package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ObjectSchema Tests")
class ObjectSchemaTest {

    @Test
    @DisplayName("should validate simple objects")
    void testSimpleObjectValidation() {
        var schema = Schemas.object()
            .required("name", Schemas.string())
            .required("age", Schemas.number());

        var data = Map.of("name", "Alice", "age", 30.0);
        assertTrue(schema.validate(data).isValid());
    }

    @Test
    @DisplayName("should reject non-object values")
    void testRejectNonObjects() {
        var schema = Schemas.object();

        var result = schema.validate("not an object");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).message().contains("Expected object"));
    }

    @Test
    @DisplayName("should enforce required properties")
    void testRequiredProperties() {
        var schema = Schemas.object()
            .required("name", Schemas.string())
            .required("age", Schemas.number());

        var missingName = Map.of("age", 30.0);
        var result = missingName.getClass();

        var resultMissingName = schema.validate(missingName);
        assertFalse(resultMissingName.isValid());
        assertTrue(resultMissingName.getErrors().stream()
            .anyMatch(e -> e.path().contains("name")));

        var missingAge = Map.of("name", "Bob");
        var resultMissingAge = schema.validate(missingAge);
        assertFalse(resultMissingAge.isValid());
        assertTrue(resultMissingAge.getErrors().stream()
            .anyMatch(e -> e.path().contains("age")));
    }

    @Test
    @DisplayName("should allow optional properties")
    void testOptionalProperties() {
        var schema = Schemas.object()
            .required("name", Schemas.string())
            .optional("email", Schemas.string());

        var withEmail = Map.of("name", "Alice", "email", "alice@example.com");
        assertTrue(schema.validate(withEmail).isValid());

        var withoutEmail = Map.of("name", "Bob");
        assertTrue(schema.validate(withoutEmail).isValid());
    }

    @Test
    @DisplayName("should validate property types")
    void testPropertyTypeValidation() {
        var schema = Schemas.object()
            .required("name", Schemas.string())
            .required("age", Schemas.number());

        var wrongTypes = Map.of("name", "Alice", "age", "thirty");
        var result = schema.validate(wrongTypes);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.path().contains("age")));
    }

    @Test
    @DisplayName("should allow additional properties by default")
    void testAdditionalPropertiesAllowed() {
        var schema = Schemas.object()
            .required("name", Schemas.string());

        var dataWithExtra = Map.of("name", "Alice", "extra", "field");
        assertTrue(schema.validate(dataWithExtra).isValid());
    }

    @Test
    @DisplayName("should disallow additional properties when configured")
    void testDisallowAdditionalProperties() {
        var schema = Schemas.object()
            .required("name", Schemas.string())
            .allowAdditionalProperties(false);

        var dataWithExtra = Map.of("name", "Alice", "extra", "field");
        var result = schema.validate(dataWithExtra);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.message().contains("Additional properties")));
    }

    @Test
    @DisplayName("should support nested objects")
    void testNestedObjects() {
        var addressSchema = Schemas.object()
            .required("street", Schemas.string())
            .required("city", Schemas.string());

        var schema = Schemas.object()
            .required("name", Schemas.string())
            .optional("address", addressSchema);

        var validData = Map.of(
            "name", "Alice",
            "address", Map.of("street", "Main St", "city", "Springfield")
        );
        assertTrue(schema.validate(validData).isValid());

        var invalidAddress = Map.of(
            "name", "Bob",
            "address", Map.of("street", "Main St", "city", 123)
        );
        assertFalse(schema.validate(invalidAddress).isValid());
    }

    @Test
    @DisplayName("should report nested error paths")
    void testNestedErrorPaths() {
        var schema = Schemas.object()
            .required("user", Schemas.object()
                .required("name", Schemas.string())
                .required("age", Schemas.number().withMinimum(0)));

        var invalidData = Map.of(
            "user", Map.of("name", "Alice", "age", -5.0)
        );

        var result = schema.validate(invalidData);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.path().contains("$.user.age")));
    }

    @Test
    @DisplayName("should export to JSON Schema")
    void testJsonSchemaExport() {
        var schema = Schemas.object()
            .required("name", Schemas.string())
            .required("age", Schemas.number())
            .optional("email", Schemas.string());

        var jsonSchema = schema.toJsonSchema();

        assertEquals("object", jsonSchema.get("type"));
        assertTrue(jsonSchema.containsKey("properties"));
        assertTrue(jsonSchema.containsKey("required"));
    }
}
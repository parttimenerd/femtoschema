package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationResult and cross-validation between our implementation
 * and the oracle.
 */
@DisplayName("ValidationResult Tests")
class ValidationResultTest {

    @Test
    @DisplayName("Valid result should indicate success")
    void testValidResult() {
        var result = ValidationResult.valid();

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Invalid result should contain errors")
    void testInvalidResult() {
        var result = ValidationResult.invalid("$.field", "Invalid value");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("$.field", result.getErrors().get(0).path());
        assertEquals("Invalid value", result.getErrors().get(0).message());
    }

    @Test
    @DisplayName("Invalid result with multiple errors")
    void testMultipleErrors() {
        var errors = List.of(
            new ValidationResult.ValidationError("$.field1", "Error 1"),
            new ValidationResult.ValidationError("$.field2", "Error 2"),
            new ValidationResult.ValidationError("$.field3", "Error 3")
        );

        var result = ValidationResult.invalid(errors);

        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
    }

    @Test
    @DisplayName("Errors should maintain insertion order")
    void testErrorOrder() {
        var result = ValidationResult.invalid(List.of(
            new ValidationResult.ValidationError("$.a", "First"),
            new ValidationResult.ValidationError("$.b", "Second"),
            new ValidationResult.ValidationError("$.c", "Third")
        ));

        var errors = result.getErrors();
        assertEquals("$.a", errors.get(0).path());
        assertEquals("$.b", errors.get(1).path());
        assertEquals("$.c", errors.get(2).path());
    }

    @Test
    @DisplayName("Error list should be immutable")
    void testErrorImmutability() {
        var mutableList = new ArrayList<ValidationResult.ValidationError>();
        mutableList.add(new ValidationResult.ValidationError("$.field", "Error"));

        var result = ValidationResult.invalid(mutableList);

        // Modifying original list should not affect result
        mutableList.add(new ValidationResult.ValidationError("$.other", "Another"));

        assertEquals(1, result.getErrors().size());
    }

    @Test
    @DisplayName("should collect errors from nested structures")
    void testNestedStructureErrors() {
        var schema = Schemas.object()
            .required("user", Schemas.object()
                .required("profile", Schemas.object()
                    .required("age", Schemas.number().withMinimum(0))
                    .required("email", Schemas.string())));

        var data = Map.of(
            "user", Map.of(
                "profile", Map.of(
                    "age", -5.0,
                    "email", 123
                )
            )
        );

        var result = schema.validate(data);

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());

        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.path().contains("age")));
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.path().contains("email")));
    }

    @Test
    @DisplayName("should provide clear error messages")
    void testErrorMessages() {
        var schema = Schemas.object()
            .required("name", Schemas.string())
            .required("age", Schemas.number());

        var result = schema.validate(Map.of("age", 30));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.message().contains("Required")));
    }

    @Test
    @DisplayName("should report field type mismatch clearly")
    void testTypeMismatchErrors() {
        var schema = Schemas.object()
            .required("count", Schemas.number())
            .required("active", Schemas.bool());

        var result = schema.validate(Map.of(
            "count", "not a number",
            "active", "not a boolean"
        ));

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());

        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.message().contains("number")));
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.message().contains("boolean")));
    }

    @Test
    @DisplayName("should track array item indices in errors")
    void testArrayErrorIndices() {
        var schema = Schemas.array(Schemas.number().withMinimum(0));

        var result = schema.validate(List.of(10, -5, 20, -10));

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());

        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.path().contains("[1]")));
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.path().contains("[3]")));
    }

    @Test
    @DisplayName("should report array constraint violations")
    void testArrayConstraintErrors() {
        var schema = Schemas.array(Schemas.string())
            .withMinItems(2)
            .withMaxItems(4);

        var tooFew = schema.validate(List.of("a"));
        assertFalse(tooFew.isValid());
        assertTrue(tooFew.getErrors().get(0).message().contains("too few"));

        var tooMany = schema.validate(List.of("a", "b", "c", "d", "e"));
        assertFalse(tooMany.isValid());
        assertTrue(tooMany.getErrors().get(0).message().contains("too many"));
    }

    @Test
    @DisplayName("should report constraint violations for strings")
    void testStringConstraintErrors() {
        var schema = Schemas.string()
            .withMinLength(5)
            .withMaxLength(10);

        var tooShort = schema.validate("hi");
        assertFalse(tooShort.isValid());
        assertTrue(tooShort.getErrors().get(0).message().contains("too short"));

        var tooLong = schema.validate("thisisareallylongstring");
        assertFalse(tooLong.isValid());
        assertTrue(tooLong.getErrors().get(0).message().contains("too long"));
    }

    @Test
    @DisplayName("should report constraint violations for numbers")
    void testNumberConstraintErrors() {
        var schema = Schemas.number()
            .withMinimum(0)
            .withMaximum(100);

        var tooSmall = schema.validate(-10);
        assertFalse(tooSmall.isValid());
        assertTrue(tooSmall.getErrors().get(0).message().contains("less than minimum"));

        var tooLarge = schema.validate(150);
        assertFalse(tooLarge.isValid());
        assertTrue(tooLarge.getErrors().get(0).message().contains("greater than maximum"));
    }

    @Test
    @DisplayName("should validate error paths format")
    void testErrorPathFormat() {
        var schema = Schemas.object()
            .required("items", Schemas.array(Schemas.object()
                .required("id", Schemas.number())));

        var data = Map.of(
            "items", List.of(
                Map.of("id", 1),
                Map.of("id", "invalid")
            )
        );

        var result = schema.validate(data);

        assertFalse(result.isValid());
        var errorPath = result.getErrors().get(0).path();

        // Should follow JSON path format: $.items[1].id
        assertTrue(errorPath.contains("$.items"));
        assertTrue(errorPath.contains("[1]"));
        assertTrue(errorPath.contains("id"));
    }
}
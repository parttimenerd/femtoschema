package me.bechberger.util.femtoschema;

import java.util.*;

/**
 * Result of validating a value against a TypeSchema.
 *
 * <h2>Examples:</h2>
 *
 * <h3>Checking if Validation Succeeded</h3>
 * <pre>{@code
 * var schema = Schemas.string();
 * var result = schema.validate("hello");
 *
 * if (result.isValid()) {
 *     System.out.println("Validation successful!");
 * }
 * }</pre>
 *
 * <h3>Handling Validation Errors</h3>
 * <pre>{@code
 * var schema = Schemas.number().withMinimum(0);
 * var result = schema.validate(-5);
 *
 * if (!result.isValid()) {
 *     result.getErrors().forEach(error ->
 *         System.out.println(error.path() + ": " + error.message())
 *     );
 *     // Output: $: Number less than minimum (0)
 * }
 * }</pre>
 *
 * <h3>Nested Validation Errors</h3>
 * <pre>{@code
 * var schema = Schemas.object()
 *     .required("user", Schemas.object()
 *         .required("name", Schemas.string())
 *         .required("age", Schemas.number().withMinimum(0)));
 *
 * var data = Map.of(
 *     "user", Map.of(
 *         "name", "Alice",
 *         "age", -5.0  // Invalid
 *     )
 * );
 *
 * var result = schema.validate(data);
 * result.getErrors().forEach(error ->
 *     System.out.println(error.path() + ": " + error.message())
 * );
 * // Output: $.user.age: Number less than minimum (0)
 * }</pre>
 *
 * <h3>Multiple Validation Errors</h3>
 * <pre>{@code
 * var schema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number().withMinimum(0))
 *     .required("email", Schemas.string());
 *
 * var data = Map.of(
 *     "name", 123,        // Wrong type
 *     "age", -5.0,        // Invalid value
 *     "extra", "field"    // Extra field not allowed
 * );
 *
 * var result = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number().withMinimum(0))
 *     .allowAdditionalProperties(false)
 *     .validate(data);
 *
 * if (!result.isValid()) {
 *     System.out.println("Validation failed with " + result.getErrors().size() + " errors:");
 *     result.getErrors().forEach(error ->
 *         System.out.println("  " + error.path() + ": " + error.message())
 *     );
 * }
 * }</pre>
 *
 * <h3>Array Item Validation Errors</h3>
 * <pre>{@code
 * var schema = Schemas.array(
 *     Schemas.object()
 *         .required("id", Schemas.number())
 *         .required("name", Schemas.string())
 * );
 *
 * var data = List.of(
 *     Map.of("id", 1.0, "name", "Alice"),
 *     Map.of("id", "invalid", "name", "Bob"),  // id should be number
 *     Map.of("id", 3.0, "name", 123)           // name should be string
 * );
 *
 * var result = schema.validate(data);
 * result.getErrors().forEach(error ->
 *     System.out.println(error.path() + ": " + error.message())
 * );
 * // Output:
 * // $[1].id: Expected number, got java.lang.String
 * // $[2].name: Expected string, got java.lang.Integer
 * }</pre>
 *
 * <h3>Using Valid and Invalid Constructors</h3>
 * <pre>{@code
 * // Create a successful result
 * ValidationResult success = ValidationResult.valid();
 * assert success.isValid();
 *
 * // Create a failed result with a single error
 * ValidationResult singleError = ValidationResult.invalid("$.field", "Invalid value");
 * assert !singleError.isValid();
 * assert singleError.getErrors().size() == 1;
 *
 * // Create a failed result with multiple errors
 * List<ValidationResult.ValidationError> errors = List.of(
 *     new ValidationResult.ValidationError("$.field1", "Error 1"),
 *     new ValidationResult.ValidationError("$.field2", "Error 2")
 * );
 * ValidationResult multiError = ValidationResult.invalid(errors);
 * assert multiError.getErrors().size() == 2;
 * }</pre>
 */
public sealed interface ValidationResult permits ValidationResult.Valid, ValidationResult.Invalid {

    /** Returns true if validation was successful */
    boolean isValid();

    /** Gets the validation errors, or empty list if valid */
    List<ValidationError> getErrors();

    /** Represents a valid value */
    record Valid() implements ValidationResult {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public List<ValidationError> getErrors() {
            return Collections.emptyList();
        }
    }

    /** Represents validation failures with error details */
    record Invalid(List<ValidationError> errors) implements ValidationResult {
        public Invalid(List<ValidationError> errors) {
            this.errors = List.copyOf(errors);
        }

        public Invalid(ValidationError error) {
            this(List.of(error));
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public List<ValidationError> getErrors() {
            return errors;
        }
    }

    /** Represents a single validation error with location and message */
    record ValidationError(
        String path,          // JSON path to the invalid value (e.g., "$.users[0].name")
        String message        // Error message describing what's wrong
    ) {
        public ValidationError {
            Objects.requireNonNull(path, "path cannot be null");
            Objects.requireNonNull(message, "message cannot be null");
        }
    }

    static Valid valid() {
        return new Valid();
    }

    static Invalid invalid(String path, String message) {
        return new Invalid(new ValidationError(path, message));
    }

    static Invalid invalid(List<ValidationError> errors) {
        return new Invalid(errors);
    }
}
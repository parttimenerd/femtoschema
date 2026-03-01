package me.bechberger.util.femtoschema;

import java.util.*;

/**
 * Sealed interface representing a type schema that can validate JSON values and export to JSON Schema.
 * Supports all standard JSON types plus enums and sum types (discriminated unions).
 *
 * <h2>Supported Schema Types</h2>
 * <ul>
 *   <li><code>StringSchema</code> - For string values with optional constraints</li>
 *   <li><code>NumberSchema</code> - For numeric values (int, long, double, etc.)</li>
 *   <li><code>BooleanSchema</code> - For boolean values</li>
 *   <li><code>ObjectSchema</code> - For object/map values with typed properties</li>
 *   <li><code>ArraySchema</code> - For array/list values with a fixed item schema</li>
 *   <li><code>EnumSchema</code> - For values restricted to a set of allowed options</li>
 *   <li><code>SumTypeSchema</code> - For discriminated unions with multiple variants</li>
 * </ul>
 *
 * <h2>Basic Usage Example</h2>
 * <pre>{@code
 * // Define a schema
 * var userSchema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number().withMinimum(0));
 *
 * // Validate data
 * var userData = Map.of("name", "Alice", "age", 30.0);
 * ValidationResult result = userSchema.validate(userData);
 *
 * // Check result
 * if (result.isValid()) {
 *     System.out.println("Valid!");
 * } else {
 *     result.getErrors().forEach(e ->
 *         System.out.println(e.path() + ": " + e.message())
 *     );
 * }
 * }</pre>
 *
 * <h2>JSON Schema Export</h2>
 * <pre>{@code
 * var schema = Schemas.string().withMinLength(1).withMaxLength(100);
 *
 * // Export to JSON Schema format
 * Map<String, Object> jsonSchema = schema.toJsonSchema();
 * // { "type": "string", "minLength": 1, "maxLength": 100 }
 *
 * // Pretty print
 * System.out.println(Schemas.toJsonSchemaString(schema));
 * }</pre>
 *
 * <h2>Path-aware Validation</h2>
 * <pre>{@code
 * var schema = Schemas.object()
 *     .required("user", Schemas.object()
 *         .required("profile", Schemas.object()
 *             .required("age", Schemas.number().withMinimum(0))));
 *
 * // Validation errors include the JSON path to the invalid value
 * var result = schema.validate(data);
 * // Error path: $.user.profile.age
 * }</pre>
 */
public sealed interface TypeSchema permits
    StringSchema, NumberSchema, BooleanSchema, ObjectSchema, ArraySchema, EnumSchema, SumTypeSchema {

    /**
     * Validates a value against this schema.
     * @param value the value to validate (typically Map, List, String, Number, Boolean, or null)
     * @return a ValidationResult indicating success or listing errors
     */
    ValidationResult validate(Object value);

    /**
     * Validates a value at a specific JSON path for better error reporting.
     * @param value the value to validate
     * @param path the JSON path to this value (e.g., "$.users[0].name")
     * @return a ValidationResult indicating success or listing errors
     */
    ValidationResult validate(Object value, String path);

    /**
     * Exports this schema as a JSON Schema representation.
     * @return a Map representing the JSON Schema format
     */
    Map<String, Object> toJsonSchema();

    /**
     * Gets a human-readable description of this schema type.
     */
    String getDescription();
}
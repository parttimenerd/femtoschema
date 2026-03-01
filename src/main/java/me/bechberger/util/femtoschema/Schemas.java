package me.bechberger.util.femtoschema;

import me.bechberger.util.json.JSONParser;
import me.bechberger.util.json.PrettyPrinter;

import java.util.*;

/**
 * Fluent DSL for creating and composing type schemas.
 *
 * <h2>Factory Methods for Basic Types</h2>
 * <pre>{@code
 * // Create basic schemas
 * var stringSchema = Schemas.string();
 * var numberSchema = Schemas.number();
 * var boolSchema = Schemas.bool();
 * var objectSchema = Schemas.object();
 * var arraySchema = Schemas.array(Schemas.string());
 * }</pre>
 *
 * <h2>Building Objects with Required and Optional Fields</h2>
 * <pre>{@code
 * var userSchema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number().withMinimum(0))
 *     .optional("email", Schemas.string())
 *     .optional("phone", Schemas.string());
 *
 * var userData = Map.of(
 *     "name", "Alice",
 *     "age", 30.0,
 *     "email", "alice@example.com"
 * );
 * var result = userSchema.validate(userData);
 * assert result.isValid();
 * }</pre>
 *
 * <h2>Enums and Sum Types</h2>
 * <pre>{@code
 * // Simple enum
 * var statusEnum = Schemas.enumOf("ACTIVE", "INACTIVE");
 * assert statusEnum.validate("ACTIVE").isValid();
 *
 * // Sum type with variants
 * var sumType = Schemas.sumType("type")
 *     .variant("email", Schemas.object().required("type", Schemas.enumOf("email")))
 *     .variant("sms", Schemas.object().required("type", Schemas.enumOf("sms")));
 * }</pre>
 *
 * <h2>Complex Nested Schemas</h2>
 * <pre>{@code
 * var addressSchema = Schemas.object()
 *     .required("street", Schemas.string())
 *     .required("city", Schemas.string())
 *     .required("zipCode", Schemas.string());
 *
 * var personSchema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number())
 *     .optional("address", addressSchema);
 *
 * var companySchema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .optional("employees", Schemas.array(personSchema));
 * }</pre>
 *
 * <h2>JSON Schema Export</h2>
 * <pre>{@code
 * var schema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number());
 *
 * // Export as JSON Schema
 * Map<String, Object> jsonSchema = schema.toJsonSchema();
 *
 * // Pretty print
 * String formatted = Schemas.toJsonSchemaString(schema);
 * System.out.println(formatted);
 * }</pre>
 */
public class Schemas {

    // Basic types

    /**
     * Creates a schema for string values.
     */
    public static StringSchema string() {
        return new StringSchema();
    }

    /**
     * Creates a schema for numeric values.
     */
    public static NumberSchema number() {
        return new NumberSchema();
    }

    /**
     * Creates a schema for boolean values.
     */
    public static BooleanSchema bool() {
        return new BooleanSchema();
    }

    /**
     * Creates a schema for object/map values.
     */
    public static ObjectSchema object() {
        return new ObjectSchema();
    }

    /**
     * Creates a schema for array/list values.
     */
    public static ArraySchema array(TypeSchema itemSchema) {
        return new ArraySchema(itemSchema);
    }

    // Enum and sum types

    /**
     * Creates an enum schema for values restricted to a set of allowed values.
     */
    @SafeVarargs
    public static <T> EnumSchema<T> enumOf(T... values) {
        return new EnumSchema<>(values);
    }

    /**
     * Creates an enum schema for values restricted to a set of allowed values.
     */
    public static <T> EnumSchema<T> enumOf(Set<T> values) {
        return new EnumSchema<>(values);
    }

    /**
     * Creates a sum type schema for discriminated unions.
     * @param discriminatorField the name of the field that determines the variant
     */
    public static SumTypeSchema sumType(String discriminatorField) {
        return new SumTypeSchema(discriminatorField);
    }

    // Utility methods

    /**
     * Pretty-prints a JSON Schema to a formatted string.
     * <p>
     * Delegates to <a href="https://github.com/parttimenerd/femtojson">femtojson</a>'s
     * {@link PrettyPrinter#prettyPrint(Object)}.
     */
    public static String toJsonSchemaString(TypeSchema schema) {
        return PrettyPrinter.prettyPrint(schema.toJsonSchema());
    }

    /**
     * Parses a JSON Schema object (as a Java map, e.g. from Jackson) back into a {@link TypeSchema}.
     * <p>
     * Only the subset of JSON Schema produced by this library's {@code toJsonSchema()} methods is supported.
     * The {@code $comment} keyword is ignored.
     *
     * @param jsonSchema JSON Schema root object
     * @return parsed schema
     * @throws IllegalArgumentException if the schema can't be parsed
     */
    public static TypeSchema fromJsonSchema(Object jsonSchema) {
        return JsonSchemaParser.parse(jsonSchema);
    }

    /**
     * Parses a JSON Schema string back into a {@link TypeSchema}.
     * <p>
     * This is a convenience wrapper around {@link #fromJsonSchema(Object)} that first parses the JSON
     * string via {@link JSONParser#parse(String)}.
     * <p>
     * Only the subset of JSON Schema produced by this library's {@code toJsonSchema()} methods is supported.
     * The {@code $comment} keyword is ignored.
     *
     * @param jsonSchema JSON Schema as a JSON string
     * @return parsed schema
     * @throws IllegalArgumentException if the input cannot be parsed as JSON or cannot be converted into a {@link TypeSchema}
     */
    public static TypeSchema fromJsonSchemaString(String jsonSchema) {
        try {
            return fromJsonSchema(JSONParser.parse(jsonSchema));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON schema string: " + e.getMessage(), e);
        }
    }
}
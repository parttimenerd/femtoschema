package me.bechberger.util.femtoschema;

import java.util.*;

/**
 * Parses (a subset of) JSON Schema maps back into {@link TypeSchema} instances.
 * <p>
 * This is intentionally limited to the JSON Schema shapes emitted by this library's
 * {@code toJsonSchema()} implementations.
 * <p>
 * The parser ignores the JSON Schema keyword {@code $comment} at any nesting level.
 */
final class JsonSchemaParser {

    private JsonSchemaParser() {
    }

    static TypeSchema parse(Object jsonSchema) {
        if (!(jsonSchema instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("JSON schema must be an object/map, got " + JsonSchemaParserHelpers.typeOf(jsonSchema));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaMap = (Map<String, Object>) map;
        return parseSchemaObject(schemaMap);
    }

    private static TypeSchema parseSchemaObject(Map<String, Object> schema) {
        // dispatch in order: sum type -> enum -> typed schema
        if (schema.containsKey("oneOf")) {
            return SumTypeSchema.fromJsonSchema(schema);
        }
        if (schema.containsKey("enum")) {
            return EnumSchema.fromJsonSchema(schema);
        }

        Object typeObj = schema.get("type");
        if (!(typeObj instanceof String type)) {
            throw new IllegalArgumentException(
                "Unsupported JSON schema object (missing 'type', 'enum', or 'oneOf'): " + schema.keySet());
        }

        return switch (type) {
            case "string" -> StringSchema.fromJsonSchema(schema);
            case "number" -> NumberSchema.fromJsonSchema(schema);
            case "boolean" -> BooleanSchema.fromJsonSchema(schema);
            case "array" -> ArraySchema.fromJsonSchema(schema);
            case "object" -> ObjectSchema.fromJsonSchema(schema);
            default -> throw new IllegalArgumentException("Unsupported JSON schema type: " + type);
        };
    }
}
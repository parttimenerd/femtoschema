package me.bechberger.util.femtoschema;

import java.util.*;

/**
 * Schema for array/list values with a fixed item schema.
 *
 * <h2>Examples:</h2>
 *
 * <h3>Array of Strings</h3>
 * <pre>{@code
 * var tagsSchema = Schemas.array(Schemas.string());
 *
 * var tags = List.of("java", "database", "distributed");
 * assert tagsSchema.validate(tags).isValid();
 *
 * // Invalid - contains non-string
 * var invalidTags = List.of("java", 123, "database");
 * assert !tagsSchema.validate(invalidTags).isValid();
 * }</pre>
 *
 * <h3>Array with Min/Max Items</h3>
 * <pre>{@code
 * var coordinatesSchema = Schemas.array(Schemas.number())
 *     .withMinItems(2)
 *     .withMaxItems(3);  // 2D or 3D coordinates
 *
 * assert coordinatesSchema.validate(List.of(10.5, 20.3)).isValid();
 * assert coordinatesSchema.validate(List.of(10.5, 20.3, 5.0)).isValid();
 * assert !coordinatesSchema.validate(List.of(10.5)).isValid();              // Too few
 * assert !coordinatesSchema.validate(List.of(1.0, 2.0, 3.0, 4.0)).isValid(); // Too many
 * }</pre>
 *
 * <h3>Array of Objects</h3>
 * <pre>{@code
 * var personSchema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number());
 *
 * var peopleSchema = Schemas.array(personSchema);
 *
 * var people = List.of(
 *     Map.of("name", "Alice", "age", 30.0),
 *     Map.of("name", "Bob", "age", 25.0)
 * );
 * assert peopleSchema.validate(people).isValid();
 * }</pre>
 *
 * <h3>Unique Items Constraint</h3>
 * <pre>{@code
 * var uniqueTagsSchema = Schemas.array(Schemas.string())
 *     .withUniqueItems(true)
 *     .withMinItems(1);
 *
 * assert uniqueTagsSchema.validate(List.of("java", "python")).isValid();
 * assert !uniqueTagsSchema.validate(List.of("java", "java")).isValid();  // Duplicates
 * }</pre>
 *
 * <h3>Validation with Error Reporting</h3>
 * <pre>{@code
 * var schema = Schemas.array(Schemas.number().withMinimum(0))
 *     .withMinItems(1);
 *
 * var invalidData = List.of(10.0, -5.0, 20.0);  // -5 is invalid
 *
 * var result = schema.validate(invalidData);
 * if (!result.isValid()) {
 *     result.getErrors().forEach(e ->
 *         System.out.println(e.path() + ": " + e.message())
 *     );
 *     // Output: $[1]: Number less than minimum (0)
 * }
 * }</pre>
 *
 * <h3>JSON Schema Export</h3>
 * <pre>{@code
 * var schema = Schemas.array(Schemas.string())
 *     .withMinItems(1)
 *     .withMaxItems(10);
 *
 * var jsonSchema = schema.toJsonSchema();
 * // {
 * //   "type": "array",
 * //   "items": {"type": "string"},
 * //   "minItems": 1,
 * //   "maxItems": 10
 * // }
 * }</pre>
 */
public final class ArraySchema implements TypeSchema {
    private final TypeSchema itemSchema;
    private final Integer minItems;
    private final Integer maxItems;
    private final boolean uniqueItems;
    private final String description;

    public ArraySchema(TypeSchema itemSchema) {
        this(itemSchema, null, null, false, "array");
    }

    private ArraySchema(TypeSchema itemSchema, Integer minItems, Integer maxItems,
                       boolean uniqueItems, String description) {
        this.itemSchema = Objects.requireNonNull(itemSchema, "itemSchema cannot be null");
        this.minItems = minItems;
        this.maxItems = maxItems;
        this.uniqueItems = uniqueItems;
        this.description = description;
    }

    public ArraySchema withMinItems(int min) {
        return new ArraySchema(itemSchema, min, maxItems, uniqueItems, description);
    }

    public ArraySchema withMaxItems(int max) {
        return new ArraySchema(itemSchema, minItems, max, uniqueItems, description);
    }

    public ArraySchema withUniqueItems(boolean unique) {
        return new ArraySchema(itemSchema, minItems, maxItems, unique, description);
    }

    public ArraySchema withDescription(String desc) {
        return new ArraySchema(itemSchema, minItems, maxItems, uniqueItems, desc);
    }

    @Override
    public ValidationResult validate(Object value) {
        return validate(value, "$");
    }

    @Override
    public ValidationResult validate(Object value, String path) {
        if (!(value instanceof List)) {
            return ValidationResult.invalid(path, "Expected array, got " + typeOf(value));
        }

        List<?> list = (List<?>) value;
        List<ValidationResult.ValidationError> errors = new ArrayList<>();

        if (minItems != null && list.size() < minItems) {
            errors.add(new ValidationResult.ValidationError(path,
                "Array has too few items (minimum " + minItems + ")"));
        }

        if (maxItems != null && list.size() > maxItems) {
            errors.add(new ValidationResult.ValidationError(path,
                "Array has too many items (maximum " + maxItems + ")"));
        }

        // Validate each item
        Set<Object> seenItems = uniqueItems ? new HashSet<>() : null;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            String itemPath = path + "[" + i + "]";

            ValidationResult result = itemSchema.validate(item, itemPath);
            if (!result.isValid()) {
                errors.addAll(result.getErrors());
            }

            if (uniqueItems && !seenItems.add(item)) {
                errors.add(new ValidationResult.ValidationError(itemPath,
                    "Array items must be unique"));
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.valid();
        }
        return ValidationResult.invalid(errors);
    }

    @Override
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", itemSchema.toJsonSchema());
        if (minItems != null) schema.put("minItems", minItems);
        if (maxItems != null) schema.put("maxItems", maxItems);
        if (uniqueItems) schema.put("uniqueItems", true);
        return schema;
    }

    @Override
    public String getDescription() {
        return description;
    }

    static ArraySchema fromJsonSchema(Map<String, Object> schema) {
        Object itemsObj = schema.get("items");
        if (!(itemsObj instanceof Map<?, ?> itemsMapRaw)) {
            throw new IllegalArgumentException("Array schema must have an 'items' schema object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> itemsMap = (Map<String, Object>) itemsMapRaw;

        TypeSchema itemSchema = JsonSchemaParser.parse(itemsMap);
        ArraySchema s = Schemas.array(itemSchema);

        Integer minItems = JsonSchemaParserHelpers.asInteger(schema.get("minItems"));
        Integer maxItems = JsonSchemaParserHelpers.asInteger(schema.get("maxItems"));
        Boolean uniqueItems = JsonSchemaParserHelpers.asBoolean(schema.get("uniqueItems"));

        if (minItems != null) s = s.withMinItems(minItems);
        if (maxItems != null) s = s.withMaxItems(maxItems);
        if (uniqueItems != null) s = s.withUniqueItems(uniqueItems);

        JsonSchemaParserHelpers.rejectUnknownKeywords(schema, Set.of("type", "items", "minItems", "maxItems", "uniqueItems"));
        return s;
    }

    private String typeOf(Object obj) {
        if (obj == null) return "null";
        return obj.getClass().getSimpleName().toLowerCase();
    }
}
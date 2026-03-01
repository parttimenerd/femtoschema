package me.bechberger.util.femtoschema;

import java.util.*;

/**
 * Schema for enum values - restricts to a set of allowed values.
 *
 * <h2>Examples:</h2>
 *
 * <h3>String Enum - User Status</h3>
 * <pre>{@code
 * var statusSchema = Schemas.enumOf("ACTIVE", "INACTIVE", "SUSPENDED");
 *
 * assert statusSchema.validate("ACTIVE").isValid();
 * assert statusSchema.validate("INACTIVE").isValid();
 * assert !statusSchema.validate("DELETED").isValid();
 * }</pre>
 *
 * <h3>Number Enum - HTTP Status Codes</h3>
 * <pre>{@code
 * var httpStatusSchema = Schemas.enumOf(200, 201, 400, 401, 404, 500);
 *
 * assert httpStatusSchema.validate(200).isValid();
 * assert httpStatusSchema.validate(404).isValid();
 * assert !httpStatusSchema.validate(999).isValid();
 * }</pre>
 *
 * <h3>Enum with Description</h3>
 * <pre>{@code
 * var prioritySchema = Schemas.enumOf("LOW", "MEDIUM", "HIGH")
 *     .withDescription("Task priority level");
 *
 * assert prioritySchema.validate("HIGH").isValid();
 * }</pre>
 *
 * <h3>Using EnumSchema in an Object</h3>
 * <pre>{@code
 * var taskSchema = Schemas.object()
 *     .required("title", Schemas.string())
 *     .required("status", Schemas.enumOf("TODO", "IN_PROGRESS", "DONE"))
 *     .required("priority", Schemas.enumOf("LOW", "MEDIUM", "HIGH"));
 *
 * var taskData = Map.of(
 *     "title", "Implement feature X",
 *     "status", "IN_PROGRESS",
 *     "priority", "HIGH"
 * );
 * assert taskSchema.validate(taskData).isValid();
 * }</pre>
 *
 * <h3>Enum as Array Items</h3>
 * <pre>{@code
 * var permissionsSchema = Schemas.array(
 *     Schemas.enumOf("READ", "WRITE", "DELETE", "ADMIN")
 * );
 *
 * var permissions = List.of("READ", "WRITE", "DELETE");
 * assert permissionsSchema.validate(permissions).isValid();
 *
 * var invalidPermissions = List.of("READ", "WRITE", "INVALID");
 * assert !permissionsSchema.validate(invalidPermissions).isValid();
 * }</pre>
 *
 * <h3>JSON Schema Export</h3>
 * <pre>{@code
 * var schema = Schemas.enumOf("DRAFT", "PUBLISHED", "ARCHIVED");
 *
 * var jsonSchema = schema.toJsonSchema();
 * // {
 * //   "enum": ["DRAFT", "PUBLISHED", "ARCHIVED"]
 * // }
 * }</pre>
 *
 * <h3>Validation with Error Messages</h3>
 * <pre>{@code
 * var roleSchema = Schemas.enumOf("USER", "ADMIN", "MODERATOR");
 *
 * var result = roleSchema.validate("GUEST");
 * if (!result.isValid()) {
 *     result.getErrors().forEach(e ->
 *         System.out.println(e.message())
 *     );
 *     // Output: Value must be one of: [USER, ADMIN, MODERATOR]
 * }
 * }</pre>
 */
public final class EnumSchema<T> implements TypeSchema {
    private final Set<T> allowedValues;
    private final String description;

    @SafeVarargs
    public EnumSchema(T... values) {
        this(Set.of(values), "enum");
    }

    public EnumSchema(Set<T> allowedValues) {
        this(allowedValues, "enum");
    }

    private EnumSchema(Set<T> allowedValues, String description) {
        this.allowedValues = new HashSet<>(allowedValues);
        this.description = description;
    }

    public EnumSchema<T> withDescription(String desc) {
        return new EnumSchema<>(allowedValues, desc);
    }

    @Override
    public ValidationResult validate(Object value) {
        return validate(value, "$");
    }

    @Override
    public ValidationResult validate(Object value, String path) {
        if (!allowedValues.contains(value)) {
            return ValidationResult.invalid(path,
                "Value must be one of: " + allowedValues);
        }
        return ValidationResult.valid();
    }

    @Override
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("enum", new ArrayList<>(allowedValues));
        return schema;
    }

    @Override
    public String getDescription() {
        return description;
    }

    static EnumSchema<?> fromJsonSchema(Map<String, Object> schema) {
        Object enumObj = schema.get("enum");
        if (!(enumObj instanceof List<?> list)) {
            throw new IllegalArgumentException("'enum' must be a list, got " + JsonSchemaParserHelpers.typeOf(enumObj));
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        Set<Object> values = new LinkedHashSet<>((List) list);
        JsonSchemaParserHelpers.rejectUnknownKeywords(schema, Set.of("enum"));
        return Schemas.enumOf(values);
    }
}
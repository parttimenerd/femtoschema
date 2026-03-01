package me.bechberger.util.femtoschema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Schema for boolean values.
 *
 * <h2>Examples:</h2>
 *
 * <h3>Basic Boolean Validation</h3>
 * <pre>{@code
 * var boolSchema = Schemas.bool();
 * assert boolSchema.validate(true).isValid();
 * assert boolSchema.validate(false).isValid();
 * assert !boolSchema.validate("true").isValid();  // String, not boolean
 * assert !boolSchema.validate(1).isValid();       // Number, not boolean
 * }</pre>
 *
 * <h3>Feature Flag Validation</h3>
 * <pre>{@code
 * var featureFlagSchema = Schemas.bool()
 *     .withDescription("Feature toggle for new UI");
 *
 * var featureData = Map.of("enableNewUI", true);
 * var result = featureFlagSchema.validate(featureData.get("enableNewUI"));
 * assert result.isValid();
 * }</pre>
 *
 * <h3>Optional Boolean in Object</h3>
 * <pre>{@code
 * var userSchema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .optional("isActive", Schemas.bool());
 *
 * var userData = Map.of(
 *     "name", "Alice",
 *     "isActive", true
 * );
 * assert userSchema.validate(userData).isValid();
 * }</pre>
 *
 * <h3>JSON Schema Export</h3>
 * <pre>{@code
 * var schema = Schemas.bool()
 *     .withDescription("Indicates if the feature is enabled");
 *
 * var jsonSchema = schema.toJsonSchema();
 * // {
 * //   "type": "boolean"
 * // }
 * }</pre>
 */
public final class BooleanSchema implements TypeSchema {
    private final String description;

    public BooleanSchema() {
        this("boolean");
    }
    
    private BooleanSchema(String description) {
        this.description = description;
    }
    
    public BooleanSchema withDescription(String desc) {
        return new BooleanSchema(desc);
    }

    @Override
    public ValidationResult validate(Object value) {
        return validate(value, "$");
    }
    
    @Override
    public ValidationResult validate(Object value, String path) {
        if (!(value instanceof Boolean)) {
            return ValidationResult.invalid(path, "Expected boolean, got " + typeOf(value));
        }
        return ValidationResult.valid();
    }
    
    @Override
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "boolean");
        return schema;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    static BooleanSchema fromJsonSchema(Map<String, Object> schema) {
        JsonSchemaParserHelpers.rejectUnknownKeywords(schema, Set.of("type"));
        return Schemas.bool();
    }

    private String typeOf(Object obj) {
        if (obj == null) return "null";
        return obj.getClass().getSimpleName().toLowerCase();
    }
}
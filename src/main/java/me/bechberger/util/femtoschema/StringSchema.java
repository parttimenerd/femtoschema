package me.bechberger.util.femtoschema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Schema for string values with optional constraints.
 *
 * <h2>Examples:</h2>
 *
 * <h3>Basic String Schema</h3>
 * <pre>{@code
 * var nameSchema = Schemas.string();
 * var result = nameSchema.validate("Alice");
 * assert result.isValid();
 *
 * // Invalid - not a string
 * result = nameSchema.validate(123);
 * assert !result.isValid();
 * }</pre>
 *
 * <h3>String with Length Constraints</h3>
 * <pre>{@code
 * var usernameSchema = Schemas.string()
 *     .withMinLength(3)
 *     .withMaxLength(20);
 *
 * assert usernameSchema.validate("alice").isValid();
 * assert !usernameSchema.validate("ab").isValid();  // Too short
 * assert !usernameSchema.validate("verylongusernamethatexceedsmax").isValid();  // Too long
 * }</pre>
 *
 * <h3>String with Pattern Matching</h3>
 * <pre>{@code
 * // Email pattern
 * var emailSchema = Schemas.string()
 *     .withPattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
 *
 * assert emailSchema.validate("user@example.com").isValid();
 * assert !emailSchema.validate("invalid-email").isValid();
 * }</pre>
 *
 * <h3>String with Description</h3>
 * <pre>{@code
 * var schema = Schemas.string()
 *     .withDescription("User's first name")
 *     .withMinLength(1)
 *     .withMaxLength(50);
 * }</pre>
 *
 * <h3>JSON Schema Export</h3>
 * <pre>{@code
 * var schema = Schemas.string()
 *     .withMinLength(5)
 *     .withMaxLength(100)
 *     .withPattern("[A-Z].*");
 *
 * var jsonSchema = schema.toJsonSchema();
 * // {
 * //   "type": "string",
 * //   "minLength": 5,
 * //   "maxLength": 100,
 * //   "pattern": "[A-Z].*"
 * // }
 * }</pre>
 */
public final class StringSchema implements TypeSchema {
    private final String description;
    private final Integer minLength;
    private final Integer maxLength;
    private final String pattern;
    
    public StringSchema() {
        this("string", null, null, null);
    }
    
    private StringSchema(String description, Integer minLength, Integer maxLength, String pattern) {
        this.description = description;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.pattern = pattern;
    }
    
    public StringSchema withMinLength(int length) {
        return new StringSchema(description, length, maxLength, pattern);
    }
    
    public StringSchema withMaxLength(int length) {
        return new StringSchema(description, minLength, length, pattern);
    }
    
    public StringSchema withPattern(String regex) {
        return new StringSchema(description, minLength, maxLength, regex);
    }
    
    public StringSchema withDescription(String desc) {
        return new StringSchema(desc, minLength, maxLength, pattern);
    }
    
    @Override
    public ValidationResult validate(Object value) {
        return validate(value, "$");
    }
    
    @Override
    public ValidationResult validate(Object value, String path) {
        if (!(value instanceof String)) {
            return ValidationResult.invalid(path, "Expected string, got " + typeOf(value));
        }
        
        String str = (String) value;
        
        if (minLength != null && str.length() < minLength) {
            return ValidationResult.invalid(path, "String too short (minimum " + minLength + " characters)");
        }
        
        if (maxLength != null && str.length() > maxLength) {
            return ValidationResult.invalid(path, "String too long (maximum " + maxLength + " characters)");
        }
        
        if (pattern != null && !str.matches(pattern)) {
            return ValidationResult.invalid(path, "String does not match pattern: " + pattern);
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        if (minLength != null) schema.put("minLength", minLength);
        if (maxLength != null) schema.put("maxLength", maxLength);
        if (pattern != null) schema.put("pattern", pattern);
        return schema;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    static StringSchema fromJsonSchema(Map<String, Object> schema) {
        StringSchema s = Schemas.string();

        Integer minLength = JsonSchemaParserHelpers.asInteger(schema.get("minLength"));
        Integer maxLength = JsonSchemaParserHelpers.asInteger(schema.get("maxLength"));
        Object patternObj = schema.get("pattern");

        if (minLength != null) s = s.withMinLength(minLength);
        if (maxLength != null) s = s.withMaxLength(maxLength);
        if (patternObj instanceof String pattern) s = s.withPattern(pattern);
        else if (patternObj != null) throw new IllegalArgumentException("'pattern' must be a string");

        JsonSchemaParserHelpers.rejectUnknownKeywords(schema, Set.of("type", "minLength", "maxLength", "pattern"));
        return s;
    }

    private String typeOf(Object obj) {
        if (obj == null) return "null";
        return obj.getClass().getSimpleName().toLowerCase();
    }
}
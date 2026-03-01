package me.bechberger.util.femtoschema;

import java.util.*;

/**
 * Schema for numeric values (int, long, double, float, BigDecimal, etc.).
 *
 * <h2>Examples:</h2>
 *
 * <h3>Basic Number Schema</h3>
 * <pre>{@code
 * var numberSchema = Schemas.number();
 * assert numberSchema.validate(42).isValid();
 * assert numberSchema.validate(3.14).isValid();
 * assert !numberSchema.validate("not a number").isValid();
 * }</pre>
 *
 * <h3>Age Validation (Non-negative Numbers)</h3>
 * <pre>{@code
 * var ageSchema = Schemas.number()
 *     .withMinimum(0);
 *
 * assert ageSchema.validate(25).isValid();
 * assert ageSchema.validate(0).isValid();
 * assert !ageSchema.validate(-5).isValid();  // Negative age not allowed
 * }</pre>
 *
 * <h3>Temperature with Min and Max</h3>
 * <pre>{@code
 * var temperatureSchema = Schemas.number()
 *     .withMinimum(-50)
 *     .withMaximum(50);
 *
 * assert temperatureSchema.validate(20).isValid();
 * assert !temperatureSchema.validate(-100).isValid();  // Too cold
 * assert !temperatureSchema.validate(100).isValid();   // Too hot
 * }</pre>
 *
 * <h3>Percentage with Exclusive Bounds</h3>
 * <pre>{@code
 * var percentageSchema = Schemas.number()
 *     .withExclusiveMinimum(0)
 *     .withExclusiveMaximum(100);
 *
 * assert percentageSchema.validate(50).isValid();
 * assert !percentageSchema.validate(0).isValid();    // Must be > 0
 * assert !percentageSchema.validate(100).isValid();  // Must be < 100
 * }</pre>
 *
 * <h3>JSON Schema Export</h3>
 * <pre>{@code
 * var schema = Schemas.number()
 *     .withMinimum(10)
 *     .withMaximum(100);
 *
 * var jsonSchema = schema.toJsonSchema();
 * // {
 * //   "type": "number",
 * //   "minimum": 10.0,
 * //   "maximum": 100.0
 * // }
 * }</pre>
 */
public final class NumberSchema implements TypeSchema {
    private final String description;
    private final Double minimum;
    private final Double maximum;
    private final Double exclusiveMinimum;
    private final Double exclusiveMaximum;

    public NumberSchema() {
        this("number", null, null, null, null);
    }

    private NumberSchema(String description, Double minimum, Double maximum,
                         Double exclusiveMin, Double exclusiveMax) {
        this.description = description;
        this.minimum = minimum;
        this.maximum = maximum;
        this.exclusiveMinimum = exclusiveMin;
        this.exclusiveMaximum = exclusiveMax;
    }

    public NumberSchema withMinimum(double min) {
        return new NumberSchema(description, min, maximum, null, exclusiveMaximum);
    }

    public NumberSchema withMaximum(double max) {
        return new NumberSchema(description, minimum, max, exclusiveMinimum, null);
    }

    public NumberSchema withExclusiveMinimum(double min) {
        return new NumberSchema(description, null, maximum, min, exclusiveMaximum);
    }

    public NumberSchema withExclusiveMaximum(double max) {
        return new NumberSchema(description, minimum, null, exclusiveMinimum, max);
    }

    public NumberSchema withDescription(String desc) {
        return new NumberSchema(desc, minimum, maximum, exclusiveMinimum, exclusiveMaximum);
    }

    @Override
    public ValidationResult validate(Object value) {
        return validate(value, "$");
    }

    @Override
    public ValidationResult validate(Object value, String path) {
        double numValue;

        if (value instanceof Number num) {
            numValue = num.doubleValue();
        } else {
            return ValidationResult.invalid(path, "Expected number, got " + typeOf(value));
        }

        if (minimum != null) {
            if (numValue < minimum) {
                return ValidationResult.invalid(path, "Number less than minimum (" + minimum + ")");
            }
        }

        if (exclusiveMinimum != null) {
            if (numValue <= exclusiveMinimum) {
                return ValidationResult.invalid(path, "Number less than or equal to exclusive minimum (" + exclusiveMinimum + ")");
            }
        }

        if (maximum != null) {
            if (numValue > maximum) {
                return ValidationResult.invalid(path, "Number greater than maximum (" + maximum + ")");
            }
        }

        if (exclusiveMaximum != null) {
            if (numValue >= exclusiveMaximum) {
                return ValidationResult.invalid(path, "Number greater than or equal to exclusive maximum (" + exclusiveMaximum + ")");
            }
        }

        return ValidationResult.valid();
    }

    @Override
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "number");
        if (minimum != null) {
            schema.put("minimum", minimum);
        }
        if (exclusiveMinimum != null) {
            schema.put("exclusiveMinimum", exclusiveMinimum);
        }
        if (maximum != null) {
            schema.put("maximum", maximum);
        }
        if (exclusiveMaximum != null) {
            schema.put("exclusiveMaximum", exclusiveMaximum);
        }
        return schema;
    }

    @Override
    public String getDescription() {
        return description;
    }

    static NumberSchema fromJsonSchema(Map<String, Object> schema) {
        NumberSchema s = Schemas.number();

        Double minimum = JsonSchemaParserHelpers.asDouble(schema.get("minimum"));
        Double maximum = JsonSchemaParserHelpers.asDouble(schema.get("maximum"));
        Double exclusiveMinimum = JsonSchemaParserHelpers.asDouble(schema.get("exclusiveMinimum"));
        Double exclusiveMaximum = JsonSchemaParserHelpers.asDouble(schema.get("exclusiveMaximum"));

        if (minimum != null) s = s.withMinimum(minimum);
        if (maximum != null) s = s.withMaximum(maximum);
        if (exclusiveMinimum != null) s = s.withExclusiveMinimum(exclusiveMinimum);
        if (exclusiveMaximum != null) s = s.withExclusiveMaximum(exclusiveMaximum);

        JsonSchemaParserHelpers.rejectUnknownKeywords(schema, Set.of("type", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum"));
        return s;
    }

    private String typeOf(Object obj) {
        if (obj == null) return "null";
        return obj.getClass().getSimpleName().toLowerCase();
    }
}
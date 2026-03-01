package me.bechberger.util.femtoschema;

import java.util.*;

/**
 * Schema for object/map values with typed properties.
 *
 * <h2>Examples:</h2>
 *
 * <h3>Simple User Object</h3>
 * <pre>{@code
 * var userSchema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number())
 *     .optional("email", Schemas.string());
 *
 * var userData = Map.of(
 *     "name", "Alice",
 *     "age", 30.0,
 *     "email", "alice@example.com"
 * );
 * assert userSchema.validate(userData).isValid();
 * }</pre>
 *
 * <h3>Nested Objects - Address in User</h3>
 * <pre>{@code
 * var addressSchema = Schemas.object()
 *     .required("street", Schemas.string())
 *     .required("city", Schemas.string())
 *     .required("zipCode", Schemas.string());
 *
 * var userSchema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .optional("address", addressSchema);
 *
 * var userData = Map.of(
 *     "name", "Bob",
 *     "address", Map.of(
 *         "street", "123 Main St",
 *         "city", "San Francisco",
 *         "zipCode", "94107"
 *     )
 * );
 * assert userSchema.validate(userData).isValid();
 * }</pre>
 *
 * <h3>Disallow Additional Properties</h3>
 * <pre>{@code
 * var strictSchema = Schemas.object()
 *     .required("id", Schemas.number())
 *     .required("name", Schemas.string())
 *     .allowAdditionalProperties(false);
 *
 * var validData = Map.of("id", 1.0, "name", "Alice");
 * assert strictSchema.validate(validData).isValid();
 *
 * var invalidData = Map.of(
 *     "id", 1.0,
 *     "name", "Alice",
 *     "extra", "field"  // Not allowed
 * );
 * assert !strictSchema.validate(invalidData).isValid();
 * }</pre>
 *
 * <h3>Validation with Error Reporting</h3>
 * <pre>{@code
 * var schema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("age", Schemas.number().withMinimum(0));
 *
 * var invalidData = Map.of(
 *     "name", "Charlie",
 *     "age", -5.0  // Invalid: negative age
 * );
 *
 * var result = schema.validate(invalidData);
 * if (!result.isValid()) {
 *     result.getErrors().forEach(e ->
 *         System.out.println(e.path() + ": " + e.message())
 *     );
 *     // Output: $.age: Number less than minimum (0)
 * }
 * }</pre>
 *
 * <h3>JSON Schema Export</h3>
 * <pre>{@code
 * var schema = Schemas.object()
 *     .required("name", Schemas.string())
 *     .required("active", Schemas.bool());
 *
 * var jsonSchema = schema.toJsonSchema();
 * // {
 * //   "type": "object",
 * //   "properties": {
 * //     "name": {"type": "string"},
 * //     "active": {"type": "boolean"}
 * //   },
 * //   "required": ["name", "active"],
 * //   "additionalProperties": true
 * // }
 * }</pre>
 */
public final class ObjectSchema implements TypeSchema {
    private final Map<String, TypeSchema> properties;
    private final Set<String> required;
    private final boolean additionalPropertiesAllowed;
    private final String description;

    public ObjectSchema() {
        this(new LinkedHashMap<>(), new HashSet<>(), true, "object");
    }

    private ObjectSchema(Map<String, TypeSchema> properties, Set<String> required,
                        boolean additionalPropertiesAllowed, String description) {
        this.properties = new LinkedHashMap<>(properties);
        this.required = new HashSet<>(required);
        this.additionalPropertiesAllowed = additionalPropertiesAllowed;
        this.description = description;
    }

    /**
     * Adds a required property to this object schema.
     */
    public ObjectSchema property(String name, TypeSchema schema) {
        return required(name, schema);
    }

    /**
     * Adds a required property to this object schema.
     */
    public ObjectSchema required(String name, TypeSchema schema) {
        Map<String, TypeSchema> newProps = new LinkedHashMap<>(properties);
        newProps.put(name, schema);
        Set<String> newRequired = new HashSet<>(required);
        newRequired.add(name);
        return new ObjectSchema(newProps, newRequired, additionalPropertiesAllowed, description);
    }

    /**
     * Adds an optional property to this object schema.
     */
    public ObjectSchema optional(String name, TypeSchema schema) {
        Map<String, TypeSchema> newProps = new LinkedHashMap<>(properties);
        newProps.put(name, schema);
        return new ObjectSchema(newProps, required, additionalPropertiesAllowed, description);
    }

    /**
     * Sets whether additional properties are allowed (default: true).
     */
    public ObjectSchema allowAdditionalProperties(boolean allow) {
        return new ObjectSchema(properties, required, allow, description);
    }

    public ObjectSchema withDescription(String desc) {
        return new ObjectSchema(properties, required, additionalPropertiesAllowed, desc);
    }

    @Override
    public ValidationResult validate(Object value) {
        return validate(value, "$");
    }

    @Override
    public ValidationResult validate(Object value, String path) {
        if (!(value instanceof Map)) {
            return ValidationResult.invalid(path, "Expected object, got " + typeOf(value));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        List<ValidationResult.ValidationError> errors = new ArrayList<>();

        // Check required properties
        for (String propName : required) {
            if (!map.containsKey(propName)) {
                errors.add(new ValidationResult.ValidationError(path + "." + propName,
                    "Required property missing"));
            }
        }

        // Validate each property
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String propName = entry.getKey();
            Object propValue = entry.getValue();
            String propPath = path + "." + propName;

            if (properties.containsKey(propName)) {
                ValidationResult result = properties.get(propName).validate(propValue, propPath);
                if (!result.isValid()) {
                    errors.addAll(result.getErrors());
                }
            } else if (!additionalPropertiesAllowed) {
                errors.add(new ValidationResult.ValidationError(propPath,
                    "Additional properties are not allowed"));
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
        schema.put("type", "object");

        if (!properties.isEmpty()) {
            Map<String, Object> props = new LinkedHashMap<>();
            for (Map.Entry<String, TypeSchema> entry : properties.entrySet()) {
                props.put(entry.getKey(), entry.getValue().toJsonSchema());
            }
            schema.put("properties", props);
        }

        if (!required.isEmpty()) {
            schema.put("required", new ArrayList<>(required));
        }

        schema.put("additionalProperties", additionalPropertiesAllowed);

        return schema;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Returns an unmodifiable set of the required property names.
     */
    public Set<String> requiredKeys() {
        return Set.copyOf(required);
    }

    static ObjectSchema fromJsonSchema(Map<String, Object> schema) {
        ObjectSchema s = Schemas.object();

        Object propsObj = schema.get("properties");
        Map<String, Object> props = null;
        if (propsObj != null) {
            if (!(propsObj instanceof Map<?, ?> propsRaw)) {
                throw new IllegalArgumentException("'properties' must be an object/map");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) propsRaw;
            props = cast;
        }

        Set<String> required = new HashSet<>();
        Object requiredObj = schema.get("required");
        if (requiredObj != null) {
            if (!(requiredObj instanceof List<?> reqList)) {
                throw new IllegalArgumentException("'required' must be a list");
            }
            for (Object o : reqList) {
                if (!(o instanceof String str)) {
                    throw new IllegalArgumentException("'required' entries must be strings");
                }
                required.add(str);
            }
        }

        if (!required.isEmpty() && props == null) {
            throw new IllegalArgumentException("'required' is present but 'properties' is missing");
        }

        if (props != null) {
            for (Map.Entry<String, Object> e : props.entrySet()) {
                String name = e.getKey();
                Object propSchemaObj = e.getValue();
                if (!(propSchemaObj instanceof Map<?, ?> propSchemaRaw)) {
                    throw new IllegalArgumentException("Property schema for '" + name + "' must be an object");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> propSchema = (Map<String, Object>) propSchemaRaw;
                TypeSchema parsed = JsonSchemaParser.parse(propSchema);
                if (required.contains(name)) {
                    s = s.required(name, parsed);
                } else {
                    s = s.optional(name, parsed);
                }
            }
        }

        Boolean additionalProps = JsonSchemaParserHelpers.asBoolean(schema.get("additionalProperties"));
        if (additionalProps != null) {
            s = s.allowAdditionalProperties(additionalProps);
        }

        JsonSchemaParserHelpers.rejectUnknownKeywords(schema, Set.of("type", "properties", "required", "additionalProperties"));
        return s;
    }

    private String typeOf(Object obj) {
        if (obj == null) return "null";
        return obj.getClass().getSimpleName().toLowerCase();
    }
}
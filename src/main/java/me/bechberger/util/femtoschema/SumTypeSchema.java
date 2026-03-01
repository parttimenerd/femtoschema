package me.bechberger.util.femtoschema;

import java.util.*;

/**
 * Schema for sum types (discriminated unions) - a value must match exactly one of several schemas.
 * Uses a discriminator field to determine which variant to validate against.
 *
 * <h2>Examples:</h2>
 *
 * <h3>Basic Notification Types</h3>
 * <pre>{@code
 * var emailVariant = Schemas.object()
 *     .required("type", Schemas.enumOf("email"))
 *     .required("address", Schemas.string());
 *
 * var smsVariant = Schemas.object()
 *     .required("type", Schemas.enumOf("sms"))
 *     .required("phoneNumber", Schemas.string());
 *
 * var notificationSchema = Schemas.sumType("type")
 *     .variant("email", emailVariant)
 *     .variant("sms", smsVariant);
 *
 * // Valid email
 * var emailNotif = Map.of("type", "email", "address", "user@example.com");
 * assert notificationSchema.validate(emailNotif).isValid();
 *
 * // Valid SMS
 * var smsNotif = Map.of("type", "sms", "phoneNumber", "+1234567890");
 * assert notificationSchema.validate(smsNotif).isValid();
 * }</pre>
 *
 * <h3>Payment Methods</h3>
 * <pre>{@code
 * var creditCardVariant = Schemas.object()
 *     .required("method", Schemas.enumOf("credit_card"))
 *     .required("cardNumber", Schemas.string())
 *     .required("cvv", Schemas.string());
 *
 * var bankVariant = Schemas.object()
 *     .required("method", Schemas.enumOf("bank_transfer"))
 *     .required("accountNumber", Schemas.string())
 *     .required("routingNumber", Schemas.string());
 *
 * var paypalVariant = Schemas.object()
 *     .required("method", Schemas.enumOf("paypal"))
 *     .required("email", Schemas.string());
 *
 * var paymentSchema = Schemas.sumType("method")
 *     .variant("credit_card", creditCardVariant)
 *     .variant("bank_transfer", bankVariant)
 *     .variant("paypal", paypalVariant);
 *
 * var creditPayment = Map.of(
 *     "method", "credit_card",
 *     "cardNumber", "4111111111111111",
 *     "cvv", "123"
 * );
 * assert paymentSchema.validate(creditPayment).isValid();
 * }</pre>
 *
 * <h3>Response Types (Success/Error)</h3>
 * <pre>{@code
 * var successVariant = Schemas.object()
 *     .required("status", Schemas.enumOf("success"))
 *     .required("data", Schemas.object()
 *         .required("id", Schemas.number())
 *         .required("message", Schemas.string()));
 *
 * var errorVariant = Schemas.object()
 *     .required("status", Schemas.enumOf("error"))
 *     .required("error", Schemas.object()
 *         .required("code", Schemas.number())
 *         .required("message", Schemas.string()));
 *
 * var responseSchema = Schemas.sumType("status")
 *     .variant("success", successVariant)
 *     .variant("error", errorVariant);
 *
 * var successResponse = Map.of(
 *     "status", "success",
 *     "data", Map.of("id", 123.0, "message", "Operation completed")
 * );
 * assert responseSchema.validate(successResponse).isValid();
 * }</pre>
 *
 * <h3>Event Types</h3>
 * <pre>{@code
 * var userCreatedEvent = Schemas.object()
 *     .required("eventType", Schemas.enumOf("user.created"))
 *     .required("userId", Schemas.string())
 *     .required("email", Schemas.string());
 *
 * var userDeletedEvent = Schemas.object()
 *     .required("eventType", Schemas.enumOf("user.deleted"))
 *     .required("userId", Schemas.string());
 *
 * var eventSchema = Schemas.sumType("eventType")
 *     .variant("user.created", userCreatedEvent)
 *     .variant("user.deleted", userDeletedEvent);
 *
 * var event = Map.of(
 *     "eventType", "user.created",
 *     "userId", "user-123",
 *     "email", "newuser@example.com"
 * );
 * assert eventSchema.validate(event).isValid();
 * }</pre>
 *
 * <h3>Error Handling - Missing Discriminator</h3>
 * <pre>{@code
 * var schema = Schemas.sumType("type")
 *     .variant("a", Schemas.object().required("type", Schemas.enumOf("a")))
 *     .variant("b", Schemas.object().required("type", Schemas.enumOf("b")));
 *
 * var invalidData = Map.of("data", "value");  // Missing "type" field
 *
 * var result = schema.validate(invalidData);
 * if (!result.isValid()) {
 *     result.getErrors().forEach(e ->
 *         System.out.println(e.path() + ": " + e.message())
 *     );
 *     // Output: $.type: Discriminator field 'type' is required
 * }
 * }</pre>
 *
 * <h3>Error Handling - Unknown Discriminator Value</h3>
 * <pre>{@code
 * var schema = Schemas.sumType("type")
 *     .variant("email", Schemas.object().required("type", Schemas.enumOf("email")))
 *     .variant("sms", Schemas.object().required("type", Schemas.enumOf("sms")));
 *
 * var invalidNotif = Map.of("type", "slack");  // Unknown variant
 *
 * var result = schema.validate(invalidNotif);
 * if (!result.isValid()) {
 *     result.getErrors().forEach(e ->
 *         System.out.println(e.message())
 *     );
 *     // Output: Unknown discriminator value 'slack'. Must be one of: [email, sms]
 * }
 * }</pre>
 *
 * <h3>JSON Schema Export</h3>
 * <pre>{@code
 * var schema = Schemas.sumType("type")
 *     .variant("a", Schemas.object().required("type", Schemas.enumOf("a")))
 *     .variant("b", Schemas.object().required("type", Schemas.enumOf("b")));
 *
 * var jsonSchema = schema.toJsonSchema();
 * // {
 * //   "oneOf": [...],
 * //   "discriminator": {
 * //     "propertyName": "type",
 * //     "mapping": {"a": "a", "b": "b"}
 * //   }
 * // }
 * }</pre>
 */
public final class SumTypeSchema implements TypeSchema {
    private final String discriminatorField;
    private final Map<String, TypeSchema> variants; // discriminator value -> schema
    private final String description;

    /**
     * Creates a sum type schema with a discriminator field.
     * @param discriminatorField the name of the field that discriminates between variants
     */
    public SumTypeSchema(String discriminatorField) {
        this(discriminatorField, new LinkedHashMap<>(), "sum type");
    }

    private SumTypeSchema(String discriminatorField, Map<String, TypeSchema> variants, String description) {
        this.discriminatorField = Objects.requireNonNull(discriminatorField, "discriminatorField cannot be null");
        this.variants = new LinkedHashMap<>(variants);
        this.description = description;
    }

    /**
     * Adds a variant schema for a specific discriminator value.
     * @param discriminatorValue the value of the discriminator field for this variant
     * @param schema the schema this variant must match
     */
    public SumTypeSchema variant(String discriminatorValue, TypeSchema schema) {
        Map<String, TypeSchema> newVariants = new LinkedHashMap<>(variants);
        newVariants.put(discriminatorValue, schema);
        return new SumTypeSchema(discriminatorField, newVariants, description);
    }

    public SumTypeSchema withDescription(String desc) {
        return new SumTypeSchema(discriminatorField, variants, desc);
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

        // Get the discriminator value
        Object discriminatorValue = map.get(discriminatorField);

        if (discriminatorValue == null) {
            return ValidationResult.invalid(path + "." + discriminatorField,
                "Discriminator field '" + discriminatorField + "' is required");
        }

        String discriminatorStr = String.valueOf(discriminatorValue);

        if (!variants.containsKey(discriminatorStr)) {
            return ValidationResult.invalid(path + "." + discriminatorField,
                "Unknown discriminator value '" + discriminatorStr + "'. Must be one of: " + variants.keySet());
        }

        // Validate against the appropriate variant schema
        TypeSchema variantSchema = variants.get(discriminatorStr);
        return variantSchema.validate(value, path);
    }

    @Override
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();

        List<Map<String, Object>> oneOfSchemas = new ArrayList<>();
        for (Map.Entry<String, TypeSchema> entry : variants.entrySet()) {
            Map<String, Object> variantSchema = new LinkedHashMap<>(entry.getValue().toJsonSchema());
            oneOfSchemas.add(variantSchema);
        }

        schema.put("oneOf", oneOfSchemas);

        // Add discriminator information
        Map<String, Object> discriminator = new LinkedHashMap<>();
        discriminator.put("propertyName", discriminatorField);
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String variant : variants.keySet()) {
            mapping.put(variant, variant);
        }
        discriminator.put("mapping", mapping);
        schema.put("discriminator", discriminator);

        return schema;
    }

    /**
     * Parses a JSON Schema map into a SumTypeSchema.
     * @param schema the JSON Schema map
     * @return the corresponding SumTypeSchema
     */
    static SumTypeSchema fromJsonSchema(Map<String, Object> schema) {
        Object discObj = schema.get("discriminator");
        if (!(discObj instanceof Map<?, ?> discMapRaw)) {
            throw new IllegalArgumentException("Only discriminated unions are supported for 'oneOf' (missing 'discriminator')");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> discMap = (Map<String, Object>) discMapRaw;

        Object propertyNameObj = discMap.get("propertyName");
        if (!(propertyNameObj instanceof String discriminatorField)) {
            throw new IllegalArgumentException("'discriminator.propertyName' must be a string");
        }

        Object oneOfObj = schema.get("oneOf");
        if (!(oneOfObj instanceof List<?> oneOfList)) {
            throw new IllegalArgumentException("'oneOf' must be a list, got " + JsonSchemaParserHelpers.typeOf(oneOfObj));
        }

        SumTypeSchema sum = Schemas.sumType(discriminatorField);

        for (Object variantObj : oneOfList) {
            if (!(variantObj instanceof Map<?, ?> variantMapRaw)) {
                throw new IllegalArgumentException("'oneOf' entries must be objects, got " + JsonSchemaParserHelpers.typeOf(variantObj));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> variantMap = (Map<String, Object>) variantMapRaw;

            TypeSchema variantSchema = JsonSchemaParser.parse(variantMap);
            String discriminatorValue = inferDiscriminatorValue(discriminatorField, variantSchema);
            sum = sum.variant(discriminatorValue, variantSchema);
        }

        JsonSchemaParserHelpers.rejectUnknownKeywords(schema, Set.of("oneOf", "discriminator"));
        return sum;
    }

    private static String inferDiscriminatorValue(String discriminatorField, TypeSchema variantSchema) {
        if (!(variantSchema instanceof ObjectSchema)) {
            throw new IllegalArgumentException("Sum type variants must be object schemas to infer discriminator value");
        }

        Map<String, Object> exported = variantSchema.toJsonSchema();

        Object propsObj = exported.get("properties");
        if (!(propsObj instanceof Map<?, ?> propsRaw)) {
            throw new IllegalArgumentException("Variant object schema must have 'properties' to infer discriminator value");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) propsRaw;

        Object discSchemaObj = props.get(discriminatorField);
        if (!(discSchemaObj instanceof Map<?, ?> discSchemaRaw)) {
            throw new IllegalArgumentException("Variant schema must define discriminator property '" + discriminatorField + "'");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> discSchema = (Map<String, Object>) discSchemaRaw;

        Object enumObj = discSchema.get("enum");
        if (!(enumObj instanceof List<?> enumList) || enumList.size() != 1) {
            throw new IllegalArgumentException("Discriminator property must be an enum with exactly one value");
        }
        return String.valueOf(enumList.get(0));
    }

    @Override
    public String getDescription() {
        return description;
    }

    private String typeOf(Object obj) {
        if (obj == null) return "null";
        return obj.getClass().getSimpleName().toLowerCase();
    }
}
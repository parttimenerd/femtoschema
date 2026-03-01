package me.bechberger.util.femtoschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.InputFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Oracle tests using json-schema-validator to validate that our schema
 * implementation produces correct JSON Schema output.
 */
@DisplayName("JSON Schema Oracle Tests")
class JsonSchemaOracleTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    @Test
    @DisplayName("Complex object with constraints should validate correctly")
    void testComplexObjectSchema() throws Exception {
        var schema = Schemas.object()
            .required("name", Schemas.string().withMinLength(1).withMaxLength(100))
            .required("age", Schemas.number().withMinimum(0).withMaximum(150))
            .required("email", Schemas.string().withPattern("^[^@]+@[^@]+\\.[^@]+$"))
            .optional("tags", Schemas.array(Schemas.string()).withMinItems(0).withMaxItems(10))
            .allowAdditionalProperties(false);

        var schemaMap = schema.toJsonSchema();
        var schemaJson = mapper.writeValueAsString(schemaMap);
        var jsonSchema = schemaRegistry.getSchema(schemaJson);

        // Valid object
        var validData = new LinkedHashMap<String, Object>();
        validData.put("name", "Alice");
        validData.put("age", 30);
        validData.put("email", "alice@example.com");
        validData.put("tags", List.of("engineer", "java"));
        var validJson = mapper.writeValueAsString(validData);
        assertTrue(jsonSchema.validate(validJson, InputFormat.JSON).isEmpty());

        // Missing required field
        var missingRequired = new LinkedHashMap<String, Object>();
        missingRequired.put("name", "Bob");
        missingRequired.put("age", 25);
        var missingJson = mapper.writeValueAsString(missingRequired);
        assertFalse(jsonSchema.validate(missingJson, InputFormat.JSON).isEmpty());

        // Invalid email format
        var invalidEmail = new LinkedHashMap<>(validData);
        invalidEmail.put("email", "not-an-email");
        var invalidJson = mapper.writeValueAsString(invalidEmail);
        assertFalse(jsonSchema.validate(invalidJson, InputFormat.JSON).isEmpty());

        // Age out of range
        var invalidAge = new LinkedHashMap<>(validData);
        invalidAge.put("age", 200);
        var ageJson = mapper.writeValueAsString(invalidAge);
        assertFalse(jsonSchema.validate(ageJson, InputFormat.JSON).isEmpty());
    }

    @Test
    @DisplayName("Deeply nested structures should validate correctly")
    void testDeeplyNestedSchema() throws Exception {
        var addressSchema = Schemas.object()
            .required("street", Schemas.string())
            .required("city", Schemas.string())
            .required("zipCode", Schemas.string().withPattern("^\\d{5}$"));

        var companySchema = Schemas.object()
            .required("name", Schemas.string())
            .required("address", addressSchema);

        var personSchema = Schemas.object()
            .required("name", Schemas.string())
            .required("company", companySchema)
            .required("yearsExperience", Schemas.number().withMinimum(0));

        var schemaMap = personSchema.toJsonSchema();
        var schemaJson = mapper.writeValueAsString(schemaMap);
        var jsonSchema = schemaRegistry.getSchema(schemaJson);

        // Valid deeply nested object
        var address = new LinkedHashMap<String, Object>();
        address.put("street", "123 Main St");
        address.put("city", "Springfield");
        address.put("zipCode", "12345");

        var company = new LinkedHashMap<String, Object>();
        company.put("name", "TechCorp");
        company.put("address", address);

        var person = new LinkedHashMap<String, Object>();
        person.put("name", "Alice");
        person.put("company", company);
        person.put("yearsExperience", 10);

        var validJson = mapper.writeValueAsString(person);
        assertTrue(jsonSchema.validate(validJson, InputFormat.JSON).isEmpty());

        // Invalid zip code
        var invalidAddress = new LinkedHashMap<>(address);
        invalidAddress.put("zipCode", "invalid");
        var invalidCompany = new LinkedHashMap<>(company);
        invalidCompany.put("address", invalidAddress);
        var invalidPerson = new LinkedHashMap<>(person);
        invalidPerson.put("company", invalidCompany);

        var invalidJson = mapper.writeValueAsString(invalidPerson);
        assertFalse(jsonSchema.validate(invalidJson, InputFormat.JSON).isEmpty());
    }

    @Test
    @DisplayName("Array of constrained objects should validate correctly")
    void testArrayOfConstrainedObjects() throws Exception {
        var itemSchema = Schemas.object()
            .required("id", Schemas.number().withMinimum(1))
            .required("name", Schemas.string().withMinLength(1).withMaxLength(50))
            .required("quantity", Schemas.number().withMinimum(0))
            .required("status", Schemas.enumOf("PENDING", "ACTIVE", "COMPLETED"));

        var schema = Schemas.array(itemSchema)
            .withMinItems(1)
            .withMaxItems(100);

        var schemaMap = schema.toJsonSchema();
        var schemaJson = mapper.writeValueAsString(schemaMap);
        var jsonSchema = schemaRegistry.getSchema(schemaJson);

        // Valid array with multiple items
        var item1 = new LinkedHashMap<String, Object>();
        item1.put("id", 1);
        item1.put("name", "Item A");
        item1.put("quantity", 10);
        item1.put("status", "ACTIVE");

        var item2 = new LinkedHashMap<String, Object>();
        item2.put("id", 2);
        item2.put("name", "Item B");
        item2.put("quantity", 5);
        item2.put("status", "PENDING");

        var validData = List.of(item1, item2);
        var validJson = mapper.writeValueAsString(validData);
        assertTrue(jsonSchema.validate(validJson, InputFormat.JSON).isEmpty());

        // Too few items
        var emptyArray = List.of();
        var emptyJson = mapper.writeValueAsString(emptyArray);
        assertFalse(jsonSchema.validate(emptyJson, InputFormat.JSON).isEmpty());

        // Invalid enum value
        var invalidItem = new LinkedHashMap<>(item1);
        invalidItem.put("status", "INVALID");
        var invalidData = List.of(invalidItem);
        var invalidJson = mapper.writeValueAsString(invalidData);
        assertFalse(jsonSchema.validate(invalidJson, InputFormat.JSON).isEmpty());
    }

    @Test
    @DisplayName("Complex schema with optional fields and constraints should validate correctly")
    void testComplexSchemaWithOptionals() throws Exception {
        var contactSchema = Schemas.object()
            .optional("email", Schemas.string().withPattern("^[^@]+@[^@]+\\.[^@]+$"))
            .optional("phone", Schemas.string().withPattern("^\\d{10}$"))
            .allowAdditionalProperties(false);

        var schema = Schemas.object()
            .required("id", Schemas.number().withMinimum(1))
            .required("name", Schemas.string())
            .optional("contact", contactSchema)
            .optional("preferences", Schemas.object()
                .optional("notifications", Schemas.bool())
                .optional("newsletter", Schemas.bool()))
            .allowAdditionalProperties(false);

        var schemaMap = schema.toJsonSchema();
        var schemaJson = mapper.writeValueAsString(schemaMap);
        var jsonSchema = schemaRegistry.getSchema(schemaJson);

        // Valid with all optional fields
        var fullData = new LinkedHashMap<String, Object>();
        fullData.put("id", 1);
        fullData.put("name", "Test User");
        var contact = new LinkedHashMap<String, Object>();
        contact.put("email", "user@example.com");
        contact.put("phone", "1234567890");
        fullData.put("contact", contact);
        var prefs = new LinkedHashMap<String, Object>();
        prefs.put("notifications", true);
        prefs.put("newsletter", false);
        fullData.put("preferences", prefs);
        var fullJson = mapper.writeValueAsString(fullData);
        assertTrue(jsonSchema.validate(fullJson, InputFormat.JSON).isEmpty());

        // Valid with minimal required fields
        var minimalData = new LinkedHashMap<String, Object>();
        minimalData.put("id", 2);
        minimalData.put("name", "Minimal User");
        var minimalJson = mapper.writeValueAsString(minimalData);
        assertTrue(jsonSchema.validate(minimalJson, InputFormat.JSON).isEmpty());

        // Invalid phone format
        var badPhone = new LinkedHashMap<>(contact);
        badPhone.put("phone", "123");
        var invalidContact = new LinkedHashMap<>(fullData);
        invalidContact.put("contact", badPhone);
        var invalidJson = mapper.writeValueAsString(invalidContact);
        assertFalse(jsonSchema.validate(invalidJson, InputFormat.JSON).isEmpty());
    }
}
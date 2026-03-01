package me.bechberger.util.femtoschema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JSON Schema parsing")
class JsonSchemaParseTest {

    @Test
    @DisplayName("Round-trip: schema -> json schema map -> schema")
    void testRoundTripComplexSchema() {
        var schema = Schemas.object()
            .required("name", Schemas.string().withMinLength(1).withMaxLength(100))
            .required("age", Schemas.number().withMinimum(0).withMaximum(150))
            .required("email", Schemas.string().withPattern("^[^@]+@[^@]+\\.[^@]+$"))
            .optional("tags", Schemas.array(Schemas.string()).withMinItems(0).withMaxItems(10))
            .allowAdditionalProperties(false);

        Map<String, Object> exported = schema.toJsonSchema();
        TypeSchema parsed = Schemas.fromJsonSchema(exported);

        assertEquals(exported, parsed.toJsonSchema());
    }

    @Test
    @DisplayName("$comment is ignored at any nesting level")
    void testIgnoreComment() {
        var schema = Schemas.array(
            Schemas.object()
                .required("status", Schemas.enumOf("PENDING", "ACTIVE"))
                .optional("flag", Schemas.bool())
        ).withMinItems(1);

        Map<String, Object> exported = new LinkedHashMap<>(schema.toJsonSchema());
        exported.put("$comment", "top-level comment");

        // add a nested comment
        Map<String, Object> items = castMap(exported.get("items"));
        items.put("$comment", "items comment");
        Map<String, Object> props = castMap(items.get("properties"));
        Map<String, Object> status = castMap(props.get("status"));
        status.put("$comment", "enum comment");

        TypeSchema parsed = Schemas.fromJsonSchema(exported);
        assertEquals(schema.toJsonSchema(), parsed.toJsonSchema());
    }

    @Test
    @DisplayName("fromJsonSchemaString parses JSON schema strings (and ignores $comment)")
    void testFromJsonSchemaString() {
        String json = "{" +
            "\"type\":\"array\"," +
            "\"$comment\":\"top\"," +
            "\"minItems\":1," +
            "\"items\":{" +
            "  \"type\":\"string\"," +
            "  \"$comment\":\"nested\"," +
            "  \"minLength\":2" +
            "}" +
            "}";

        TypeSchema parsed = Schemas.fromJsonSchemaString(json);

        var expected = Schemas.array(Schemas.string().withMinLength(2)).withMinItems(1);
        assertEquals(expected.toJsonSchema(), parsed.toJsonSchema());
    }

    private static Map<String, Object> castMap(Object obj) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) assertInstanceOf(Map.class, obj);
        return map;
    }
}
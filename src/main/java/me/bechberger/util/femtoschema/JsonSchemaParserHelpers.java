package me.bechberger.util.femtoschema;

import java.util.Map;
import java.util.Set;

/** Internal helpers shared by JSON Schema parsing code across schema classes. */
final class JsonSchemaParserHelpers {

    private JsonSchemaParserHelpers() {
    }

    static void rejectUnknownKeywords(Map<String, Object> schema, Set<String> allowed) {
        for (String key : schema.keySet()) {
            if (key.equals("$comment")) continue;
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unsupported JSON Schema keyword: '" + key + "'");
            }
        }
    }

    static Integer asInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Long l) return Math.toIntExact(l);
        if (obj instanceof Number n) return n.intValue();
        throw new IllegalArgumentException("Expected integer, got " + typeOf(obj));
    }

    static Double asDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        throw new IllegalArgumentException("Expected number, got " + typeOf(obj));
    }

    static Boolean asBoolean(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Boolean b) return b;
        throw new IllegalArgumentException("Expected boolean, got " + typeOf(obj));
    }

    static String typeOf(Object obj) {
        if (obj == null) return "null";
        return obj.getClass().getSimpleName().toLowerCase();
    }
}
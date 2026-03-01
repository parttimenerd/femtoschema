# femtoschema

A tiny JSON Schema DSL for Java that is built for simplicity
and not for performance, alongside [femtojson](https://github.com/parttimenerd/femtojson).
It provides a fluent API to define schemas,
validate Java JSON-like values (maps, lists, strings, numbers, booleans, null),
and export to a [JSON Schema (Draft 2020-12)](https://json-schema.org/draft/2020-12) representation.

It does not aim to support every feature of JSON Schema, see below for the supported features.

## Installation

### Maven

```xml
<dependency>
    <groupId>me.bechberger.util</groupId>
    <artifactId>femtoschema</artifactId>
    <version>0.0.0</version>
</dependency>
```

## Usage

### Quick Start: define + validate

```java
import me.bechberger.util.femtoschema.Schemas;
import me.bechberger.util.femtoschema.ValidationResult;

import java.util.Map;

public class Example {
    public static void main(String[] args) {
        var userSchema = Schemas.object()
            .required("name", Schemas.string().withMinLength(1))
            .required("age", Schemas.number().withMinimum(0))
            .optional("email", Schemas.string());

        var userData = Map.of(
            "name", "Alice",
            "age", 30.0,
            "email", "alice@example.com"
        );

        ValidationResult result = userSchema.validate(userData);

        if (!result.isValid()) {
            result.getErrors().forEach(e ->
                System.out.println(e.path() + ": " + e.message())
            );
        }
    }
}
```

### Building object schemas

```java
import me.bechberger.util.femtoschema.Schemas;

var personSchema = Schemas.object()
    .required("name", Schemas.string())
    .required("age", Schemas.number().withMinimum(0).withMaximum(150))
    .optional("email", Schemas.string());
```

### Nested objects and arrays

```java
import me.bechberger.util.femtoschema.Schemas;

import java.util.List;
import java.util.Map;

var addressSchema = Schemas.object()
    .required("street", Schemas.string())
    .required("city", Schemas.string())
    .required("zipCode", Schemas.string());

var employeeSchema = Schemas.object()
    .required("name", Schemas.string())
    .required("address", addressSchema);

var employeesSchema = Schemas.array(employeeSchema);

var employees = List.of(
    Map.of(
        "name", "Alice",
        "address", Map.of(
            "street", "123 Main St",
            "city", "Springfield",
            "zipCode", "12345"
        )
    )
);

assert employeesSchema.validate(employees).isValid();
```

### Enums

```java
import me.bechberger.util.femtoschema.Schemas;

var status = Schemas.enumOf("ACTIVE", "INACTIVE", "SUSPENDED");
assert status.validate("ACTIVE").isValid();
assert !status.validate("UNKNOWN").isValid();
```

### Sum types (discriminated unions)

```java
import me.bechberger.util.femtoschema.Schemas;

import java.util.Map;

var notificationSchema = Schemas.sumType("type")
    .variant("email", Schemas.object()
        .required("type", Schemas.enumOf("email"))
        .required("address", Schemas.string()))
    .variant("sms", Schemas.object()
        .required("type", Schemas.enumOf("sms"))
        .required("phoneNumber", Schemas.string()));

assert notificationSchema.validate(Map.of(
    "type", "email",
    "address", "user@example.com"
)).isValid();
```

## Validation

### Path-aware error reporting

Errors include the JSON path to the invalid value:

```java
import me.bechberger.util.femtoschema.Schemas;

import java.util.Map;

var schema = Schemas.object()
    .required("user", Schemas.object()
        .required("profile", Schemas.object()
            .required("age", Schemas.number().withMinimum(0))));

var data = Map.of(
    "user", Map.of(
        "profile", Map.of(
            "age", -5.0
        )
    )
);

var result = schema.validate(data);
result.getErrors().forEach(e ->
    System.out.println(e.path() + ": " + e.message())
);
// e.g. $.user.profile.age: Number less than minimum (0.0)
```

### Validating with a custom root path

```java
import me.bechberger.util.femtoschema.Schemas;

var schema = Schemas.string();
var result = schema.validate("hello", "$.config.name");
assert result.isValid();
```

## JSON Schema export

Export your schema into a JSON-Schema-shaped `Map`:

```java
import me.bechberger.util.femtoschema.Schemas;

import java.util.Map;

var schema = Schemas.object()
    .required("name", Schemas.string().withMinLength(1))
    .required("age", Schemas.number().withMinimum(0))
    .optional("email", Schemas.string());

Map<String, Object> jsonSchema = schema.toJsonSchema();
```

Pretty print the exported schema (via `femtojson`):

```java
import me.bechberger.util.femtoschema.Schemas;

System.out.println(Schemas.toJsonSchemaString(schema));
```

## JSON Schema import (read schemas back)

If you already have a JSON Schema object (for example parsed via Jackson as a `Map<String, Object>`), you can convert it back into a `TypeSchema`:

```java
import me.bechberger.util.femtoschema.Schemas;
import me.bechberger.util.femtoschema.TypeSchema;

import java.util.Map;

Map<String, Object> jsonSchema = /* decoded JSON schema object */;
TypeSchema schema = Schemas.fromJsonSchema(jsonSchema);
```

If you have the schema as a JSON string, you can use:

```java
import me.bechberger.util.femtoschema.Schemas;
import me.bechberger.util.femtoschema.TypeSchema;

String json = "{\"type\":\"string\",\"minLength\":1}";
TypeSchema schema = Schemas.fromJsonSchemaString(json);
```

Notes:

- The importer supports the same (small) subset of JSON Schema keywords that `toJsonSchema()` produces.
- The keyword `$comment` is ignored at any nesting level.

## Common constraints

### Strings

```java
import me.bechberger.util.femtoschema.Schemas;

var username = Schemas.string()
    .withMinLength(3)
    .withMaxLength(20)
    .withPattern("^[A-Za-z0-9_]+$");
```

### Numbers

`NumberSchema` supports inclusive and exclusive bounds. Note that *exclusive* bounds are values (Draft 2020-12 style), not boolean flags:

```java
import me.bechberger.util.femtoschema.Schemas;

var percentage = Schemas.number()
    .withExclusiveMinimum(0)
    .withExclusiveMaximum(100);
```

## Supported JSON Schema features

- Basic types: string, number, boolean, object, array
- String constraints: `minLength`, `maxLength`, `pattern`
- Number constraints: `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`
- Objects: required/optional properties
- Arrays: item schemas
- Enumerations
- Discriminated unions (sum types)
- Path-aware validation errors
- JSON Schema export

## Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports etc.
via [GitHub](https://github.com/parttimenerd/femtojson/issues) issues.
Contribution and feedback are encouraged and always welcome.

## License

MIT, Copyright 2026 Johannes Bechberger and contributors
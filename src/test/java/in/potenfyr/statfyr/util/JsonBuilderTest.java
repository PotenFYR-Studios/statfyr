package in.potenfyr.statfyr.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the dependency-free JSON serializer.
 */
class JsonBuilderTest {

    @Test
    void serializesPrimitives() {

        String json =
                new JsonBuilder()
                        .add("name", "Steve")
                        .add("kills", 42)
                        .add("online", true)
                        .add("missing", (Object) null)
                        .build();

        assertEquals(
                "{\"name\":\"Steve\",\"kills\":42,\"online\":true,\"missing\":null}",
                json
        );
    }

    @Test
    void escapesStrings() {

        assertEquals(
                "{\"msg\":\"a\\\"b\\nc\"}",
                new JsonBuilder()
                        .add("msg", "a\"b\nc")
                        .build()
        );
    }

    @Test
    void serializesNestedStructures() {

        Map<String, Object> nested =
                new LinkedHashMap<>();

        nested.put("a", 1);
        nested.put("list", Arrays.asList(1, 2, 3));

        String json =
                new JsonBuilder()
                        .add("nested", nested)
                        .build();

        assertEquals(
                "{\"nested\":{\"a\":1,\"list\":[1,2,3]}}",
                json
        );
    }
}

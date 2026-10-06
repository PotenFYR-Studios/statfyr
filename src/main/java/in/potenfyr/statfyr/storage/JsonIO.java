package in.potenfyr.statfyr.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Small Gson wrapper shared by the storage layer.
 *
 * <p>Gson is shipped with every Minecraft server, so no dependency is bundled.
 */
public final class JsonIO {

    private JsonIO() {
    }

    private static final Gson GSON =
            new GsonBuilder()
                    .disableHtmlEscaping()
                    .create();

    public static String toJson(Object value) {

        return GSON.toJson(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {

        return GSON.fromJson(json, type);
    }

    public static Gson gson() {

        return GSON;
    }
}

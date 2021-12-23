package querqy.solr.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class JsonUtil {

    private static final  ObjectMapper MAPPER = new ObjectMapper();

    public static String toJson(final Object obj) {

        StringWriter writer = new StringWriter();
        try {
            MAPPER.writeValue(writer, obj);
            return writer.toString();
        } catch (final IOException e) {
            throw new RuntimeException("Could not serialize object", e);
        }
    }

    public static void writeJson(final Object obj, final OutputStream os) {

        try {
            MAPPER.writeValue(os, obj);
        } catch (final IOException e) {
            throw new RuntimeException("Could not serialize object", e);
        }
    }

    public static <T> T readJson(final String str, final Class<T> clazz) {
        try {
            return MAPPER.readValue(str, clazz);
        } catch (final IOException e) {
            throw new RuntimeException("Could not deserialize object from " + str, e);
        }
    }

    public static <K,V> Map<K,V> readMapFromJson(final String str) {
        try {
            return MAPPER.readValue(str, new TypeReference<HashMap<K,V>>() {});
        } catch (final IOException e) {
            throw new RuntimeException("Could not deserialize Map from " + str, e);
        }
    }

    public static <T> T readJson(final InputStream is, final Class<T> clazz) {
        try {
            return MAPPER.readValue(is, clazz);
        } catch (final IOException e) {
            throw new RuntimeException("Could not deserialize object", e);
        }
    }
}


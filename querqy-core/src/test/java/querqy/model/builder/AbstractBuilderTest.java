package querqy.model.builder;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractBuilderTest {

    public Map<String, Object> map(final Entry... entries) {
        return Arrays.stream(entries).collect(Collectors.toMap(entry -> entry.key, entry -> entry.value));
    }

    public Entry entry(final String key, final Object value) {
        return new Entry(key, value);
    }

    @Data
    public static class Entry {
        final String key;
        final Object value;
    }

    @SafeVarargs
    public final <T> List<T> list(final T... elements) {
        return Arrays.asList(elements);
    }
}

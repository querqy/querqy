package querqy.solr.utils;

import org.apache.solr.common.util.NamedList;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface ConfigUtils {


    static String getStringArg(final Map<String, Object> config, final String name, final String defaultValue) {
        final String value = (String) config.get(name);
        return value == null ? defaultValue : value;
    }

    static Optional<Integer> getIntArg(final Map<String, Object> config, final String name) {
        final Object object = config.get(name);
        if (object == null) {
            return Optional.empty();
        } else if (object instanceof Integer) {
            return Optional.of((Integer) object);
        } else {
            return Optional.of(Integer.parseInt(object.toString()));
        }
    }

    static Optional<String> getStringArg(final Map<String, Object> config, final String name) {
        return Optional.ofNullable((String) config.get(name));
    }

    static boolean getBoolArg(final Map<String, Object> config, final String name, final boolean defaultValue) {
        return getBoolArg(config, name).orElse(defaultValue);
    }

    static Optional<Boolean> getBoolArg(final Map<String, Object> config, final String name) {
        final Object object = config.get(name);
        if (object == null) {
            return Optional.empty();
        } else if (object instanceof Boolean) {
            return Optional.of((Boolean) object);
        } else {
            return Optional.of(Boolean.parseBoolean(object.toString()));
        }
    }

    static <T extends Enum<T>> Optional<T> getEnumArg(final Map<String, Object> config, final String name,
                                                  final Class<T> enumClass) {
        final String value = (String) config.get(name);
        return (value == null) ? Optional.empty() : Optional.of(Enum.valueOf(enumClass, value));
    }


    static <T> T getArg(final Map<String, Object> config, final String name, final T defaultValue) {
        return (T) config.getOrDefault(name, defaultValue);
    }

    static <V> V getInstanceFromArg(final Map<String, Object> config, final String propertyName, final V defaultValue) {

        final String classField = (String) config.get(propertyName);
        if (classField == null) {
            return defaultValue;
        }

        final String className = classField.trim();
        if (className.isEmpty()) {
            return defaultValue;
        }

        try {
            return (V) Class.forName(className).newInstance();
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    static <T> T newInstance(final String className, final Class<T> expectedType) {
        try {
            Class<? extends T> clazz =  Class.forName(className).asSubclass(expectedType);
            return clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static <T> void ifNotNull(T value, Consumer<T> supplier) {
        if (value != null) {
            supplier.accept(value);
        }
    }

    static <T> T get(final NamedList<?> list, final String key, final T defaultValue) {
        final Object value = list.get(key);
        return value == null ? defaultValue : (T) value;
    }
}

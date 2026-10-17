/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.json;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared reflection-based POJO binder for test-only {@link JsonProvider} implementations that parse JSON
 * into a generic {@code Map}/{@code List} structure of their own and then need to bind that structure into a
 * fixed, caller-supplied target class - mirroring what Jackson's {@code ObjectMapper} does internally,
 * without depending on Jackson.
 * <p>
 * Deliberately uses plain {@code java.lang.reflect} with {@code setAccessible(true)} rather than bytecode
 * generation: several of querqy-core's JSON binding targets (e.g. {@code NumberUnitConfigObject}) are
 * package-private, and a bytecode-generation approach that defines its generated accessor class in a
 * different {@link ClassLoader} instance than the one that loaded the target class - as
 * {@code net.minidev:accessors-smart} does - fails on those with an {@code IllegalAccessError}, since the
 * JVM's package-private access check requires the same defining classloader, not just the same package name.
 * Plain reflection has no such restriction.
 */
final class ReflectiveBeanBinder {

    private ReflectiveBeanBinder() {
    }

    @SuppressWarnings("unchecked")
    static <T> T bind(final Object parsed, final Class<T> type) throws ReflectiveOperationException, IOException {
        if (parsed == null) {
            return null;
        }
        if (type.isInstance(parsed)) {
            return (T) parsed;
        }
        if (!(parsed instanceof Map)) {
            throw new IOException("Cannot bind " + parsed + " into " + type.getName());
        }
        final Map<?, ?> map = (Map<?, ?>) parsed;
        final Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true); // binding targets like NumberUnitConfigObject are package-private
        final T instance = constructor.newInstance();
        for (final Method method : type.getMethods()) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1) {
                continue;
            }
            final String propertyName = decapitalize(method.getName().substring(3));
            if (!map.containsKey(propertyName)) {
                continue;
            }
            final Object coerced = coerce(map.get(propertyName), method.getGenericParameterTypes()[0]);
            method.setAccessible(true);
            method.invoke(instance, coerced);
        }
        return instance;
    }

    private static Object coerce(final Object rawValue, final Type targetType)
            throws ReflectiveOperationException, IOException {
        if (rawValue == null) {
            return null;
        }
        if (targetType instanceof Class) {
            final Class<?> targetClass = (Class<?>) targetType;
            if (targetClass == String.class) {
                return rawValue.toString();
            }
            if ((targetClass == Integer.class || targetClass == int.class) && rawValue instanceof Number) {
                return ((Number) rawValue).intValue();
            }
            if ((targetClass == Float.class || targetClass == float.class) && rawValue instanceof Number) {
                return ((Number) rawValue).floatValue();
            }
            if ((targetClass == Double.class || targetClass == double.class) && rawValue instanceof Number) {
                return ((Number) rawValue).doubleValue();
            }
            if (targetClass.isInstance(rawValue)) {
                return rawValue;
            }
            return bind(rawValue, targetClass); // nested POJO
        }
        if (targetType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) targetType;
            if (parameterizedType.getRawType() == List.class && rawValue instanceof List) {
                final Type elementType = parameterizedType.getActualTypeArguments()[0];
                final List<Object> result = new ArrayList<>();
                for (final Object element : (List<?>) rawValue) {
                    result.add(coerce(element, elementType));
                }
                return result;
            }
        }
        return rawValue;
    }

    private static String decapitalize(final String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}

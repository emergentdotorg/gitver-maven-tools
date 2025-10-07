package org.emergent.gittle.core.gson;

import org.emergent.gittle.core.PropCodec;
import org.emergent.gittle.core.Util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public interface Codable {

    Map<String, String> asMap();

    abstract class AbstractCodable implements Codable {

        //private static final Gson gson = GsonUtil.getGson();
        private static final Map<Type, Map<String, String>> defCache = new ConcurrentHashMap<>();

        protected Map<String, String> asMap(String prefix) {
            return toMap(this, prefix, this.getClass());
        }

        protected static Map<String, String> toMap(Codable src, String prefix, Class<? extends Codable> clazz) {
            return codableToMap(prefix, src, clazz);
            //return PropCodec.codableToMap(src, clazz);
            //return Util.appendPrefix(prefix, map);
        }

        protected static <T extends Codable> T toObj(Map<String, String> src, String prefix, Class<T> clazz) {
            //Map<String, String> fixed = Util.removePrefix(prefix, src);
            //return PropCodec.mapToCodable(src, clazz);
            return mapToCodable(prefix, src, clazz);
        }

        public static <T extends Codable> T mapToCodable(String prefix, Map<String, String> in, Class<? extends T> clazz) {
            Map<String, String> rekeyed = Util.isEmpty(prefix) ? in : Util.removePrefix(prefix, in);
            return PropCodec.fromProperties(rekeyed, clazz);
        }

        public static <T extends Codable> Map<String, String> codableToMap(String prefix, Codable src, Class<? extends T> clazz) {
            Map<String, String> map = PropCodec.toFlatMap(src, clazz);

            getDefaultProperties(clazz).forEach((k, v) -> {
                if (v != null && v.equals(map.get(k))) {
                    map.remove(k);
                }
            });

            Map<String, String> rekeyed = Util.isEmpty(prefix) ? map : Util.appendPrefix(prefix, map);

            return new TreeMap<>(rekeyed);
        }

        private static Map<String, String> getDefaultProperties(Class<?> clazz) {
            return defCache.computeIfAbsent(clazz, c -> {
                try {
                    Optional<Method> initMethod = Arrays.stream(clazz.getMethods())
                            .filter(m -> "newInstance".equals(m.getName()))
                            .filter(m -> m.getParameterCount() == 0)
                            .filter(m -> Modifier.isPublic(m.getModifiers()))
                            .filter(m -> Modifier.isStatic(m.getModifiers()))
                            .findFirst();

                    Object inst = initMethod.isPresent()
                            ? initMethod.get().invoke(null)
                            : clazz.getDeclaredConstructor().newInstance();

                    return PropCodec.toFlatMap(inst, clazz);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }
}

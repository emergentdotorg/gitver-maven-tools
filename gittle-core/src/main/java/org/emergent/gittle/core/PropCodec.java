package org.emergent.gittle.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.java.Log;
import org.emergent.gittle.core.gson.Codable;
import org.emergent.gittle.core.gson.GsonUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.emergent.gittle.core.gson.GsonUtil.OBJ_MAP_TT;
import static org.emergent.gittle.core.gson.GsonUtil.STR_MAP_TT;

@Log
public class PropCodec {

  private static final Map<Type, Map<String, String>> defCache = new ConcurrentHashMap<>();
  private static final Map<Class<?>, String> prefixMap = Map.of(
      Config.class, "gittle.",
      Resolved.class, "gittle.resolved."
  );

  private static final Gson rawGson = GsonUtil.getGson();

  public static Map<String, String> toProperties(Map<String, ?> src) {
    return toFlatMap(src, OBJ_MAP_TT.getType());
  }

  public static <V> V fromProperties(Map<String, String> in, Type type) {
    JsonElement json = toJsonTree(in);
    return fromJsonTree(json, type);
  }

  public static <T extends Codable> Map<String, String> codableToMap(Codable src, Class<? extends T> clazz) {
    Map<String, String> map = toFlatMap(src, clazz);

    getDefaultProperties(clazz).forEach((k, v) -> {
      if (v != null && v.equals(map.get(k))) {
        map.remove(k);
      }
    });

    Map<String, String> rekeyed = getPrefix(clazz).map(p -> Util.appendPrefix(p, map)).orElse(map);

    return new TreeMap<>(rekeyed);
  }

  private static Optional<String> getPrefix(Class<?> clazz) {
    return prefixMap.entrySet().stream()
        .filter(e -> e.getKey().isAssignableFrom(clazz))
        .sorted(Comparator.comparing(e -> calculateDistance(e.getKey(), clazz)))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  /**
   * Calculates the distance (number of inheritance levels) from the
   * implementation class to the base class.
   *
   * @param baseClass The base class to find in the hierarchy.
   * @param implClass The implementation class.
   * @return The distance as an integer, or -1 if the implClass
   * does not inherit from the baseClass.
   */
  public static int calculateDistance(Class<?> baseClass, Class<?> implClass) {
    // Handle invalid inputs.
    if (implClass == null || baseClass == null) {
      throw new IllegalArgumentException("Input classes cannot be null.");
    }

    // Check if the implClass is the baseClass itself.
    if (Objects.equals(implClass, baseClass)) {
      return 0;
    }

    int distance = 0;
    Class<?> currentClass = implClass;

    // Loop up the inheritance tree until a match is found or the top is reached.
    while (currentClass != null && !Objects.equals(currentClass, baseClass)) {
      currentClass = currentClass.getSuperclass();
      distance++;
    }

    // If currentClass is null, the baseClass was not found in the hierarchy.
    if (currentClass == null) {
      return -1;
    }

    return distance;
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

        return toFlatMap(inst, clazz);
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static Map<String, String> toFlatMap(Object src, Type type) {
    JsonElement flattened = flatten(toJsonTree(src, type));
    return rawGson.fromJson(flattened, STR_MAP_TT.getType());
  }

  public static Map<String, Object> toMap(JsonElement json) {
    return fromJsonTree(json, OBJ_MAP_TT.getType());
  }

  public static <V> V fromJsonTree(JsonElement json, Type type) {
    if (hasFlattenedKeys(json)) {
      json = rebuild(json);
    }
    return rawGson.fromJson(json, type);
  }

  public static JsonElement toJsonTree(Map<String, ?> in) {
    return rawGson.toJsonTree(in, OBJ_MAP_TT.getType());
  }

  public static JsonElement toJsonTree(Object in, Type type) {
    return rawGson.toJsonTree(in, type);
  }

  public static boolean hasFlattenedKeys(JsonElement json) {
    if (json.isJsonObject()) {
      JsonObject obj = json.getAsJsonObject();
      return obj.keySet().stream().anyMatch(key -> key.contains("."));
    }
    return false;
  }

  public static boolean hasContainerValues(JsonElement json) {
    if (json.isJsonNull() || json.isJsonPrimitive()) {
      return false;
    }
    Collection<JsonElement> children = json.isJsonArray()
        ? json.getAsJsonArray().asList()
        : json.getAsJsonObject().asMap().values();
    return children.stream().anyMatch(c -> c.isJsonArray() || c.isJsonObject());
  }

  public static JsonElement flatten(JsonElement src) {
    if (src.isJsonArray()) return flattenValues(src.getAsJsonArray());
    if (src.isJsonObject()) return flattenValues(src.getAsJsonObject());
    return src;
  }

  public static JsonObject flattenValues(JsonArray src) {
    return flattenValues(new JsonObject(), "", src);
  }

  public static JsonObject flattenValues(JsonObject src) {
    if (!hasContainerValues(src)) return src;
    return flattenValues(new JsonObject(), "", src);
  }

  private static JsonObject flattenValues(JsonObject dst, String key, JsonElement in) {
    if (in.isJsonPrimitive()) {
      dst.add(key, in);
    }
    String subkeyPrefix = key.isEmpty() ? "" : key + ".";
    if (in.isJsonObject()) {
      in.getAsJsonObject().asMap().forEach((k, v) -> flattenValues(dst, subkeyPrefix + k, v));
    }
    if (in.isJsonArray()) {
      AtomicInteger ii = new AtomicInteger();
      in.getAsJsonArray().forEach(v -> flattenValues(dst, subkeyPrefix + ii.incrementAndGet(), v));
    }
    return dst;
  }

  public static JsonElement rebuild(JsonElement in) {
    return rebuild(in, true);
  }

  public static JsonElement rebuild(JsonElement in, boolean rebuildArrays) {
    if (hasFlattenedKeys(in)) {
      return rebuildValues(new JsonObject(), rebuildArrays, in.getAsJsonObject());
    }
    if (rebuildArrays && in.isJsonObject()) {
      JsonObject src = in.getAsJsonObject();
      Set<String> arrayKeys = getArrayRebuildKeys(src);
      if (!arrayKeys.isEmpty()) {
        return rebuildArrayValues(src.deepCopy(), arrayKeys);
      }
    }
    return in;
  }

  private static JsonObject rebuildValues(JsonObject dst, boolean rebuildArrays, JsonObject src) {
    src.keySet().stream().filter(key -> !key.contains(".")).forEach(key -> dst.add(key, src.get(key)));

    Map<String, JsonObject> rebuiltObjs = new LinkedHashMap<>();
    src.keySet().stream().filter(key -> key.contains(".")).forEach(key -> {
      String groupKey = Util.substringBefore(key, ".");
      JsonObject groupObj = rebuiltObjs.computeIfAbsent(groupKey, gk -> new JsonObject());
      String valueKey = Util.substringAfter(key, ".");
      groupObj.add(valueKey, rebuild(src.get(key), rebuildArrays));
    });
    rebuiltObjs.forEach(dst::add);

    if (rebuildArrays) {
      Set<String> arrayKeys = getArrayRebuildKeys(dst);
      rebuildArrayValues(dst, arrayKeys);
    }

    return dst;
  }

  private static Set<String> getArrayRebuildKeys(JsonObject obj) {
    // Everything has been migrated from 'in' to 'dst' so we're using 'dst' as both source
    // and target for transforms after this point.  Note the toList() before forEach is to
    // avoid a ConcurrentModificationException.
    Set<String> arrayCandidateKeys = new LinkedHashSet<>();
    obj.keySet().stream().filter(k -> obj.get(k).isJsonObject()).toList().forEach(groupKey -> {
      JsonObject groupObj = obj.getAsJsonObject(groupKey);
      List<String> orderedKeys = IntStream.range(1, groupObj.size() + 1).mapToObj(String::valueOf).toList();
      if (orderedKeys.stream().allMatch(groupObj::has)) {
        arrayCandidateKeys.add(groupKey);
      }
    });
    return arrayCandidateKeys;
  }

  private static JsonObject rebuildArrayValues(JsonObject obj, Set<String> arrayCandidateKeys) {
    arrayCandidateKeys.forEach(groupKey -> {
      JsonObject groupObj = obj.getAsJsonObject(groupKey);
      JsonArray arr = new JsonArray();
      groupObj.keySet().stream().map(groupObj::get).forEachOrdered(arr::add);
      obj.add(groupKey, arr);
    });
    return obj;
  }
}

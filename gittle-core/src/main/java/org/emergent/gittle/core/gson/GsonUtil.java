package org.emergent.gittle.core.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value
@AllArgsConstructor
@Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GsonUtil {

  private static final Map<Boolean, Gson> INSTANCES = new ConcurrentHashMap<>();

  public static final TypeToken<Map<String, String>> STR_MAP_TT = new TypeToken<>() {
  };
  public static final TypeToken<Map<String, Object>> OBJ_MAP_TT = new TypeToken<>() {
  };

  public static Gson getGson() {
    return getGson(false);
  }

  public static Gson getGson(boolean pretty) {
    return INSTANCES.computeIfAbsent(pretty, p -> {
      GsonBuilder builder = getGsonBuilder();
      if (p) builder.setPrettyPrinting();
      return builder.create();
    });
  }

  public static GsonBuilder getGsonBuilder() {
    return getGsonBuilder(Collections.emptyMap());
  }

  private static GsonBuilder getGsonBuilder(Map<Class<?>, Object> typeAdapters) {
    GsonBuilder builder = new GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
    typeAdapters.forEach(builder::registerTypeAdapter);
    return builder;
  }
}

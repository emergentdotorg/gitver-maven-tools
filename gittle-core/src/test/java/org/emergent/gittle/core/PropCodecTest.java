package org.emergent.gittle.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class PropCodecTest {

  private static final TypeToken<Map<String, Object>> STR_OBJ_MAP_TT = new TypeToken<>() {
  };
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

//    @Test
//    void testPatternStrategyToProperties() {
//        Map<String, String> actual = getPatternStrategy().asMap();
//        Map<String, String> expected = new LinkedHashMap<>();
//        expected.putAll(getPatternStrategyProperties());
//        assertThat(actual).isNotNull().isEqualTo(expected);
//    }
//
//    @Test
//    void toPatternStrategy() {
//        PatternStrategy expected = getPatternStrategy();
//        Map<String, String> expectedProps = expected.asMap();
//
//        PatternStrategy actual = PatternStrategy.from(expectedProps);
//        Map<String, String> actualProps = actual.asMap();
//
//        assertThat(GSON.toJson(actualProps, STR_OBJ_MAP_TT.getType()))
//                .isNotNull()
//                .isEqualTo(GSON.toJson(expectedProps, STR_OBJ_MAP_TT.getType()));
//
//        assertThat(GSON.toJson(actual, PatternStrategy.class))
//                .isNotNull()
//                .isEqualTo(GSON.toJson(expected, PatternStrategy.class));
//    }

  @Test
  void testResolvedDataToProperties() {
    Map<String, String> expected = getResolvedDataProperties();
    Map<String, String> actual = PropCodec.codableToMap(getCalc(), Resolved.class);
    assertThat(actual).isNotNull()
        .as(() -> GSON.toJson(Map.of(
            "actual", actual,
            "expected", expected
        ), STR_OBJ_MAP_TT.getType()))
        .isEqualTo(expected);
  }

  @Test
  void toResolvedData() {
    Resolved expected = getCalc();
    Map<String, String> expectedProps = getResolvedDataProperties();

    Resolved actual = Resolved.from(expectedProps);
    Map<String, String> actualProps = PropCodec.codableToMap(actual, Resolved.class);

//        assertThat(expectedProps.getClass())
//                .isNotNull()
//                .isEqualTo(LinkedHashMap.class);

    assertThat(expectedProps.keySet().stream().toList())
        .isNotNull()
        .isEqualTo(expectedProps.keySet().stream().sorted().toList());

    assertThat(join(new TreeMap<>(actualProps)))
        .isNotNull()
        .isEqualTo(join(new TreeMap<>(expectedProps)));

    assertThat(join(actualProps))
        .isNotNull()
        .isEqualTo(join(expectedProps));

    assertThat(GSON.toJson(actual, Resolved.class))
        .isNotNull()
        .isEqualTo(GSON.toJson(expected, Resolved.class));
  }

  @Test
  void testConfToProperties() {
    Map<String, String> expected = getConfProperties();
    Map<String, String> actual = getConf().asMap();
    assertThat(actual).isNotNull()
        .as(() -> GSON.toJson(Map.of(
            "actual", actual,
            "expected", expected
        ), STR_OBJ_MAP_TT.getType()))
        .isEqualTo(expected);
  }

  @Test
  void toConf() {
    Config expected = getConf();
    Map<String, String> expectedProps = getConfProperties();

    Config actual = Config.from(expectedProps);
    Map<String, String> actualProps = actual.asMap();

    assertThat(GSON.toJson(actualProps))
        .isNotNull()
        .isEqualTo(GSON.toJson(expectedProps));

    assertThat(GSON.toJson(actual, Config.class))
        .isNotNull()
        .isEqualTo(GSON.toJson(expected, Config.class));
  }

  // private static JsonElement toJsonElement(Object var) {
  //     if (var == null) {
  //         return JsonNull.INSTANCE;
  //     } else if (var instanceof Boolean v) {
  //         return new JsonPrimitive(v);
  //     } else if (var instanceof Number v) {
  //         return new JsonPrimitive(v);
  //     } else if (var instanceof String v) {
  //         return new JsonPrimitive(v);
  //     } else if (var instanceof Map<?, ?> v) {
  //         JsonObject object = new JsonObject();
  //         v.entrySet().stream()
  //           .filter(Allowed::isAllowedValue)
  //           .forEach(e -> object.add(String.valueOf(e.getKey()), toJsonElement(e.getValue())));
  //         return object;
  //     } else if (var instanceof Iterable<?> v) {
  //         JsonArray array = new JsonArray();
  //         StreamSupport.stream(v.spliterator(), false)
  //           .filter(Allowed::isAllowedValue)
  //           .forEach(v1 -> array.add(toJsonElement(v1)));
  //         return array;
  //     } else {
  //         throw new IllegalArgumentException("Unsupported type: " + var.getClass());
  //     }
  // }

//    private Map<String, String> getPatternStrategyProperties() {
//        return PropCodec.toProperties(getPatternStrategy());
//    }

  private Map<String, String> getResolvedDataProperties() {
    if (false) {
      return Map.of(
          "gittle.resolved.branch", "release",
          "gittle.resolved.commits", "5",
          "gittle.resolved.dirty", "true",
          "gittle.resolved.hash", "10fedcba",
          "gittle.resolved.tagVersion", "1.2.3"
      );
    } else {
      return flattenMap(Map.of(
          "gittle.resolved.branch", "release",
          "gittle.resolved.commits", 5,
          "gittle.resolved.dirty", true,
          "gittle.resolved.hash", "10fedcba",
          "gittle.resolved.tagVersion", "1.2.3"
      ));
    }
  }

  private Map<String, String> getConfProperties() {
    return flattenMap(Map.of(
        "gittle.newVersion", "0.1.2",
        "gittle.releaseBranchRegex", "^(release|stable)$"
    ));
  }

  private static Resolved getCalc() {
    return Resolved.builder()
        .tagVersion("1.2.3")
        .branch("release")
        .hash("10fedcba")
        .commits(5)
        .dirty(true)
        .build();
  }

  private static Config getConf() {
    return Config.builder()
        .setNewVersion("0.1.2")
        .setReleaseBranchRegex("^(release|stable)$")
        .setTagNameRegex("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
        .setVersionPattern("%t(-%B)(-%C)(-%S)(+%H)(.%D)")
        .build();
  }

  private static Map<String, String> flattenMap(Map<String, ?> src) {
    return PropCodec.toProperties(new TreeMap<String, Object>(src));
//        return src.entrySet().stream()
//                .collect(CollectorsEx.toTreeMap(Map.Entry::getKey, e -> e.getValue().toString()));
//        return new TreeSet<>(src.keySet()).stream()
//                .map(k -> Map.entry(k, String.valueOf(src.get(k))))
//                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
//        if (src.values().stream().noneMatch(v -> (v instanceof Map) || (v instanceof List))) {
//            return src.entrySet().stream()
//                    .collect(CollectorsEx.toLinkedHashMap(
//                            e -> String.valueOf(e.getKey()),
//                            e -> String.valueOf(e.getValue())
//                    ));
//        }
  }

  private static String join(Map<String, String> map) {
    return Util.join(map);
  }
}
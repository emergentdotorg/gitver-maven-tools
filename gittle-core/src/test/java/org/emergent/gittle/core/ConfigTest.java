package org.emergent.gittle.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.emergent.gittle.core.Constants.VERSION_PATTERN_DEF;

public class ConfigTest {

  private static final HashMap<Object, Object> EMPTY = new HashMap<>();

  @Test
  public void getDefaults() {
    Config config = Config.builder().build();
    assertThat(config)
        .extracting("versionPattern", "newVersion")
        .containsExactly(VERSION_PATTERN_DEF, null);
  }

  @Test
  public void setMiscellaneous() {
    Config config = getConf();
    assertThat(config)
        .extracting("versionPattern", "newVersion")
        .containsExactly("%t(-%C)", "0.1.2");
  }

  @Test
  public void testPropertiesRoundTrip() {
    Config config = getConf();
    Map<String, String> props = config.asMap();
    Config reborn = Config.from(props);
    assertThat(reborn).isEqualTo(config);

    Config def = Config.builder().build();
    Map<String, String> map = def.asMap();
    assertThat(map).isEqualTo(EMPTY);
  }

  private static Config getConf() {
    return Config.builder()
        .setReleaseBranchRegex("^(release|stable)$")
        .setTagNameRegex("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
        .setVersionPattern("%t(-%C)")
        .setNewVersion("0.1.2")
        .build();
  }
}

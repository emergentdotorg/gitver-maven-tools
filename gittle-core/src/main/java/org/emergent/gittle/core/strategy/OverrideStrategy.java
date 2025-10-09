package org.emergent.gittle.core.strategy;

import lombok.NonNull;
import org.emergent.gittle.core.Config;

import java.util.Map;

record OverrideStrategy(@NonNull Config config) implements VersionStrategy {

  @Override
  public String version() {
    return config().getNewVersion();
  }

  @Override
  public Map<String, String> asMap() {
    return config().asMap();
  }
}

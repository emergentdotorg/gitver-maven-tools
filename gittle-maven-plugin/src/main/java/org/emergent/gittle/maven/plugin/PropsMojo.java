package org.emergent.gittle.maven.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.emergent.gittle.core.Config;
import org.emergent.gittle.core.Coordinates;
import org.emergent.gittle.core.Util;
import org.emergent.gittle.core.strategy.VersionStrategy;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.emergent.gittle.core.Constants.NEW_VERSION;
import static org.emergent.gittle.core.Constants.RELEASE_BRANCHES;
import static org.emergent.gittle.core.Constants.RELEASE_BRANCH_REGEX_DEF;
import static org.emergent.gittle.core.Constants.TAG_NAME_PATTERN;
import static org.emergent.gittle.core.Constants.TAG_NAME_REGEX_DEF;
import static org.emergent.gittle.core.Constants.VERSION_PATTERN;
import static org.emergent.gittle.core.Constants.VERSION_PATTERN_DEF;

//@Log
@Getter
@Setter
@Mojo(name = PropsMojo.NAME, defaultPhase = LifecyclePhase.INITIALIZE)
public class PropsMojo extends AbstractGittleMojo {

  public static final String NAME = "props";

  @Parameter(name = NEW_VERSION, defaultValue = "", property = NEW_VERSION_PROP)
  private String newVersion;

  @Parameter(name = RELEASE_BRANCHES, defaultValue = RELEASE_BRANCH_REGEX_DEF, property = RELEASE_BRANCHES_PROP)
  private String releaseBranchRegex;

  @Parameter(name = TAG_NAME_PATTERN, defaultValue = TAG_NAME_REGEX_DEF, property = TAG_NAME_PATTERN_PROP)
  private String tagNameRegex;

  @Parameter(name = VERSION_PATTERN, defaultValue = VERSION_PATTERN_DEF, property = VERSION_PATTERN_PROP)
  private String versionPattern;

  @Override
  protected void execute0() {
    VersionStrategy strategy = getVersionStrategy();
    Map<String, String> properties = strategy.asMap();
    getLog().info("Adding properties to project "
        + MessageUtils.buffer()
        .mojo(Coordinates.builder()
            .setGroupId(mavenProject.getGroupId())
            .setArtifactId(mavenProject.getArtifactId())
            .setVersion(mavenProject.getVersion())
            .build())
        .a(Util.join(properties)));
    mavenProject.getProperties().putAll(properties);
  }

  protected VersionStrategy getVersionStrategy() {
    return VersionStrategy.getInstance(getConfig(), mavenProject.getBasedir());
  }

  @Override
  public Config getConfig() {
    Properties loaded = Util.loadProperties(mavenProject.getBasedir().toPath());
    of(newVersion).ifPresent(v -> loaded.setProperty(NEW_VERSION_PROP, v));
    of(releaseBranchRegex).ifPresent(v -> loaded.setProperty(RELEASE_BRANCHES_PROP, v));
    of(tagNameRegex).ifPresent(v -> loaded.setProperty(TAG_NAME_PATTERN_PROP, v));
    of(versionPattern).ifPresent(v -> loaded.setProperty(VERSION_PATTERN_PROP, v));
    //getLog().warn("LOADED:\n" + Util.join(loaded));
    return Config.from(Util.toMap(loaded));
  }

  private Optional<String> of(String value) {
    return Optional.ofNullable(value).filter(Util::isNotEmpty);
  }
}

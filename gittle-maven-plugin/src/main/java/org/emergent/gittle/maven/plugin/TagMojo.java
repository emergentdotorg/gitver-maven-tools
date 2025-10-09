package org.emergent.gittle.maven.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.emergent.gittle.core.GittleException;
import org.emergent.gittle.core.git.GitUtil;
import org.emergent.gittle.core.strategy.VersionStrategy;

@Getter
@Setter
@Mojo(name = "tag")
public class TagMojo extends AbstractGittleMojo {

  @Parameter(name = "messagePattern", defaultValue = "Release version %v", property = "tag.messagePattern")
  private String messagePattern = "Release version %v";

  @Parameter(name = "namePattern", defaultValue = "v%v", property = "tag.namePattern")
  private String namePattern = "v%v";

  @Parameter(name = "force", defaultValue = "false", property = "tag.force")
  private boolean force;

  @Parameter(name = "failWhenExists", defaultValue = "true", property = "tag.failWhenExists")
  private boolean failWhenExists;

  @Override
  protected void execute0() {
    VersionStrategy versionStrategy = getVersionStrategy();
    String tagName = replaceTokens(getNamePattern(), versionStrategy);
    String tagMessage = replaceTokens(getMessagePattern(), versionStrategy);
    getLog().info("Current Version: " + versionStrategy.version());
    getLog().info(String.format("Tag Version '%s' with message '%s'", tagName, tagMessage));
    GitUtil gitutil = getGitUtil();
    if (!force && gitutil.tagExists(tagName)) {
      getLog().error(String.format("Tag already exist: %s", tagName));
      if (failWhenExists) {
        throw new GittleException("Tag already exist: " + tagName);
      } else {
        return;
      }
    }
    String tagId = gitutil.createTag(tagName, tagMessage, force);
    getLog().info(String.format("Created tag: '%s'", tagId));
  }
}

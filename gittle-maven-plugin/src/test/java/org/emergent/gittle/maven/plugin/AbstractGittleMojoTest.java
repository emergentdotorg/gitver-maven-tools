package org.emergent.gittle.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.emergent.gittle.core.Config;
import org.emergent.gittle.core.strategy.StrategyFactory;
import org.emergent.gittle.core.strategy.VersionStrategy;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractGittleMojoTest {

  AbstractGittleMojo testMojo = new AbstractGittleMojo() {

    {
      mavenProject = new MavenProject();
      mavenProject.setFile(new File("my/pom.xml"));
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
    }
  };

  @Test
  public void getVersionConfig() {
    assertThat(testMojo).isNotNull().extracting("skip").isEqualTo(false);
    assertThat(testMojo).isNotNull();
  }

  @Test
  public void getVersionStrategy() {
    VersionStrategy strategy = testMojo.getVersionStrategy();
    assertThat(strategy).isNotNull().isInstanceOf(VersionStrategy.class);
  }

  @Test
  public void replaceVersionToken() {
    Config conf = Config.builder().setNewVersion("1.2.3").build();
    assertThat(testMojo.replaceTokens("v%v", StrategyFactory.getInstance(conf))).isEqualTo("v1.2.3");
  }
}

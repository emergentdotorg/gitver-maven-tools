package org.emergent.gittle.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.emergent.gittle.core.Coordinates;
import org.emergent.gittle.core.Util;

@Mojo(name = "print", defaultPhase = LifecyclePhase.VALIDATE)
public class PrintMojo extends AbstractGittleMojo {

  @Override
  protected void execute0() {
    getLog().info("Printing properties of project "
        + MessageUtils.buffer()
        .mojo(Coordinates.builder()
            .setGroupId(mavenProject.getGroupId())
            .setArtifactId(mavenProject.getArtifactId())
            .setVersion(mavenProject.getVersion())
            .build())
        .a(Util.join(getVersionStrategy().asMap())));
  }
}

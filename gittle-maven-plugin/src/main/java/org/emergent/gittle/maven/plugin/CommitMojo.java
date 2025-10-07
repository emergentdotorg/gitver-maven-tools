package org.emergent.gittle.maven.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.emergent.gittle.core.GittleException;

import java.util.Objects;

@Getter
@Setter
@Mojo(name = "commit")
public class CommitMojo extends AbstractGittleMojo {

    @Parameter(name = "message", property = "gittle.commitMessage", defaultValue = "Empty Commit")
    private String message;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession mavenSession;

    @Override
    protected void execute0() throws Exception {
        if (!Objects.equals(mavenSession.getTopLevelProject(), mavenProject)) {
            getLog().debug("Skipping CommitMojo in child module: " + mavenProject.getArtifactId());
            return;
        }
        try {
            getGitUtil().executeCommit(message);
        } catch (GittleException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}

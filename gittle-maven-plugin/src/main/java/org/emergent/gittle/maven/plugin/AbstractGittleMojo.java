package org.emergent.gittle.maven.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emergent.gittle.core.Config;
import org.emergent.gittle.core.Constants;
import org.emergent.gittle.core.GittleException;
import org.emergent.gittle.core.Util;
import org.emergent.gittle.core.git.GitUtil;
import org.emergent.gittle.core.strategy.VersionStrategy;

import java.util.Properties;

@Getter
@Setter
public abstract class AbstractGittleMojo extends org.apache.maven.plugin.AbstractMojo {

    private static final String DOTTED_PREFIX = Constants.GITTLE + ".";
    protected static final String NEW_VERSION_PROP = DOTTED_PREFIX + Constants.NEW_VERSION;
    protected static final String RELEASE_BRANCHES_PROP = DOTTED_PREFIX + Constants.RELEASE_BRANCHES;
    protected static final String VERSION_PATTERN_PROP = DOTTED_PREFIX + Constants.VERSION_PATTERN;
    protected static final String TAG_NAME_PATTERN_PROP = DOTTED_PREFIX + Constants.TAG_NAME_PATTERN;

    @Parameter(name = "skip", defaultValue = "false", property = "gittle.skip")
    protected boolean skip;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject mavenProject;

    @Parameter(defaultValue = "${pluginManager}", required = true, readonly = true)
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            execute0();
        } catch (MojoExecutionException | MojoFailureException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new GittleException(e.getMessage(), e);
        }
    }

    protected void execute0() throws Exception {
    }

    protected GitUtil getGitUtil() {
        return GitUtil.getInstance(mavenProject.getBasedir());
    }

    public Config getConfig() {
        Properties extensionProps = Util.loadProperties(mavenProject.getBasedir().toPath());
        return Config.from(Util.toMap(extensionProps));
    }

    protected VersionStrategy getVersionStrategy() {
        return VersionStrategy.getInstance(getConfig(), mavenProject.getBasedir());
    }

    protected String replaceTokens(String pattern, VersionStrategy versionStrategy) {
        return pattern.replace("%v", versionStrategy.version());
    }
}

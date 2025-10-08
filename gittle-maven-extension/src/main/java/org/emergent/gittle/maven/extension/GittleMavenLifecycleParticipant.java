package org.emergent.gittle.maven.extension;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.emergent.gittle.core.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.emergent.gittle.core.Util.GITTLE_POM_XML;
import static org.emergent.gittle.maven.extension.ExtensionUtil.$_REVISION;
import static org.emergent.gittle.maven.extension.ExtensionUtil.REVISION;

/**
 * Handles creating the updated pom file, and assigning it to the project model.
 */
@Named("gittle-lifecycle-participant")
@Singleton
public class GittleMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(GittleMavenLifecycleParticipant.class);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        updateProjects(session);
    }

    private void updateProjects(MavenSession session) {
        session.getAllProjects().forEach(this::updateProject);
    }

    private void updateProject(MavenProject project) {
        if (Util.isDisabled()) {
            if (initialized.compareAndSet(false, true)) {
                LOGGER.debug("{} is disabled", getClass().getSimpleName());
            }
            return;
        }
        Model originalModel = project.getModel();
        Path originalPomFile = originalModel.getPomFile().toPath().toAbsolutePath();
        Path gittlePomFile = originalPomFile.resolveSibling(GITTLE_POM_XML);
        try {
            Model gittleModel = ExtensionUtil.readModelFromPom(originalPomFile);
            copyVersions(originalModel, gittleModel);
            // Now write the updated model out to a file so we can point the project to it.
            ExtensionUtil.writeModelToPom(gittleModel, gittlePomFile);
            project.setPomFile(gittlePomFile.toFile());
            LOGGER.debug("Updated project with newly generated gittle pom {}", gittlePomFile);
        } catch (Exception e) {
            LOGGER.error("Failed creating new gittle pom at {}", gittlePomFile, e);
        }
    }


    @SuppressWarnings("UnusedReturnValue")
    public static boolean copyVersions(Model sourceModel, Model targetModel) {
        Optional<Model> source = Optional.ofNullable(sourceModel);

        Optional<String> sourceVersion = source.map(Model::getVersion);
        boolean versionUpdated = sourceVersion.map(newValue -> {
            String original = targetModel.getVersion();
            if ($_REVISION.equals(original)) {
                targetModel.setVersion(newValue);
                return true;
            }
            return false;
        }).orElse(false);

        Optional<String> sourceParentVersion = source.map(Model::getParent).map(Parent::getVersion);
        boolean parentUpdated = sourceParentVersion.map(newValue -> {
            String original = Optional.ofNullable(targetModel.getParent()).map(Parent::getVersion).orElse(null);
            if ($_REVISION.equals(original)) {
                Parent parent = Optional.ofNullable(targetModel.getParent()).orElseGet(() -> {
                    targetModel.setParent(new Parent());
                    return targetModel.getParent();
                });
                parent.setVersion(newValue);
                return true;
            }
            return false;
        }).orElse(false);

        Optional<Object> sourceRevisionProperty = source.map(Model::getProperties).map(p -> p.get(REVISION));
        boolean revisionUpdated = sourceRevisionProperty.map(newValue -> {
            Object original = targetModel.getProperties().put(REVISION, newValue);
            return !newValue.equals(original);
        }).orElse(false);

        return versionUpdated | parentUpdated | revisionUpdated;
    }
}

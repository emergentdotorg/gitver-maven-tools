package org.emergent.gittle.maven.extension;

import org.apache.maven.it.Verifier;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.emergent.gittle.core.Util;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtensionTestIT {

    @TempDir
    public Path temporaryFolder;

    @BeforeEach
    public void housekeeping() {
        // IT tests do not print to standard maven logs
        // It may look like process is stuck until all tests are executed in background.
        System.out.print(".");
    }

    @AfterAll
    public static void cleanHousekeeping() {
        System.out.println(".");
        System.out.println("IT Log files are available in target/it-logs/");
    }

    @Test
    public void extensionBuildInitialVersion() throws Exception {
        File tempProject = setupTestProject();
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "0.0.0-1-SNAPSHOT";
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
            assertThat(resolveGittlePom(tempProject).toFile())
                    .as("Git versioner pom file")
                    .exists();
        }
    }

    @Test
    public void extensionBuildVersionWithCommits() throws Exception {
        File tempProject = setupTestProject();
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addEmptyCommit(git);
            addEmptyCommit(git);
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "0.0.0-3-SNAPSHOT";
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
            assertThat(resolveGittlePom(tempProject).toFile())
                    .as("Git versioner pom file")
                    .exists();
        }
    }

    @Test
    public void extensionValidateVersionProperties() throws Exception {
        File tempProject = setupTestProject();
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addTag(git, "v0.0.1");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "0.0.1";
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
        }
    }

    @Test
    public void extensionBuildPatchVersion() throws Exception {
        File tempProject = setupTestProject();
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addTag(git, "v0.0.1");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "0.0.1";
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
        }
    }

    @Test
    public void extensionBuildMinorVersion() throws Exception {
        File tempProject = setupTestProject();
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addTag(git, "v0.1.0");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "0.1.0";
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
        }
    }

    @Test
    public void extensionBuildMajorVersion() throws Exception {
        File tempProject = setupTestProject();
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addTag(git, "v1.0.0");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "1.0.0";
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
        }
    }

    @Test
    public void extensionBuildTagVersion() throws Exception {
        File tempProject = setupTestProject();
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addTag(git, "v2.3.4");
            addCommit(git, "[patch]");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "2.3.4-1-SNAPSHOT";
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
        }
    }

    @Test
    public void extensionBuildHashVersion() throws Exception {
        File tempProject = setupTestProject("project-with-extension", "3.");
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addCommit(git, "[patch]");
            addTag(git, "v1.3.5");
            String hash = addCommit(git, "new commit");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "1.3.5+" + hash.substring(0, 8);
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
        }
    }

    @Test
    public void extensionWithInitialVersionProperties() throws Exception {
        File tempProject = setupTestProject("project-with-extension", "1.");
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addTag(git, "v1.3.5");
            String hash = addCommit(git, "new commit");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "1.3.5-1";
            verifier.verifyTextInLog("Building gittle-extension-test " + expectedVersion);
            verifier.verifyTextInLog("gittle-extension-test-" + expectedVersion + ".jar");
        }
    }

    @Test
    public void extensionWithModule() throws Exception {
        File tempProject = setupTestProject("multi-module-project");
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addTag(git, "v0.0.1");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "0.0.1";
            verifier.verifyTextInLog("Building multi-module-parent " + expectedVersion);
            verifier.verifyTextInLog("Building cli " + expectedVersion);
            verifier.verifyTextInLog("Building lib " + expectedVersion);
        }
    }

    @Test
    public void extensionWithParentChild() throws Exception {
        File tempProject = setupTestProject("parent-child-project");
        try (Git git = getMain(tempProject)) {
            addEmptyCommit(git);
            addTag(git, "v0.0.1");
            Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
            verifier.displayStreamBuffers();
            verifier.executeGoal("verify");
            copyExecutionLog(tempProject, verifier);
            verifier.verifyErrorFreeLog();
            String expectedVersion = "0.0.1";
            verifier.verifyTextInLog("Building parent-test-pom " + expectedVersion);
            verifier.verifyTextInLog("Building cli " + expectedVersion);
            verifier.verifyTextInLog(
                    "Setting parent org.emergent.its.gittle:parent-test-pom:pom:${revision} version to " + expectedVersion);
            verifier.verifyTextInLog("Building lib " + expectedVersion);
        }
    }

    private static String getTestName() {
        Class<?> clazz = ExtensionTestIT.class;
        for (StackTraceElement trace : new Exception().getStackTrace()) {
            if (clazz.getName().equals(trace.getClassName())) {
                Optional<Method> match = Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> m.getName().equals(trace.getMethodName()))
                        .filter(m ->
                                m.getAnnotation(Test.class) != null || m.getAnnotation(ParameterizedTest.class) != null)
                        .findFirst();
                if (match.isPresent()) {
                    return match.get().getName();
                }
            }
        }
        return "unknownTest" + new Random().nextInt(10000, 99999);
    }

    private static void copyExecutionLog(File tempProject, Verifier verifier) throws IOException {
        copyExecutionLog(tempProject, verifier, "");
    }

    private static void copyExecutionLog(File tempProject, Verifier verifier, String logKey) throws IOException {
        Path source = tempProject.toPath();
        String testName = getTestName() + logKey;
        Path destdir = Files.createDirectories(Paths.get("./target/it-logs/"));
        Path target = destdir.resolve(testName);
        copyTree(source, target);
    }

    private static void addEmptyCommit(Git git) throws GitAPIException {
        git.commit()
                .setSign(false)
                .setMessage("Empty commit")
                .setAllowEmpty(true)
                .call();
    }

    private static String addCommit(Git git, String message) throws GitAPIException {
        RevCommit commit = git.commit()
                .setSign(false)
                .setMessage(message)
                .setAllowEmpty(true)
                .call();
        return commit.toObjectId().getName();
    }

    private static String addTag(Git git, String message) throws GitAPIException {
        Ref commit = git.tag().setName(message).call();
        return commit.getLeaf().getName();
    }

    private static Git getMain(File tempProject) throws GitAPIException, IOException {
        Path projPath = tempProject.toPath();
        Git main = Git.init()
                .setInitialBranch("main")
                .setDirectory(tempProject)
                .setGitDir(projPath.resolve(".git").toFile())
                .call();
        StoredConfig config = main.getRepository().getConfig();
        config.setString("user", null, "name", "GitHub Actions Test");
        config.setString("user", null, "email", "");
        config.save();
        return main;
    }

    private File setupTestProject() throws IOException {
        return setupTestProject("project-with-extension", "");
    }

    private File setupTestProject(String projname) throws IOException {
        return setupTestProject(projname, "");
    }

    private File setupTestProject(String projname, String propPrefix) throws IOException {
        File tempProject = temporaryFolder.toFile();
        //tempProject.mkdirs();

        Path testProject = Paths.get(getResourcesPrefix() + projname);
        copyTree(testProject, tempProject.toPath());

        //Files.copy(
        //        testProject.resolve("pom.xml"),
        //        tempProject.toPath().resolve("pom.xml"),
        //        StandardCopyOption.REPLACE_EXISTING);
        Path mvnDir = tempProject.toPath().resolve(Util.DOT_MVN);
        //Files.createDirectory(mvnDir);
        //Files.copy(
        //        testProject.resolve(Util.DOT_MVN).resolve("extensions.xml"),
        //        mvnDir.resolve("extensions.xml"),
        //        StandardCopyOption.REPLACE_EXISTING);
        if (Util.isNotEmpty(propPrefix)) {
            Files.copy(
                    testProject.resolve(Util.DOT_MVN).resolve(propPrefix.concat(Util.GITTLE_EXTENSION_PROPERTIES)),
                    mvnDir.resolve(Util.GITTLE_EXTENSION_PROPERTIES),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        assertThat(mvnDir.toFile().list()).contains(Util.GITTLE_EXTENSION_PROPERTIES);
        assertThat(tempProject.list()).contains("pom.xml");
        assertThat(mvnDir.toFile().list()).contains("extensions.xml");
        return tempProject;
    }

    private static void copyTree(Path source, Path target) throws IOException {
        //org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(source.toAbsolutePath().toFile(), target.toAbsolutePath().toFile());
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEachOrdered(path -> {
                try {
                    Path rel = source.relativize(path);
                    Path dst = target.resolve(rel);
                    Files.copy(path, dst, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private String getResourcesPrefix() {
        //    return "src/test/resources";
        return "target/test-classes/";
    }

    private static Path resolveGittlePom(File basedir) {
        return resolveGittlePom(basedir.toPath());
    }

    private static Path resolveGittlePom(Path basedir) {
        return basedir.resolve(Util.GITTLE_POM_XML);
    }
}

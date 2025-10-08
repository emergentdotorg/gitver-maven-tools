package org.emergent.gittle.maven.plugin;

import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.emergent.gittle.core.Coordinates;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PropsMojoTest extends AbstractMojoTest {

    public static class TestLog extends SilentLog {
        List<String> messages = new ArrayList<>();

        @Override
        public void warn(String message) {
            super.warn(message);
            messages.add(message);
        }

        @Override
        public void warn(CharSequence content) {
            super.warn(content);
            messages.add(content.toString());
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void info(CharSequence content) {
            super.info(content);
            messages.add(content.toString());
        }

        @Override
        public void info(String message) {
            super.info(message);
            messages.add(message);
        }

        public List<String> getMessages() {
            return messages;
        }
    }

    @Test
    public void testPrint() throws Exception {
        File pom = new File("target/test-classes/project-to-test/");
        assertThat(pom).as("POM file").isNotNull().exists();

        PropsMojo propsMojo = (PropsMojo) rule.lookupConfiguredMojo(pom, PropsMojo.NAME);
        TestLog testLog = new TestLog();
        propsMojo.setLog(testLog);
        assertThat(propsMojo).isNotNull();
        propsMojo.execute();
        //String gav = "";
        //String gittleVersion = "";
        MavenProject proj = propsMojo.getMavenProject();
        String gittleVersion = proj.getProperties().getProperty("gittle.resolved.version");
        Coordinates gav = Coordinates.builder()
          .setGroupId(proj.getGroupId())
          .setArtifactId(proj.getArtifactId())
          .setVersion(proj.getVersion())
          .build();
        //testLog.warn(buffer()
        //        .a("--- ")
        //                .mojo(gav)
        //                .a(" ")
        //                .strong("[core-extension]")
        //                .a(" ---")
        //        .a(Util.join(proj.getProperties()))
        //        .a("--- ")
        //                .strong("properties" )
        //        .a(" ---")
        //        .build());
        assertThat(testLog.getMessages()).isNotEmpty()
                .allMatch(s -> s.startsWith("Adding properties to project " + gav))
                .allMatch(s -> s.contains("gittle.resolved.version=" + gittleVersion + "\n"));
    }
}

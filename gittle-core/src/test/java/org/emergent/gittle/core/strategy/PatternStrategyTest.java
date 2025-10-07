package org.emergent.gittle.core.strategy;

import org.emergent.gittle.core.Resolved;
import org.emergent.gittle.core.Config;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class PatternStrategyTest {

    private static final HashMap<Object, Object> EMPTY = new HashMap<>();

    @Test
    public void testReleaseSansCommits() {
        PatternStrategy strategy = getPatternStrategy();
        assertThat(strategy.version()).isNotNull()
                .isEqualTo("1.2.3");
    }

    @Test
    public void testDevelSansCommits() {
        PatternStrategy strategy = getPatternStrategy();
        Config config = strategy.config();
        Resolved resolved = strategy.resolved();
        strategy = new PatternStrategy(
                config.toBuilder()
                        .releaseBranchRegex("main")
                        .build(),
                resolved.toBuilder()
                        .branch("development")
                        .build()
        );
        assertThat(strategy.version()).isNotNull()
                .isEqualTo("1.2.3-development+c9f54782");
    }

    @Test
    public void testReleaseWithCommits() {
        PatternStrategy strategy = getPatternStrategy();
        Config config = strategy.config();
        Resolved resolved = strategy.resolved();
        strategy = new PatternStrategy(
                config,
                resolved.toBuilder()
                        .commits(1)
                        .build());
        assertThat(strategy.version()).isNotNull()
                .isEqualTo("1.2.3-1-SNAPSHOT+c9f54782");
    }

    @Test
    public void testDevelopmentWithCommits() {
        PatternStrategy strategy = getPatternStrategy();
        Config config = strategy.config();
        Resolved resolved = strategy.resolved();
        strategy = new PatternStrategy(
                config,
                resolved.toBuilder()
                        .branch("development")
                        .commits(1)
                        .build());
        assertThat(strategy.version()).isNotNull()
                .isEqualTo("1.2.3-development-1-SNAPSHOT+c9f54782");
    }

    @Test
    public void testDirty() {
        PatternStrategy strategy = getPatternStrategy();
        Config config = strategy.config();
        Resolved resolved = strategy.resolved();
        strategy = new PatternStrategy(
                config,
                resolved.toBuilder()
                        .dirty(true)
                        .build());
        assertThat(strategy.version()).isNotNull()
                .isEqualTo("1.2.3.dirty");
    }

    @Test
    public void testPatternSansHash() {
        PatternStrategy strategy = getPatternStrategy();
        Config config = strategy.config();
        Resolved resolved = strategy.resolved();
        strategy = new PatternStrategy(
                config.toBuilder()
                        .versionPattern("%t(-%B)(-%C)(-%S)(.%D)")
                        .build(),
                resolved);
        assertThat(strategy.version()).isNotNull()
                .isEqualTo("1.2.3");
    }

    @Test
    public void testPropertiesNames() {
        PatternStrategy strategy = getPatternStrategy();
        Map<String, String> props = strategy.asMap();
        String collect = props.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n\t", "\n\t", "\n"));
        System.out.printf("props:%s%n", collect);
        PatternStrategy reborn = toPatternStrategy(props);
        assertThat(reborn).isEqualTo(strategy);
        PatternStrategy def = new PatternStrategy();
        Map<String, String> map = def.asMap();
        assertThat(map).isEqualTo(EMPTY);
    }

    @Test
    public void testPropertiesRoundTrip() {
        PatternStrategy strategy = getPatternStrategy();
        Map<String, String> props = strategy.asMap();
        PatternStrategy reborn = toPatternStrategy(props);
        assertThat(reborn).isEqualTo(strategy);
        PatternStrategy def = new PatternStrategy();
        Map<String, String> map = def.asMap();
        assertThat(map).isEqualTo(EMPTY);
    }

    @Test
    public void testXmlOutput() {
        // PatternStrategy strategy = getStrategy();
        // String xml = PropCodec.toXml(strategy);
        // assertThat(xml).asString().isEqualTo("");
    }

    private static PatternStrategy getPatternStrategy() {
        Config config = getConf();
        Resolved resolved = getCalc();
        return new PatternStrategy(config, resolved);
    }

    private static Resolved getCalc() {
        return Resolved.builder()
                .setTagVersion("1.2.3")
                .setBranch("release")
                .setHash("c9f54782")
                .setCommits(0)
                .setDirty(false)
                .build();
    }

    private static Config getConf() {
        return Config.builder()
                .setNewVersion("0.1.2")
                .setReleaseBranchRegex("^(release|stable)$")
                .setTagNameRegex("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
                .setVersionPattern("%t(-%B)(-%C)(-%S)(+%H)(.%D)")
                .build();
    }

    private static PatternStrategy toPatternStrategy(Map<String, String> src) {
        Config config = Config.from(src);
        Resolved resolved = Resolved.from(src);
        //Builder builder = builder()
        //        .conf(conf)
        //        .calc(calc);
        //Optional.ofNullable(src.get(STANDARD_PREFIX + VERSION_STRING)).ifPresent(builder::version);
        return new PatternStrategy(config, resolved);

    }
}

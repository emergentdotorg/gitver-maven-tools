package org.emergent.gittle.core.strategy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.NonFinal;
import org.emergent.gittle.core.Resolved;
import org.emergent.gittle.core.Config;
import org.emergent.gittle.core.Constants;
import org.emergent.gittle.core.Util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.regex.Pattern.quote;

@Value
@EqualsAndHashCode
@lombok.experimental.Accessors(fluent = true)
class PatternStrategy implements VersionStrategy {

    static final String STANDARD_PREFIX = "gittle.resolved.";
    static final String VERSION_STRING = "version";
    private static final String RESOLVED_PREFIX = "resolved.";
    private static final String VERSION_PATTERN_DEF = Constants.VERSION_PATTERN_DEF;
    private static final String VERSION_DEF = Resolved.TAG_VERSION_DEF;

    @NonNull
    Config config;
    @NonNull
    Resolved resolved;

    @NonFinal
    @EqualsAndHashCode.Exclude
    transient String version;

    public PatternStrategy() {
        this(Config.builder().build(), Resolved.builder().build());
    }

    public PatternStrategy(Config config, Resolved resolved) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.resolved = Objects.requireNonNull(resolved, "resolved cannot be null");
    }

    @Override
    public String version() {
        if (Util.isEmpty(version)) {
            version = TokenVersionUtil.calculateVersion(config, resolved);
        }
        return version;
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> map = new TreeMap<>();
        map.putAll(config.asMap());
        map.putAll(resolved.asMap());
        Optional.ofNullable(version())
                .filter(v -> !VERSION_DEF.equals(v))
                .ifPresent(v -> map.put(STANDARD_PREFIX + VERSION_STRING, v));
        return map;
    }

    @Getter
    @Accessors(fluent = true)
    public enum PatternToken {
        TAG("t"),
        COMMIT("c"),
        COMMIT_OPT("C"),
        BRANCH("b"),
        BRANCH_OPT("B"),
        HASH_SHORT("h"),
        HASH_SHORT_OPT("H"),
        HASH_FULL("f"),
        HASH_FULL_OPT("F"),
        SNAPSHOT("S"),
        DIRTY("D");

        private final String code;

        PatternToken(String code) {
            this.code = code;
        }

        public String token() {
            return "%" + code;
        }

        @Override
        public String toString() {
            return token();
        }
    }

    private static class TokenVersionUtil {

        public static String calculateVersion(Config config, Resolved resolved) {
            String pattern = config.getVersionPattern();
            Map<String, String> values = getReplacementMap(config, resolved);
            return performTokenReplacements(pattern, values);
        }

        private static Map<String, String> getReplacementMap(Config config, Resolved resolved) {
            Predicate<String> releaseBranchPredicate = Pattern.compile(config.getReleaseBranchRegex()).asPredicate();
            String branch = Optional.ofNullable(resolved.getBranch()).orElse("");
            int commits = resolved.getCommits();
            boolean isDirty = resolved.isDirty();
            String hash = resolved.getHash();
            String hashShort = Util.toShortHash(hash);
            boolean isReleaseBranch = releaseBranchPredicate.test(branch);
            boolean hasCommits = commits > 0;
            boolean isRelease = isReleaseBranch && !hasCommits;
            return Arrays.stream(PatternToken.values()).collect(Collectors.toMap(
                    PatternToken::token,
                    t -> String.valueOf(
                            switch (t) {
                                case TAG -> resolved.getTagVersion();
                                case BRANCH -> branch;
                                case COMMIT -> commits;
                                case HASH_FULL -> hash;
                                case HASH_SHORT -> hashShort;
                                case SNAPSHOT -> hasCommits ? "SNAPSHOT" : "";
                                case COMMIT_OPT -> hasCommits ? commits : "";
                                case BRANCH_OPT -> isReleaseBranch ? "" : branch;
                                case HASH_FULL_OPT -> isRelease ? "" : hash;
                                case HASH_SHORT_OPT -> isRelease ? "" : hashShort;
                                case DIRTY -> isDirty ? "dirty" : "";
                            }
                    )
            ));
        }

        private static String performTokenReplacements(String versionPattern, Map<String, String> codeReplMap) {
            String codes = Arrays.stream(PatternToken.values()).map(PatternToken::code).collect(Collectors.joining());
            String tokenRegex = quote("%") + "[" + codes + "]";
            Pattern patternx = Pattern.compile(
                    "(?<nakedToken>" + tokenRegex + ")"
                            + "|\\("
                            + "(?<groupPrefix>[^()%]+)?(?<groupToken>" + tokenRegex + ")(?<groupSuffix>[^()%]+)?"
                            + "\\)");

            Matcher m = patternx.matcher(versionPattern);
            AtomicInteger priorEnd = new AtomicInteger(-1);

            String result = m.results()
                    .flatMap(r -> {
                        String nakedToken = null;
                        String groupPrefix = null;
                        String groupToken = null;
                        String groupSuffix = null;
                        for (int ii = 1; ii <= r.groupCount(); ii++) {
                            switch (ii) {
                                case 1:
                                    nakedToken = r.group(ii);
                                    break;
                                case 2:
                                    groupPrefix = r.group(ii);
                                    break;
                                case 3:
                                    groupToken = r.group(ii);
                                    break;
                                case 4:
                                    groupSuffix = r.group(ii);
                                    break;
                            }
                        }

                        List<String> res = new LinkedList<>();
                        if (Util.isNotBlank(nakedToken)) {
                            res.add(Optional.ofNullable(codeReplMap.get(nakedToken)).orElse(""));
                        } else if (Util.isNotBlank(groupToken)) {
                            String repl = Optional.ofNullable(codeReplMap.get(groupToken)).orElse("");
                            if (!repl.isEmpty()) {
                                Stream.of(groupPrefix, repl, groupSuffix).filter(Util::isNotEmpty).forEach(res::add);
                            }
                        }

                        int priorMatchEnd = priorEnd.getAndUpdate($ -> r.end());
                        if (priorMatchEnd > -1 && priorMatchEnd < r.start()) {
                            String unmatchedPreviousSegment = versionPattern.substring(priorMatchEnd, r.start());
                            res.add(0, unmatchedPreviousSegment);
                        }
                        return res.stream();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining());

            // append any remaining after the matches ran out
            int lastMatchEndIdx = priorEnd.get();
            result = lastMatchEndIdx < 0 ? result : result.concat(versionPattern.substring(lastMatchEndIdx));
            return result;
        }
    }
}

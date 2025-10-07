package org.emergent.gittle.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Tolerate;
import org.emergent.gittle.core.gson.Codable;

import java.util.Map;

import static org.emergent.gittle.core.Constants.RELEASE_BRANCH_REGEX_DEF;
import static org.emergent.gittle.core.Constants.TAG_NAME_REGEX_DEF;
import static org.emergent.gittle.core.Constants.VERSION_PATTERN_DEF;

@Value
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@lombok.experimental.FieldDefaults(level = AccessLevel.PRIVATE)
@lombok.experimental.Accessors(fluent = false)
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class Config extends Codable.AbstractCodable {

    private static final String PREFIX = "gittle.";

    @lombok.Builder.Default
    String newVersion = null;
    @NonNull
    @lombok.Builder.Default
    String releaseBranchRegex = RELEASE_BRANCH_REGEX_DEF;
    @NonNull
    @lombok.Builder.Default
    String tagNameRegex = TAG_NAME_REGEX_DEF;
    @NonNull
    @lombok.Builder.Default
    String versionPattern = VERSION_PATTERN_DEF;

    public static Config from(Map<String, String> map) {
        return toObj(map, PREFIX, Config.class);
    }

    public Map<String, String> asMap() {
        return asMap(PREFIX);
    }

    public static class Builder {

        @Tolerate
        public Builder newVersion(String newVersion) {
            return setNewVersion(newVersion);
        }

        @Tolerate
        public Builder releaseBranchRegex(String releaseBranchRegex) {
            return setReleaseBranchRegex(releaseBranchRegex);
        }

        @Tolerate
        public Builder tagNameRegex(String tagNameRegex) {
            return setTagNameRegex(tagNameRegex);
        }

        @Tolerate
        public Builder versionPattern(String versionPattern) {
            return setVersionPattern(versionPattern);
        }
    }
}

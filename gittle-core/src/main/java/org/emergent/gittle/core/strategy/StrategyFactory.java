package org.emergent.gittle.core.strategy;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.emergent.gittle.core.Resolved;
import org.emergent.gittle.core.Config;
import org.emergent.gittle.core.Util;
import org.emergent.gittle.core.git.GitExec;
import org.emergent.gittle.core.git.TagProvider;

import java.io.File;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class StrategyFactory {

    public static VersionStrategy getInstance(Config config) {
        return getInstance(config, new File("."));
    }

    public static VersionStrategy getInstance(Config config, File basePath) {
        return Optional.ofNullable(config)
                .filter(c -> Util.isNotEmpty(c.getNewVersion()))
                .<VersionStrategy>map(OverrideStrategy::new)
                .orElseGet(() -> getPatternStrategy(config, basePath));
    }

    private static VersionStrategy getPatternStrategy(Config config, File basePath) {
        return GitExec.execOp(basePath, git -> {
            return getPatternStrategy(config, git);
        });
    }

    private static VersionStrategy getPatternStrategy(Config config, Git git) throws Exception {
        Repository repository = git.getRepository();
        ObjectId headId = requireNonNull(repository.resolve(Constants.HEAD), "headId is null");

        Resolved.Builder builder = Resolved.builder()
                .gitDir(git.getRepository().getDirectory().getAbsolutePath())
                .branch(repository.getBranch())
                .hash(headId.getName());

        TagProvider tagProvider = new TagProvider(config.getTagNameRegex(), git);

        int commits = 0;
        for (RevCommit commit : git.log().add(headId).call()) {
            boolean isMergeCommit = commit.getParentCount() > 1;
            if (!isMergeCommit) {
                Optional<String> tag = tagProvider.getTag(commit).map(ComparableVersion::toString);
                if (tag.isPresent()) {
                    builder.tagVersion(tag.get());
                    break;
                }
            }
            commits++;
        }
        builder.commits(commits);

        Status status = git.status().setIgnoreSubmodules(SubmoduleWalk.IgnoreSubmoduleMode.UNTRACKED).call();
        builder.dirty(!status.getUncommittedChanges().isEmpty());

        return new PatternStrategy(config, builder.build());
    }

}

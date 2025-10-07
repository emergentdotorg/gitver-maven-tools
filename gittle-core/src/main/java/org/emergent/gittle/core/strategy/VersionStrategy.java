package org.emergent.gittle.core.strategy;

import org.emergent.gittle.core.Config;

import java.io.File;
import java.util.Map;

public interface VersionStrategy {

    static VersionStrategy getInstance(Config config, File basePath) {
        return StrategyFactory.getInstance(config, basePath);
    }

    String version();

    Config config();

    Map<String, String> asMap();

}

package org.emergent.gittle.core;

import org.emergent.gittle.core.strategy.VersionStrategy;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;

public class Main {

  private static final String DEFAULT_PATH = "../test-gittle";

  public static void main(String[] args) {
    Package pack = Util.class.getPackage();
    String implVendor = pack.getImplementationVendor();
    String implVersion = pack.getImplementationVersion();
    System.out.printf("Vendor: %s ; Version: %s%n", implVendor, implVersion);
    String path = args.length > 0 ? args[0] : DEFAULT_PATH;
    File basedir = Paths.get(path).toAbsolutePath().toFile();
    Config config = Config.from(Collections.emptyMap());
    VersionStrategy strategy = VersionStrategy.getInstance(config, basedir);
    System.out.printf("version: %s%n", strategy.version());
  }
}

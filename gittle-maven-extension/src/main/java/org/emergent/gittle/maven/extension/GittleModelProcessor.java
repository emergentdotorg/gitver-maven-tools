package org.emergent.gittle.maven.extension;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.building.Source;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Priority;
import org.eclipse.sisu.Typed;
import org.emergent.gittle.core.Config;
import org.emergent.gittle.core.Coordinates;
import org.emergent.gittle.core.Util;
import org.emergent.gittle.core.strategy.StrategyFactory;
import org.emergent.gittle.core.strategy.VersionStrategy;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;
import static org.emergent.gittle.core.Constants.GITTLE_PREFIX;
import static org.emergent.gittle.core.Util.join;
import static org.emergent.gittle.maven.extension.ExtensionUtil.REVISION;

/**
 * Handles calculating version properties from the Git history.
 */
@Slf4j
@Priority(1)
@Named("core-default")
@Singleton
@Typed(ModelProcessor.class)
public class GittleModelProcessor extends DefaultModelProcessor {

  private final Set<Path> relatedPoms = new HashSet<>();
  private final AtomicReference<VersionStrategy> strategyRef = new AtomicReference<>();
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  private final boolean addProperties;
  private final boolean addPlugin;
  private final boolean configurePlugin;

  public GittleModelProcessor() {
    addProperties = true;
    addPlugin = false;
    configurePlugin = false;
  }

  @Override
  public Model read(File input, Map<String, ?> options) throws IOException {
    return processModel(super.read(input, options), options);
  }

  @Override
  public Model read(Reader input, Map<String, ?> options) throws IOException {
    return processModel(super.read(input, options), options);
  }

  @Override
  public Model read(InputStream input, Map<String, ?> options) throws IOException {
    return processModel(super.read(input, options), options);
  }

  public Model processModel(Model projectModel, Map<String, ?> options) {
    if (Util.isDisabled()) {
      if (initialized.compareAndSet(false, true)) {
        log.debug("{} is disabled", getClass().getSimpleName());
      }
      return projectModel;
    }

    Source pomSource = (Source) options.get(ModelProcessor.SOURCE);
    Optional<String> pomLoc = Optional.ofNullable(pomSource).map(Source::getLocation);
    pomLoc.ifPresent(loc -> projectModel.setPomFile(new File(loc)));
    if (pomLoc.filter(loc -> loc.endsWith(".xml")).isEmpty()) {
      // Source poms end with .xml but dependency poms end with .pom
      return projectModel;
    }

    // This model processor is invoked for every POM on the classpath, including the plugins.
    // The first execution is with the project's pom though. We use strategyRef to avoid processing other poms.
    VersionStrategy versionStrategy = strategyRef.updateAndGet(curStrat -> {
      if (curStrat != null) {
        return curStrat;
      }
      relatedPoms.addAll(findRelatedProjects(projectModel));
      return getVersionStrategy(projectModel);
    });

    processRelatedProjects(projectModel, versionStrategy);
    return projectModel;
  }

  private VersionStrategy getVersionStrategy(Model projectModel) {
    if (Util.useExistingRevision()) {
      String revision = relatedPoms.stream()
          .sorted(Comparator.comparing(p -> p.toString().length()))
          .map(ExtensionUtil::readModelFromPom)
          .map(m -> m.getProperties().getProperty(REVISION))
          .filter(Objects::nonNull).findFirst()
          .orElseGet(() -> System.getProperty(REVISION));
      return StrategyFactory.getInstance(revision);
    }
    Config config = loadConfig(projectModel);
    Coordinates extensionGAV = Util.getExtensionCoordinates();
    log.info(buffer().a("--- ")
        .mojo(extensionGAV)
        .a(" ")
        .strong("[core-extension]")
        .a(" ---")
        .build());
    File basedir = projectModel.getProjectDirectory();
    return StrategyFactory.getInstance(config, basedir);
  }

  private Config loadConfig(Model projectModel) {
    Path currentDir = projectModel.getProjectDirectory().toPath().toAbsolutePath();
    Path extConfigFile = Util.getExtensionPropsFile(currentDir);
    Map<String, String> fileProps = Util.toMap(Util.loadPropsFromFile(extConfigFile));
    log.info("Loaded configuration from file {}:{}", extConfigFile, join(fileProps));
    //Map<String, String> normalized = Util.removePrefix(GITTLE_PREFIX, fileProps);
    Config config = Config.from(fileProps);
    if (!fileProps.equals(config.asMap())) {
      log.warn("Round-trip configuration to properties:{}",
          join(Util.appendPrefix(GITTLE_PREFIX, config.asMap())));
    }
    return config;
  }

  private static Set<Path> findRelatedProjects(Model model) {
    Set<Path> relatedPoms = new HashSet<>();
    Path basedir = model.getProjectDirectory().toPath();
    log.debug("Finding related projects for {} {}", model.getArtifactId(), basedir);
    // Add main project absolute path
    relatedPoms.add(model.getPomFile().toPath());
    // Find modules
    List<Path> modulePoms = model.getModules().stream()
        .map(module -> basedir.resolve(module).resolve("pom.xml"))
        .toList();
    log.debug(
        "Modules found:{}",
        modulePoms.stream().map(Path::toString).collect(Collectors.joining("\n", "\n", "")));
    relatedPoms.addAll(modulePoms);
    return relatedPoms;
  }

  private void processRelatedProjects(Model model, VersionStrategy strategy) {
    String versionString = strategy.version();

    Path modelPomPath = Optional.ofNullable(model.getPomFile()).map(File::toPath).orElse(null);
    if (modelPomPath == null || !relatedPoms.contains(modelPomPath)) {
      return;
    }
    log.debug("Processing model for {}", modelPomPath);

    Coordinates projectGav = Coordinates.builder()
        .setGroupId(getGroupId(model).orElse(""))
        .setArtifactId(model.getArtifactId())
        .setVersion(versionString)
        .build();

    log.debug(
        "Project {}:{}, Computed version: {}",
        projectGav.getGroupId(),
        projectGav.getArtifactId(),
        buffer().strong(projectGav.getVersion()));

    ExtensionUtil.replaceRevision(model, versionString);

    if (addProperties) {
      Map<String, String> newProps = strategy.asMap();
      log.debug("Adding properties to project {}", buffer().mojo(projectGav).a(join(newProps)));
      model.getProperties().putAll(newProps);
    }
    if (addPlugin) {
      addBuildPlugin(model, strategy);
    }
  }

  private void addBuildPlugin(Model projectModel, VersionStrategy strategy) {
    Coordinates coordinates = Util.getPluginCoordinates();
    log.debug("Adding build plugin version {}", coordinates);

    Build build = Optional.ofNullable(projectModel.getBuild()).orElseGet(() -> {
      projectModel.setBuild(new Build());
      return projectModel.getBuild();
    });
    if (Optional.ofNullable(build.getPlugins()).isEmpty()) {
      build.setPlugins(new ArrayList<>());
    }
    PluginManagement pluginMgmt = Optional.ofNullable(build.getPluginManagement())
        .orElseGet(() -> {
          build.setPluginManagement(new PluginManagement());
          return build.getPluginManagement();
        });
    if (Optional.ofNullable(pluginMgmt.getPlugins()).isEmpty()) {
      pluginMgmt.setPlugins(new ArrayList<>());
    }

    Plugin plugin = new Plugin();
    plugin.setGroupId(coordinates.getGroupId());
    plugin.setArtifactId(coordinates.getArtifactId());
    plugin.setVersion(coordinates.getVersion());
    String key = plugin.getKey();

    Plugin normPlugin = build.getPluginsAsMap().get(key);
    Plugin mgmtPlugin = pluginMgmt.getPluginsAsMap().get(key);
    Optional<Plugin> found =
        Stream.of(normPlugin, mgmtPlugin).filter(Objects::nonNull).findFirst();

    found.ifPresent(existing -> log.warn(buffer().mojo(existing)
        .warning(" version is different than ")
        .mojo(coordinates)
        .newline()
        .a("This can introduce unexpected behaviors.")
        .build()));

    if (found.isEmpty()) {
      if (configurePlugin) {
        addPluginConfiguration(plugin, strategy.config());
      }
      pluginMgmt.getPlugins().add(0, plugin);
    }
  }

  private static void addPluginConfiguration(Plugin plugin, Config config) {
    Xpp3Dom dom = ExtensionUtil.toXml(config);
    if (dom.getChildCount() != 0 || Util.isNotEmpty(dom.getValue())) {
      plugin.setConfiguration(dom);
    }
  }

  private static Optional<String> getGroupId(Model projectModel) {
    Optional<String> groupId = Optional.ofNullable(projectModel.getGroupId());
    if (groupId.isEmpty()) {
      groupId = Optional.ofNullable(projectModel.getParent()).map(Parent::getGroupId);
    }
    return groupId;
  }
}

package com.datasqrl.service;

import com.datasqrl.cmd.RootCommand;
import com.datasqrl.config.EngineKeys;
import com.datasqrl.config.PipelineFactory;
import com.datasqrl.config.SqrlConfig;
import com.datasqrl.config.SqrlConfigCommons;
import com.datasqrl.engine.EngineFactory;
import com.datasqrl.engine.database.relational.JDBCEngineFactory;
import com.datasqrl.engine.server.GenericJavaServerEngineFactory;
import com.datasqrl.engine.stream.flink.FlinkEngineFactory;
import com.datasqrl.engine.server.VertxEngineFactory;
import com.datasqrl.error.ErrorCollector;
import com.datasqrl.error.ErrorPrefix;
import com.datasqrl.io.formats.JsonLineFormat;
import com.datasqrl.io.impl.jdbc.JdbcDataSystemConnector;
import com.datasqrl.io.impl.kafka.KafkaDataSystemFactory;
import com.datasqrl.packager.Packager;
import com.datasqrl.packager.PackagerConfig;
import com.datasqrl.schema.input.FlexibleTableSchemaFactory;
import com.google.common.base.Preconditions;
import com.datasqrl.kafka.KafkaLogEngineFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.TaskManagerOptions;

@Slf4j
public class PackagerUtil {

  @SneakyThrows
  public static Packager create(Path rootDir, Path[] files, SqrlConfig config,
      ErrorCollector errors) {
    errors = errors.withLocation(ErrorPrefix.CONFIG).resolve("package");
    PackagerConfig packagerConfig = createPackageConfig(files, rootDir, config);
    return packagerConfig.getPackager(errors);
  }

  protected static PackagerConfig createPackageConfig(Path[] files, Path rootDir,
      SqrlConfig config) {
    PackagerConfig.PackagerConfigBuilder pkgBuilder =
        PackagerConfig.builder()
            .rootDir(rootDir)
            .config(config)
            .mainScript(files[0]);
    if (files.length > 1) {
      pkgBuilder.graphQLSchemaFile(files[1]);
    }
    return pkgBuilder.build();
  }

  public static final Path DEFAULT_PACKAGE = Path.of(Packager.PACKAGE_FILE_NAME);

  public static SqrlConfig getOrCreateDefaultConfiguration(RootCommand root, ErrorCollector errors,
      Supplier<SqrlConfig> defaultConfig) {
    List<Path> configFiles = getOrCreateDefaultPackageFiles(root, errors, defaultConfig);
    Preconditions.checkArgument(configFiles.size() >= 1);
    return SqrlConfigCommons.fromFiles(errors, configFiles.get(0),
        configFiles.subList(1, configFiles.size()).stream().toArray(Path[]::new));
  }

  public static List<Path> getOrCreateDefaultPackageFiles(RootCommand root, ErrorCollector errors,
      Supplier<SqrlConfig> defaultConfig) {
    Optional<List<Path>> existingPackageJson = findRootPackageFiles(root);
    return existingPackageJson
        .orElseGet(() -> List.of(writeEngineConfig(root.getRootDir(),
            defaultConfig.get())));
  }

  @SneakyThrows
  protected static Path writeEngineConfig(Path rootDir, SqrlConfig config) {
    Path enginesFile = Files.createTempFile(rootDir, "package-engines", ".json");
    File file = enginesFile.toFile();
    file.deleteOnExit();

    config.toFile(enginesFile, true);
    return enginesFile;
  }

  public static Optional<List<Path>> findRootPackageFiles(RootCommand root) {
    return findPackageFiles(root.getRootDir(), root.getPackageFiles());
  }

  public static Optional<List<Path>> findPackageFiles(Path rootDir, List<Path> packageFiles) {
    if (packageFiles.isEmpty()) {
      Path defaultPkg = rootDir.resolve(DEFAULT_PACKAGE);
      if (Files.isRegularFile(defaultPkg)) {
        return Optional.of(List.of(defaultPkg));
      } else {
        return Optional.empty();
      }
    } else {
      return Optional.of(packageFiles);
    }
  }

  public static SqrlConfig createDockerConfig(Path rootDir, Path targetDir, ErrorCollector errors) {
    SqrlConfig rootConfig = SqrlConfigCommons.create(errors);

//    Optional<String> graphqlSchema = getGraphqlSchema(rootDir);
//    graphqlSchema.ifPresent((schema)->{
//      SqrlConfig script = rootConfig.getSubConfig("script");
//      script.setProperty("graphql", schema);
//    });

    SqrlConfig config = rootConfig.getSubConfig(PipelineFactory.ENGINES_PROPERTY);

    SqrlConfig dbConfig = config.getSubConfig("database");
    dbConfig.setProperty(JDBCEngineFactory.ENGINE_NAME_KEY, JDBCEngineFactory.ENGINE_NAME);
    dbConfig.setProperties(JdbcDataSystemConnector.builder()
        .url("jdbc:postgresql://database:5432/datasqrl")
        .driver("org.postgresql.Driver")
        .dialect("postgres")
        .database("datasqrl")
        .user("postgres")
        .password("postgres")
        .host("database")
        .port(5432)
        .build()
    );
    SqrlConfig flinkConfig = config.getSubConfig(EngineKeys.STREAMS);
    flinkConfig.setProperty(FlinkEngineFactory.ENGINE_NAME_KEY, FlinkEngineFactory.ENGINE_NAME);

    SqrlConfig server = config.getSubConfig(EngineKeys.SERVER);
    server.setProperty(GenericJavaServerEngineFactory.ENGINE_NAME_KEY,
        VertxEngineFactory.ENGINE_NAME);

    SqrlConfig logConfig = config.getSubConfig(EngineKeys.LOG);
    logConfig.setProperty(EngineFactory.ENGINE_NAME_KEY, KafkaLogEngineFactory.ENGINE_NAME);
    logConfig.copy(
        KafkaDataSystemFactory.getKafkaEngineConfig(KafkaLogEngineFactory.ENGINE_NAME, "kafka:9092",
            JsonLineFormat.NAME, FlexibleTableSchemaFactory.SCHEMA_TYPE));

    return rootConfig;
  }

  private static Optional<String> getGraphqlSchema(Path rootDir) {
    try {
      return Files.walk(rootDir)
          .filter(p -> !Files.isDirectory(p) && p.getFileName().toString().endsWith(".graphqls"))
          .map(Path::toString)
          .findFirst();
    } catch (IOException ignore) {
      log.warn("Could not walk root directory");
    }
    return Optional.empty();
  }

  @SneakyThrows
  public static SqrlConfig createEmbeddedConfig(Path rootDir, ErrorCollector errors) {
    SqrlConfig rootConfig = SqrlConfigCommons.create(errors);

//    Optional<String> graphqlSchema = getGraphqlSchema(rootDir);
//    graphqlSchema.ifPresent((schema)->{
//      SqrlConfig script = rootConfig.getSubConfig("script");
//      script.setProperty("graphql", schema);
//    });
//
    SqrlConfig config = rootConfig.getSubConfig(PipelineFactory.ENGINES_PROPERTY);

    SqrlConfig dbConfig = config.getSubConfig("database");
    dbConfig.setProperty(JDBCEngineFactory.ENGINE_NAME_KEY, JDBCEngineFactory.ENGINE_NAME);
    dbConfig.setProperties(JdbcDataSystemConnector.builder()
        .url("jdbc:h2:file:./h2.db")
        .driver("org.h2.Driver")
        .dialect("h2")
        .database("datasqrl")
        .build()
    );

    SqrlConfig flinkConfig = config.getSubConfig(EngineKeys.STREAMS);
    flinkConfig.setProperty(FlinkEngineFactory.ENGINE_NAME_KEY, FlinkEngineFactory.ENGINE_NAME);

    SqrlConfig server = config.getSubConfig(EngineKeys.SERVER);
    server.setProperty(GenericJavaServerEngineFactory.ENGINE_NAME_KEY,
        VertxEngineFactory.ENGINE_NAME);

    return rootConfig;
  }

  public static SqrlConfig createLocalConfig(String bootstrapServers, ErrorCollector errors) {
    SqrlConfig rootConfig = SqrlConfigCommons.create(errors);

    SqrlConfig config = rootConfig.getSubConfig(PipelineFactory.ENGINES_PROPERTY);

    SqrlConfig dbConfig = config.getSubConfig(EngineKeys.DATABASE);
    dbConfig.setProperty(JDBCEngineFactory.ENGINE_NAME_KEY, JDBCEngineFactory.ENGINE_NAME);
    dbConfig.setProperties(JdbcDataSystemConnector.builder()
        .url("jdbc:postgresql://localhost/datasqrl")
        .driver("org.postgresql.Driver")
        .dialect("postgres")
        .database("datasqrl")
            .host("localhost")
            .port(5432)
            .user("postgres")
            .password("postgres")
        .build()
    );

    SqrlConfig flinkConfig = config.getSubConfig(EngineKeys.STREAMS);
    flinkConfig.setProperty(FlinkEngineFactory.ENGINE_NAME_KEY, FlinkEngineFactory.ENGINE_NAME);
    flinkConfig.setProperty(ConfigConstants.LOCAL_START_WEBSERVER, "true");
    flinkConfig.setProperty(TaskManagerOptions.NETWORK_MEMORY_MIN.key(), "256mb");
    flinkConfig.setProperty(TaskManagerOptions.NETWORK_MEMORY_MAX.key(), "256mb");
    flinkConfig.setProperty(TaskManagerOptions.MANAGED_MEMORY_SIZE.key(), "256mb");

    SqrlConfig server = config.getSubConfig(EngineKeys.SERVER);
    server.setProperty(GenericJavaServerEngineFactory.ENGINE_NAME_KEY,
        VertxEngineFactory.ENGINE_NAME);

    SqrlConfig logConfig = config.getSubConfig(EngineKeys.LOG);
    logConfig.setProperty(EngineFactory.ENGINE_NAME_KEY, KafkaLogEngineFactory.ENGINE_NAME);
    logConfig.copy(
        KafkaDataSystemFactory.getKafkaEngineConfig(KafkaLogEngineFactory.ENGINE_NAME, bootstrapServers,
            JsonLineFormat.NAME, FlexibleTableSchemaFactory.SCHEMA_TYPE));

    return rootConfig;
  }
}

package ai.datasqrl.compiler;

import ai.datasqrl.io.sources.DataSystemConnectorConfig;
import lombok.Value;

public class InputModel {

  @Value
  public static class DataSource {
    String name;
    DataSystemConnectorConfig source;
  }
}

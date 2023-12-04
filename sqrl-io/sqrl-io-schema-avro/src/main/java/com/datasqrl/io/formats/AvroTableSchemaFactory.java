package com.datasqrl.io.formats;

import com.datasqrl.canonicalizer.NameCanonicalizer;
import com.datasqrl.canonicalizer.NamePath;
import com.datasqrl.error.ErrorCode;
import com.datasqrl.error.ErrorCollector;
import com.datasqrl.io.tables.TableConfig;
import com.datasqrl.io.tables.TableSchema;
import com.datasqrl.io.tables.TableSchemaFactory;
import com.datasqrl.module.resolver.ResourceResolver;
import com.datasqrl.serializer.Deserializer;
import com.datasqrl.util.BaseFileUtil;
import com.google.auto.service.AutoService;
import java.net.URI;
import java.util.Optional;
import org.apache.avro.Schema;

@AutoService(TableSchemaFactory.class)
public class AvroTableSchemaFactory implements TableSchemaFactory {

  public static final String SCHEMA_EXTENSION = ".avsc";

  public static final String SCHEMA_TYPE = "avro";

  @Override
  public AvroSchemaHolder create(String schemaDefinition, Optional<URI> location, ErrorCollector errors) {
    if (location.isPresent()) errors = errors.withConfig(location.get());
    Schema schema;
    try {
       schema = new Schema.Parser().parse(schemaDefinition);
    } catch (Exception e) {
      throw errors.exception(ErrorCode.SCHEMA_ERROR, "Could not parse schema: %s", e);
    }
    return new AvroSchemaHolder(schema, schemaDefinition, location);
  }


  @Override
  public String getSchemaFilename(TableConfig tableConfig) {
    return tableConfig.getName().getDisplay() + SCHEMA_EXTENSION;
  }

  @Override
  public String getType() {
    return SCHEMA_TYPE;
  }


}

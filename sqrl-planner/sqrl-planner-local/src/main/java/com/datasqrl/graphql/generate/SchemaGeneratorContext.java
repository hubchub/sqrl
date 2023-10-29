/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.graphql.generate;

import com.datasqrl.graphql.inference.SqrlSchemaForInference;
import com.datasqrl.graphql.inference.SqrlSchemaForInference.SQRLTable;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;

@Getter
public class SchemaGeneratorContext {

  private final BiMap<String, SQRLTable> names = HashBiMap.create();
  public SQRLTable sqrlTable;

  public SchemaGeneratorContext setSqrlTable(SQRLTable table) {
    this.sqrlTable = table;
    return this;
  }
}

package com.datasqrl.calcite.convert;

import com.datasqrl.calcite.Dialect;
import com.datasqrl.calcite.SqrlConfigurations;
import com.datasqrl.calcite.convert.RelToSqlNode.SqlNodes;
import com.google.auto.service.AutoService;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;

@AutoService(SqlNodeToString.class)
public class CalciteSqlNodeToString implements SqlNodeToString {

  @Override
  public SqlStrings convert(SqlNodes sqlNode) {
    SqlWriterConfig config2 = SqrlConfigurations.sqlToString.apply(SqlPrettyWriter.config());
    SqlPrettyWriter prettyWriter = new SqlPrettyWriter(config2);
    sqlNode.getSqlNode().unparse(prettyWriter, 0, 0);
    return () -> prettyWriter.toSqlString().getSql();
  }

  @Override
  public Dialect getDialect() {
    return Dialect.CALCITE;
  }
}

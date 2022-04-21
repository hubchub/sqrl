package ai.datasqrl;

import ai.datasqrl.config.BundleOptions;
import ai.datasqrl.config.error.ErrorCollector;
import ai.datasqrl.config.scripts.ScriptBundle;
import ai.datasqrl.config.scripts.SqrlScript;
import ai.datasqrl.parse.SqrlParser;
import ai.datasqrl.parse.tree.Node;
import ai.datasqrl.parse.tree.ScriptNode;
import ai.datasqrl.physical.ExecutionPlan;
import ai.datasqrl.physical.Physicalizer;
import ai.datasqrl.plan.LocalPlanner;
import ai.datasqrl.plan.LogicalPlan;
import ai.datasqrl.plan.Optimizer;
import ai.datasqrl.schema.Schema;
import ai.datasqrl.schema.operations.OperationExecutor;
import ai.datasqrl.schema.operations.SchemaOperation;
import ai.datasqrl.transform.StatementTransformer;
import ai.datasqrl.validate.StatementValidator;
import ai.datasqrl.validate.scopes.StatementScope;
import com.google.common.base.Preconditions;
import org.apache.calcite.sql.SqlNode;

public class BundleProcessor {

  private final BundleOptions options;
  ErrorCollector errorCollector = ErrorCollector.root();

  public BundleProcessor(BundleOptions options) {
    this.options = options;
  }

  public ExecutionPlan processBundle(ScriptBundle bundle) {
    Schema schema = processMain(bundle.getMainScript());

    Optimizer optimizer = new Optimizer(bundle.getQueries(), true);
    LogicalPlan plan = optimizer.findBestPlan(schema);

    Physicalizer physicalizer = new Physicalizer(options.getImportManager(),
        options.getJdbcConfiguration(),
        options.getStreamEngine());
    return physicalizer.plan(plan);
  }

  private Schema processMain(SqrlScript mainScript) {
    SqrlParser parser = SqrlParser.newParser(errorCollector);
    ScriptNode script = parser.parse(mainScript.getContent());

    OperationExecutor schema = new OperationExecutor();
    for (Node node : script.getStatements()) {
      SchemaOperation operation = processStatement(node, schema);
      Preconditions.checkNotNull(operation, "Operation is null for statement: {%s}", node);
      schema.apply(operation);
    }

    return schema.build();
  }

  public SchemaOperation processStatement(Node statement, OperationExecutor schema) {
    StatementValidator validator = new StatementValidator(
        options.getImportManager(),
        schema.peek());
    StatementScope scope = validator.validate(statement);

    StatementTransformer transformer = new StatementTransformer();
    Node node = transformer.transform(statement, scope);

    LocalPlanner planner = new LocalPlanner(schema.peek());
    return planner.plan(statement, node, scope);
  }
}

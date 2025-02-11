package com.datasqrl.engine.stream.flink.sql.rules;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalCorrelate;
import org.apache.calcite.rel.rules.TransformationRule;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.util.ImmutableBeans;

public class ShapeBushyCorrelateJoinRule extends RelRule<ShapeBushyCorrelateJoinRule.Config>
    implements TransformationRule {

  protected ShapeBushyCorrelateJoinRule() {
    super(ShapeBushyCorrelateJoinRule.Config.DEFAULT);
  }

  @Override
  public void onMatch(RelOptRuleCall relOptRuleCall) {
    LogicalCorrelate top = relOptRuleCall.rel(0);
    LogicalCorrelate left = relOptRuleCall.rel(1);
    RelNode right = relOptRuleCall.rel(2);

    RelBuilder builder = relOptRuleCall.builder()
        .transform(config -> config.withSimplify(false).withPruneInputOfAggregate(false)
            .withBloat(-10000).withSimplifyLimit(false).withPushJoinCondition(false)
        );

    RelNode relNode = builder
        .push(left)
        .project(allNodes(builder, builder.peek()),
            builder.peek().getRowType().getFieldNames(), true)
        .push(right)
        .correlate(top.getJoinType(), top.getCorrelationId())
        .build();

    relOptRuleCall.transformTo(relNode);
  }

  private List<RexNode> allNodes(RelBuilder builder, RelNode node) {
    return IntStream.range(0, node.getRowType().getFieldCount())
        .mapToObj(i->builder.getRexBuilder().makeInputRef(node, i))
        .collect(Collectors.toList());
  }


  /** Rule configuration. */
  public interface Config extends RelRule.Config {
    ShapeBushyCorrelateJoinRule.Config DEFAULT = EMPTY
        .withOperandSupplier(b0 ->
            b0.operand(LogicalCorrelate.class).inputs(
                b1 -> b1.operand(LogicalCorrelate.class).anyInputs(),
                b2 -> b2.operand(RelNode.class).anyInputs()))
        .withDescription("ShapeBushyCorrelateJoinRule")
        .as(ShapeBushyCorrelateJoinRule.Config.class);

    @Override default ShapeBushyCorrelateJoinRule toRule() {
      return new ShapeBushyCorrelateJoinRule();
    }

    /** Whether to include outer joins, default false. */
    @ImmutableBeans.Property
    @ImmutableBeans.BooleanDefault(false)
    boolean isIncludeOuter();

    /** Sets {@link #isIncludeOuter()}. */
    ShapeBushyCorrelateJoinRule.Config withIncludeOuter(boolean includeOuter);
  }
}

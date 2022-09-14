package ai.datasqrl.plan.calcite.table;

import ai.datasqrl.plan.calcite.util.CalciteUtil;
import ai.datasqrl.plan.calcite.util.IndexMap;
import ai.datasqrl.plan.calcite.util.SqrlRexUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.tools.RelBuilder;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public abstract class AddedColumn {

    final String nameId;
    //Being inlined means that the column has been added to the base QueryRelationalTable
    final boolean isInlined;

    public abstract RelDataType getDataType();

    public RelDataType appendTo(@NonNull RelDataType base, @NonNull RelDataTypeFactory factory) {
        return CalciteUtil.appendField(base, nameId, getDataType(), factory);
    }

    public static class Simple extends AddedColumn {

        final RexNode expression;

        public Simple(String nameId, RexNode expression, boolean isInlined) {
            super(nameId, isInlined);
            this.expression = expression;
        }

        @Override
        public RelDataType getDataType() {
            return expression.getType();
        }

        public RexNode getExpression(IndexMap indexMap) {
            return SqrlRexUtil.mapIndexes(expression, indexMap);
        }

        public void appendTo(@NonNull RelBuilder relBuilder) {
            RelDataType baseType = relBuilder.peek().getRowType();
            int noBaseFields = baseType.getFieldCount();
            List<String> fieldNames = new ArrayList<>(noBaseFields+1);
            List<RexNode> rexNodes = new ArrayList<>(noBaseFields+1);
            for (int i = 0; i < noBaseFields; i++) {
                fieldNames.add(i,null); //Calcite will infer name
                rexNodes.add(i, RexInputRef.of(i,baseType));
            }
            fieldNames.add(noBaseFields,nameId);
            rexNodes.add(expression);

            relBuilder.project(rexNodes,fieldNames);
        }
    }


    public static class Complex extends AddedColumn {

        //Logical plan that produces the added column value in the last field. All previous fields
        //are primary key columns of the table (in the same order as for the table) to which this column is added.
        final RelNode rightJoin;

        public Complex(String nameId, RelNode rightJoin) {
            super(nameId, false); //For now, we never inline complex columns
            this.rightJoin = rightJoin;
        }

        RelDataTypeField getAddedColumnField() {
            List<RelDataTypeField> fields = rightJoin.getRowType().getFieldList();
            return fields.get(fields.size()-1);
        }

        @Override
        public RelDataType getDataType() {
            return getAddedColumnField().getType();
        }
    }

}

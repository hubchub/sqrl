package ai.datasqrl.plan.calcite.table;

import ai.datasqrl.plan.calcite.util.IndexMap;
import lombok.Value;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.tools.RelBuilder;
import org.apache.commons.collections.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO: Pullup sort orders through the logical plan and into the database (or discard if they no longer apply)
 */
@Value
public class SortOrder implements PullupOperator {

    public static SortOrder EMPTY = new SortOrder(RelCollations.EMPTY);

    final RelCollation collation;

    public boolean isEmpty() {
        return !collation.getFieldCollations().isEmpty();
    }

    public SortOrder remap(IndexMap map) {
        if (isEmpty()) return this;
        RelCollation newCollation = RelCollations.of(collation.getFieldCollations().stream().map(fc -> fc.withFieldIndex(map.map(fc.getFieldIndex()))).collect(Collectors.toList()));
        return new SortOrder(newCollation);
    }

    public RelBuilder addTo(RelBuilder relBuilder) {
        relBuilder.sort(collation);
        return relBuilder;
    }

    public SortOrder ifEmpty(SortOrder secondary) {
        if (isEmpty()) return secondary;
        else return this;
    }

    public SortOrder join(SortOrder right) {
        if (isEmpty()) return right;
        return new SortOrder(RelCollations.of(ListUtils.union(collation.getFieldCollations(),right.collation.getFieldCollations())));
    }

    public static SortOrder of(List<Integer> partition, RelCollation collation) {
        List<RelFieldCollation> collationList = new ArrayList<>();
        collationList.addAll(partition.stream().map(idx -> new RelFieldCollation(idx, RelFieldCollation.Direction.ASCENDING)).collect(Collectors.toList()));
        collationList.addAll(collation.getFieldCollations());
        return new SortOrder(RelCollations.of(collationList));
    }

}

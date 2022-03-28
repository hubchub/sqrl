package ai.dataeng.sqml.type.constraint;

import ai.dataeng.sqml.config.error.ErrorCollector;
import ai.dataeng.sqml.tree.name.Name;
import ai.dataeng.sqml.type.Type;

import java.util.Map;
import java.util.Optional;

import lombok.Getter;

@Getter
public class Unique implements Constraint {

    public static final Name NAME = Name.system("unique");

    public static final Unique UNCONSTRAINED = new Unique();

    private Unique() {} //For Kryo

    @Override
    public boolean satisfies(Object value) {
        return true;
    }

    @Override
    public boolean appliesTo(Type type) {
        return false;
    }

    @Override
    public Name getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> export() {
        return Map.of();
    }

    @Override
    public String toString() {
        return NAME.getDisplay();
    }

    public static class Factory implements Constraint.Factory {

        @Override
        public Name getName() {
            return NAME;
        }

        @Override
        public Optional<Constraint> create(Map<String, Object> parameters, ErrorCollector errors) {
            return Optional.of(new Unique());
        }
    }
}

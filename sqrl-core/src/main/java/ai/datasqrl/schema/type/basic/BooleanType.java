package ai.datasqrl.schema.type.basic;

import ai.datasqrl.schema.type.SqrlTypeVisitor;

import java.util.Optional;
import java.util.function.Function;

public class BooleanType extends AbstractBasicType<Boolean> {

  private static final Function<String, Boolean> parseBoolean = new Function<String, Boolean>() {
    @Override
    public Boolean apply(String s) {
      if (s.equalsIgnoreCase("true")) {
        return true;
      } else if (s.equalsIgnoreCase("false")) {
        return false;
      }
      throw new IllegalArgumentException("Not a boolean");
    }
  };

  public static final BooleanType INSTANCE = new BooleanType();

  @Override
  public String getName() {
    return "BOOLEAN";
  }

  @Override
  public Conversion conversion() {
    return Conversion.INSTANCE;
  }

  private static class Conversion extends SimpleBasicType.Conversion<Boolean> {

    private static final Conversion INSTANCE = new Conversion();

    private Conversion() {
      super(Boolean.class, parseBoolean);
    }

    @Override
    public Boolean convert(Object o) {
      if (o instanceof Boolean) {
        return (Boolean) o;
      }
      if (o instanceof Number) {
        return ((Number) o).longValue() > 0;
      }
      throw new IllegalArgumentException("Invalid type to convert: " + o.getClass());
    }

    @Override
    public Optional<Integer> getTypeDistance(BasicType fromType) {
      if (fromType instanceof IntegerType) {
        return Optional.of(80);
      }
      return Optional.empty();
    }

  }

  public <R, C> R accept(SqrlTypeVisitor<R, C> visitor, C context) {
    return visitor.visitBooleanType(this, context);
  }
}

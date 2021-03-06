package columnar;

import btree.IntegerKey;
import btree.KeyClass;
import heap.FieldNumberOutOfBoundException;
import heap.Tuple;
import java.io.IOException;

public class IntegerValue extends ValueClass {

  Integer val;

  IntegerValue()
  {

  }

  public IntegerValue(Integer val)
  {
    this.val = val;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IntegerValue that = (IntegerValue) o;

    return val != null ? val.equals(that.val) : that.val == null;
  }

  @Override
  public int hashCode() {
    return val != null ? val.hashCode() : 0;
  }

  @Override
  public Object getValue() {
    return val;
  }

  @Override
  public void setValue(Object val) {
    this.val = (Integer) val;
  }

  @Override
  public void setValueFromColumnTuple(Tuple columnTuple, int fieldPos)
      throws IOException, FieldNumberOutOfBoundException {
    int val = columnTuple.getIntFld(fieldPos);
    this.val = val;
  }

  @Override
  void setValueinRowTuple(Tuple rowTuple, int fieldNo)
      throws IOException, FieldNumberOutOfBoundException {
    rowTuple.setIntFld(fieldNo, val.intValue());
  }

  @Override
  KeyClass getKeyClassFromColumnTuple(Tuple columnarTuple, int fieldPos)
      throws IOException, FieldNumberOutOfBoundException {
    int val = columnarTuple.getIntFld(fieldPos);
    return new IntegerKey(val);
  }

  @Override
  void setValueFromRowTuple(Tuple rowTuple, int fieldPos)
      throws IOException, FieldNumberOutOfBoundException {
    val = rowTuple.getIntFld(fieldPos);
  }

  @Override
  public Boolean evaluateEquals(ValueClass otherValue) {
    if (this == otherValue) {
      return true;
    }
    if (otherValue == null || getClass() != otherValue.getClass()) {
      return false;
    }

    IntegerValue that = (IntegerValue) otherValue;

    return val != null ? val.equals(that.val) : false;
  }

  @Override
  public Boolean evaluateGT(ValueClass otherValue) {
    if (this == otherValue) {
      return true;
    }
    if (otherValue == null || getClass() != otherValue.getClass()) {
      return false;
    }

    IntegerValue that = (IntegerValue) otherValue;

    return val != null ? val>that.val : false;
  }

  @Override
  public Boolean evaluateGTEquals(ValueClass otherValue) {

    if (this == otherValue) {
      return true;
    }
    if (otherValue == null || getClass() != otherValue.getClass()) {
      return false;
    }

    IntegerValue that = (IntegerValue) otherValue;

    return val != null ? val>=that.val : false;
  }

  @Override
  public Boolean evaluateLT(ValueClass otherValue) {
    if (this == otherValue) {
      return true;
    }
    if (otherValue == null || getClass() != otherValue.getClass()) {
      return false;
    }

    IntegerValue that = (IntegerValue) otherValue;

    return val != null ? val<that.val : false;
  }

  @Override
  public Boolean evaluateLTEquals(ValueClass otherValue) {
    if (this == otherValue) {
      return true;
    }
    if (otherValue == null || getClass() != otherValue.getClass()) {
      return false;
    }

    IntegerValue that = (IntegerValue) otherValue;

    return val != null ? val<=that.val : false;
  }

  @Override
  public Boolean evaluateNotEquals(ValueClass otherValue) {
    if (this == otherValue) {
      return true;
    }
    if (otherValue == null || getClass() != otherValue.getClass()) {
      return false;
    }

    IntegerValue that = (IntegerValue) otherValue;

    return val != null ? val!=that.val : false;
  }

  @Override
  public String toString() {
    return val.toString();
  }
}

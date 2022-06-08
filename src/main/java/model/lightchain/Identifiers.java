package model.lightchain;

import java.io.Serializable;
import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents an aggregated type for identifiers.
 */
public class Identifiers implements Serializable {
  private final ArrayList<Identifier> identifiers;

  public Identifiers() {
    this.identifiers = new ArrayList<>();
  }

  public void add(Identifier identifier) {
    this.identifiers.add(identifier);
  }

  public boolean has(Identifier identifier) {
    return this.identifiers.contains(identifier);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Identifiers)) {
      return false;
    }
    Identifiers that = (Identifiers) o;
    return this.identifiers.equals(that.identifiers);
  }

  @Override
  public int hashCode() {
    return this.identifiers.hashCode();
  }

  public int size() {
    return this.identifiers.size();
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public ArrayList<Identifier> getAll() {
    return this.identifiers;
  }

  @Override
  public String toString() {
    return "Identifiers{"
            + "identifiers=" + identifiers
            + '}';
  }
}
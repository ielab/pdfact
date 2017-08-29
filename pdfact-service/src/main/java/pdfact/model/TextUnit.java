package pdfact.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The text units that can be extracted with PdfAct.
 *
 * @author Claudius Korzen
 */
public enum TextUnit {
  /**
   * The paragraphs.
   */
  PARAGRAPH("paragraphs"),

  /**
   * The words.
   */
  WORD("words"),

  /**
   * The characters.
   */
  CHARACTER("characters");

  /**
   * The plural name of this unit.
   */
  String pluralName;

  /**
   * Creates a new text unit.
   * 
   * @param pluralName
   *        The plural name of this text unit.
   */
  TextUnit(String pluralName) {
    this.pluralName = pluralName;
  }

  /**
   * Returns the plural name of this text unit.
   * 
   * @return The plural name of this text unit.
   */
  public String getPluralName() {
    return this.pluralName;
  }

  /**
   * The text units per plural names.
   */
  protected static Map<String, TextUnit> index;

  static {
    index = new HashMap<>();

    // Fill the map of types per group name.
    for (TextUnit type : values()) {
      index.put(type.getPluralName(), type);
    }
  }

  /**
   * Returns a set of the plural names of all text units.
   * 
   * @return A set of the plural names of all text units.
   */
  public static Set<String> getPluralNames() {
    return index.keySet();
  }

  /**
   * Checks if the given name is a plural name of an existing text unit.
   * 
   * @param name
   *        The name to check.
   * 
   * @return True, if the given name is a plural name of an existing type.
   */
  public static boolean isValidType(String name) {
    return index.containsKey(name.toLowerCase());
  }

  /**
   * Returns the types that are associated with the given plural names.
   * 
   * @param names
   *        The names of the text units to fetch.
   *
   * @return A set of the fetched text units.
   */
  public static Set<TextUnit> fromStrings(String... names) {
    return fromStrings(Arrays.asList(names));
  }

  /**
   * Returns the text units that are associated with the given names.
   * 
   * @param names
   *        The names of the text units to fetch.
   *
   * @return A set of the fetched text units.
   */
  public static Set<TextUnit> fromStrings(List<String> names) {
    if (names == null || names.isEmpty()) {
      return null;
    }

    Set<TextUnit> types = new HashSet<>();
    for (String name : names) {
      TextUnit type = fromString(name);
      if (type != null) {
        types.add(type);
      }
    }
    return types;
  }

  /**
   * Returns the text unit that is associated with the given name.
   * 
   * @param name
   *        The plural name of the type to fetch.
   *
   * @return The type that is associated with the given plural name.
   */
  public static TextUnit fromString(String name) {
    if (!isValidType(name)) {
      throw new IllegalArgumentException(name + " isn't a valid type.");
    }
    return index.get(name.toLowerCase());
  }
}
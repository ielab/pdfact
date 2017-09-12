package pdfact.core.model;

import java.util.List;

/**
 * An interface that is implemented by PDF elements that have shapes (like
 * lines, curves, paths, etc.).
 *
 * @author Claudius Korzen
 */
public interface HasShapes {
  /**
   * Returns the shapes of this element.
   * 
   * @return The shapes of this element.
   */
  List<Shape> getShapes();

  /**
   * Returns the first shape of this element.
   * 
   * @return The first shape or null if there are no shapes.
   */
  Shape getFirstShape();

  /**
   * Returns the last shape of this element.
   * 
   * @return The last shape or null if there are no shapes.
   */
  Shape getLastShape();

  // ==========================================================================

  /**
   * Sets the shapes of this element.
   * 
   * @param shapes
   *        The shapes of this element.
   */
  void setShapes(List<Shape> shapes);

  /**
   * Adds the given shapes to this element.
   * 
   * @param shapes
   *        The shapes to add.
   */
  void addShapes(List<Shape> shapes);

  /**
   * Adds the given shape to this element.
   * 
   * @param shape
   *        The shape to add.
   */
  void addShape(Shape shape);
}
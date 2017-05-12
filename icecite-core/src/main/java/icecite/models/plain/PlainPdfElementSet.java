package icecite.models.plain;

import java.util.Collection;
import java.util.HashSet;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import icecite.models.PdfElement;
import icecite.models.PdfElementSet;
import icecite.utils.counter.FloatCounter;
import icecite.utils.geometric.Rectangle;
import icecite.utils.geometric.Rectangle.RectangleFactory;

/**
 * An implementation of {@link PdfElementSet} based on a HashSet.
 * 
 * @param <T>
 *        The concrete type of the elements in this set.
 * 
 * @author Claudius Korzen
 */
public class PlainPdfElementSet<T extends PdfElement> extends HashSet<T>
    implements PdfElementSet<T> {
  /**
   * The serial id.
   */
  protected static final long serialVersionUID = -8634251754322444722L;

  /**
   * The counter for the heights of the elements.
   */
  protected FloatCounter heightCounter;

  /**
   * The counter for the widths of the elements.
   */
  protected FloatCounter widthCounter;

  /**
   * The counter for the minX values of the elements.
   */
  protected FloatCounter minXCounter;

  /**
   * The counter for the minY values of the elements.
   */
  protected FloatCounter minYCounter;

  /**
   * The counter for the maxX values of the elements.
   */
  protected FloatCounter maxXCounter;

  /**
   * The counter for the maxY values of the elements.
   */
  protected FloatCounter maxYCounter;

  /**
   * The factory to create instances of {@Rectangle}.
   */
  protected RectangleFactory rectangleFactory;

  /**
   * A flag to indicate whether the bounding box is outdated.
   */
  protected boolean isBoundingBoxOutdated;

  /**
   * The bounding box around the elements in this set.
   */
  protected Rectangle boundingBox;
  
  // ==========================================================================
  // Constructors.

  /**
   * Creates a new set of PDF elements.
   * 
   * @param rectangleFactory
   *        The factory to create instances of {@Rectangle}.
   */
  @AssistedInject
  public PlainPdfElementSet(RectangleFactory rectangleFactory) {
    this.rectangleFactory = rectangleFactory;
    this.heightCounter = new FloatCounter();
    this.widthCounter = new FloatCounter();
    this.minXCounter = new FloatCounter();
    this.minYCounter = new FloatCounter();
    this.maxXCounter = new FloatCounter();
    this.maxYCounter = new FloatCounter();
  }

  /**
   * Creates a new set of PDF elements.
   * 
   * @param rectangleFactory
   *        The factory to create instances of {@Rectangle}.
   * @param elements
   *        The elements of this set.
   */
  @AssistedInject
  public PlainPdfElementSet(RectangleFactory rectangleFactory,
      @Assisted Collection<T> elements) {
    this(rectangleFactory);
    addAll(elements);
  }

  // ==========================================================================
  // Override all methods that changes this set.

  @Override
  public boolean add(T e) {
    boolean added = super.add(e);
    if (added) {
      addToCounters(e);
    }
    return added;
  }

  @Override
  public boolean remove(Object o) {
    boolean removed = super.remove(o);
    if (removed) {
      removeFromCounters(o);
    }
    return removed;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    super.clear();
    clearCounters();
  }

  // ==========================================================================

  /**
   * Adds the given element to all counters of this set.
   * 
   * @param e
   *        The element to add.
   */
  protected void addToCounters(T e) {
    this.heightCounter.add(e.getBoundingBox().getHeight());
    this.widthCounter.add(e.getBoundingBox().getWidth());
    this.minXCounter.add(e.getBoundingBox().getMinX());
    this.minYCounter.add(e.getBoundingBox().getMinY());
    this.maxXCounter.add(e.getBoundingBox().getMaxX());
    this.maxYCounter.add(e.getBoundingBox().getMaxY());
    this.isBoundingBoxOutdated = true;
  }

  /**
   * Removes the given object from all counters of this set.
   * 
   * @param o
   *        The object to remove.
   */
  protected void removeFromCounters(Object o) {
    if (o instanceof PdfElement) {
      PdfElement e = (PdfElement) o;
      this.heightCounter.discount(e.getBoundingBox().getHeight());
      this.widthCounter.discount(e.getBoundingBox().getWidth());
      this.minXCounter.discount(e.getBoundingBox().getMinX());
      this.minYCounter.discount(e.getBoundingBox().getMinY());
      this.maxXCounter.discount(e.getBoundingBox().getMaxX());
      this.maxYCounter.discount(e.getBoundingBox().getMaxY());
      this.isBoundingBoxOutdated = true;
    }
  }

  /**
   * Clears the counters of this set.
   */
  protected void clearCounters() {
    this.heightCounter.clear();
    this.widthCounter.clear();
    this.minXCounter.clear();
    this.minYCounter.clear();
    this.maxXCounter.clear();
    this.maxYCounter.clear();
    this.isBoundingBoxOutdated = true;
  }

  // ==========================================================================
  // Getter methods.

  @Override
  public float getMostCommonHeight() {
    return this.heightCounter.getMostFrequentFloat();
  }

  @Override
  public float getAverageHeight() {
    return this.heightCounter.getAverageValue();
  }

  @Override
  public float getMostCommonWidth() {
    return this.widthCounter.getMostFrequentFloat();
  }

  @Override
  public float getAverageWidth() {
    return this.widthCounter.getAverageValue();
  }

  @Override
  public float getMostCommonMinX() {
    return this.minXCounter.getMostFrequentFloat();
  }

  @Override
  public float getMostCommonMinY() {
    return this.minYCounter.getMostFrequentFloat();
  }

  @Override
  public float getMostCommonMaxX() {
    return this.maxXCounter.getMostFrequentFloat();
  }

  @Override
  public float getMostCommonMaxY() {
    return this.maxYCounter.getMostFrequentFloat();
  }

  @Override
  public Rectangle getBoundingBox() {
    if (this.boundingBox == null || this.isBoundingBoxOutdated) {
      this.boundingBox = this.rectangleFactory.create();
      this.boundingBox.setMinX(this.minXCounter.getSmallestFloat());
      this.boundingBox.setMinY(this.minYCounter.getSmallestFloat());
      this.boundingBox.setMaxX(this.maxXCounter.getLargestFloat());
      this.boundingBox.setMaxY(this.maxYCounter.getLargestFloat());
      this.isBoundingBoxOutdated = false;
    }
    return this.boundingBox;
  }

  @Override
  public void setBoundingBox(Rectangle boundingBox) {
    this.boundingBox = boundingBox;
    this.isBoundingBoxOutdated = false;
  }
}

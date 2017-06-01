package icecite.parse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import icecite.models.PdfCharacter;
import icecite.models.PdfCharacterList.PdfCharacterListFactory;
import icecite.models.PdfDocument;
import icecite.models.PdfDocument.PdfDocumentFactory;
import icecite.parse.filter.PdfCharacterFilter;
import icecite.parse.filter.PdfFigureFilter;
import icecite.parse.filter.PdfShapeFilter;
import icecite.parse.stream.HasPdfStreamParserHandlers;
import icecite.parse.stream.PdfStreamParser;
import icecite.parse.stream.PdfStreamParser.PdfStreamParserFactory;
import icecite.parse.translate.DiacriticsTranslator;
import icecite.parse.translate.LigaturesTranslator;
import icecite.models.PdfFigure;
import icecite.models.PdfPage;
import icecite.models.PdfShape;

/**
 * A plain implementation of {@link PdfParser}.
 *
 * @author Claudius Korzen
 */
public class PlainPdfParser implements PdfParser, HasPdfStreamParserHandlers {
  /**
   * The instance of PdfStreamParser.
   */
  protected PdfStreamParserFactory streamParserFactory;

  /**
   * The factory to create instances of PdfDocument.
   */
  protected PdfDocumentFactory documentFactory;

  /**
   * The factory to create instances of PdfCharacterList.
   */
  protected PdfCharacterListFactory characterListFactory;

  /**
   * The PDF document.
   */
  protected PdfDocument pdfDocument;

  /**
   * The current page.
   */
  protected PdfPage page;

  /**
   * The predecessor of the current character (needed to resolve diacritics).
   */
  protected PdfCharacter prevCharacter;

  /**
   * The predecessor of prevCharacter (needed to resolve diacritics).
   */
  protected PdfCharacter prevPrevCharacter;

  /**
   * The boolean flag to indicate whether ligatures should be resolved or not.
   */
  protected boolean resolveLigatures;

  /**
   * The boolean flag to indicate whether characters with diacritics should be
   * resolved or not.
   */
  protected boolean resolveDiacritics;

  // ==========================================================================
  // Constructors.

  /**
   * Creates a new PDF parser.
   * 
   * @param pdfStreamParserFactory
   *        The PDF stream parser.
   * @param pdfDocFactory
   *        The factory to create instances of PdfDocument.
   * @param characterListFactory
   *        The factory to create instances of PdfCharacterList.
   */
  @AssistedInject
  public PlainPdfParser(PdfStreamParserFactory pdfStreamParserFactory,
      PdfDocumentFactory pdfDocFactory,
      PdfCharacterListFactory characterListFactory) {
    this(pdfStreamParserFactory, pdfDocFactory, characterListFactory, true,
        true);
  }

  /**
   * Creates a new PDF parser.
   * 
   * @param pdfStreamParserFactory
   *        The PDF stream parser.
   * @param pdfDocumentFactory
   *        The factory to create instances of PdfDocument.
   * @param characterListFactory
   *        The factory to create instances of PdfCharacterList.
   * @param resolveLigatures
   *        The boolean flag to indicate whether ligatures should be resolved
   *        or not.
   * @param resolveDiacritics
   *        The boolean flag to indicate whether characters with diacritics
   *        should be resolved or not.
   */
  @AssistedInject
  public PlainPdfParser(PdfStreamParserFactory pdfStreamParserFactory,
      PdfDocumentFactory pdfDocumentFactory,
      PdfCharacterListFactory characterListFactory,
      @Assisted("resolveLigatures") boolean resolveLigatures,
      @Assisted("resolveDiacritics") boolean resolveDiacritics) {
    this.streamParserFactory = pdfStreamParserFactory;
    this.documentFactory = pdfDocumentFactory;
    this.characterListFactory = characterListFactory;
    this.resolveLigatures = resolveLigatures;
    this.resolveDiacritics = resolveDiacritics;
  }

  // ==========================================================================
  // Parse methods.

  @Override
  public PdfDocument parsePdf(Path pdf) throws IOException {
    return parsePdf(pdf != null ? pdf.toFile() : null);
  }

  @Override
  public PdfDocument parsePdf(File pdf) throws IOException {
    PdfStreamParser streamParser = this.streamParserFactory.create(this);

    // Parse the PDF file.
    streamParser.parsePdf(pdf);

    // Return the PdfDocument generated by the handler methods below.
    return this.pdfDocument;
  }

  // ==========================================================================
  // Getter methods.

  /**
   * Returns true, if ligatures should be resolved; false otherwise.
   * 
   * @return true, if ligatures should be resolved; false otherwise.
   */
  public boolean isResolveLigatures() {
    return this.resolveLigatures;
  }

  /**
   * Returns true, if diacritics should be resolved; false otherwise.
   * 
   * @return true, if diacritics should be resolved; false otherwise.
   */
  public boolean isResolveDiacritics() {
    return this.resolveDiacritics;
  }

  // ==========================================================================
  // Handler methods.

  @Override
  public void handlePdfFileStart(File pdf) {
    // Create a new PDF document.
    this.pdfDocument = this.documentFactory.create(pdf);
  }

  @Override
  public void handlePdfFileEnd(File pdf) {
    // Nothing to do so far.
  }

  @Override
  public void handlePdfPageStart(PdfPage page) {
    this.page = page;
  }

  @Override
  public void handlePdfPageEnd(PdfPage page) {
    this.pdfDocument.addPage(page);
  }

  @Override
  public void handlePdfCharacter(PdfCharacter character) {
    // Check if the character is a ligature. If so, resolve it.
    if (isResolveLigatures()) {
      LigaturesTranslator.resolveLigature(character);
    }

    // Check if the character is a diacritic. If so, resolve it.
    // In most cases, diacritic characters are represented in its decomposed
    // form. For example, "è" may be represented as the two characters "'e" or
    // "e'". To merge such characters the base character must be followed by
    // the diacritic: "e'" implicitly. To maintain this order, decide to which
    // base character a diacritic belongs and merge them together.

    // To decide to which character the diacritic belongs, we have to wait for
    // the character after the diacritic, so we check if the previous character
    // is a diacritic and compare the horizontal overlap between (a) the
    // diacritic and the character "in front" of the character
    // (prePreviousCharacter) and (b) the diacritic and the character "behind"
    // the character (the current character).
    if (isResolveDiacritics()) {
      DiacriticsTranslator.resolveDiacritic(this.prevCharacter,
          this.prevPrevCharacter, character);
    }

    if (!PdfCharacterFilter.filterPdfCharacter(character)) {
      // TODO
      character.setPositionInExtractionOrder(this.page.getCharacters().size());
      character.setPage(this.page);

      this.page.addCharacter(character);
      this.pdfDocument.addCharacter(character);
    }

    this.prevPrevCharacter = this.prevCharacter;
    this.prevCharacter = character;
  }

  @Override
  public void handlePdfFigure(PdfFigure figure) {
    if (!PdfFigureFilter.filterPdfFigure(figure)) {
      figure.setPage(this.page);

      this.page.addFigure(figure);
      this.pdfDocument.addFigure(figure);
    }
  }

  @Override
  public void handlePdfShape(PdfShape shape) {
    if (!PdfShapeFilter.filterPdfShape(shape)) {
      shape.setPage(this.page);

      this.page.addShape(shape);
      this.pdfDocument.addShape(shape);
    }
  }
}
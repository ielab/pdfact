package pdfact.cli.pipes.serialize;

import static pdfact.cli.pipes.serialize.PdfSerializerConstants.B;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.CHARACTER;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.CHARACTERS;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.COLOR;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.COLORS;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.FONT;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.FONTS;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.FONTSIZE;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.G;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.HEIGHT;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.ID;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.MAX_X;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.MAX_Y;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.MIN_X;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.MIN_Y;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.NAME;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.PAGE;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.PAGES;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.PARAGRAPH;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.PARAGRAPHS;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.POSITIONS;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.R;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.ROLE;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.TEXT;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.TEXT_BLOCK;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.TEXT_LINE;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.WIDTH;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.WORD;
import static pdfact.cli.pipes.serialize.PdfSerializerConstants.WORDS;
import static pdfact.core.PdfActCoreSettings.DEFAULT_ENCODING;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import pdfact.cli.model.TextUnit;
import pdfact.core.model.Character;
import pdfact.core.model.Color;
import pdfact.core.model.Element;
import pdfact.core.model.Font;
import pdfact.core.model.FontFace;
import pdfact.core.model.HasColor;
import pdfact.core.model.HasFontFace;
import pdfact.core.model.HasPosition;
import pdfact.core.model.HasPositions;
import pdfact.core.model.HasSemanticRole;
import pdfact.core.model.HasText;
import pdfact.core.model.Page;
import pdfact.core.model.Paragraph;
import pdfact.core.model.PdfDocument;
import pdfact.core.model.Position;
import pdfact.core.model.Rectangle;
import pdfact.core.model.SemanticRole;
import pdfact.core.model.TextBlock;
import pdfact.core.model.TextLine;
import pdfact.core.model.Word;

/**
 * A serializer to serialize a PDF document in JSON format.
 *
 * @author Claudius Korzen
 */
public class PdfJsonSerializer implements PdfSerializer {
  /**
   * The indentation length.
   */
  protected static final int INDENT_LENGTH = 2;

  // ==============================================================================================

  /**
   * The element types to consider on serializing.
   */
  protected TextUnit textUnit;

  /**
   * The semantic roles to consider on serializing.
   */
  protected Set<SemanticRole> rolesFilter;

  /**
   * The fonts of the PDF elements which were in fact serialized.
   */
  protected Set<Font> usedFonts;

  /**
   * The colors of the PDF elements which were in fact serialized.
   */
  protected Set<Color> usedColors;

  // ==============================================================================================
  // Constructors.

  /**
   * Creates a new serializer that serializes a PDF document in JSON format.
   */
  public PdfJsonSerializer() {
    this.usedFonts = new HashSet<>();
    this.usedColors = new HashSet<>();
  }

  /**
   * Creates a new serializer that serializes a PDF document in JSON format.
   * 
   * @param textUnit
   *        The text unit.
   * @param rolesFilter
   *        The semantic roles filter.
   */
  public PdfJsonSerializer(TextUnit textUnit, Set<SemanticRole> rolesFilter) {
    this();
    this.textUnit = textUnit;
    this.rolesFilter = rolesFilter;
  }

  // ==============================================================================================

  @Override
  public byte[] serialize(PdfDocument pdf) {
    // The serialization to return.
    String result = "";

    if (pdf != null) {
      // The JSON object to fill.
      JSONObject json = new JSONObject();

      // Create the section that contains all serialized PDF elements.
      serializePdfElements(pdf, json);
 
      // Create the section that contains the used fonts.
      JSONArray fontsJson = serializeFonts(this.usedFonts);
      if (fontsJson != null && fontsJson.length() > 0) {
        json.put(FONTS, fontsJson);
      }

      // Create the section that contains the used colors.
      JSONArray colorsJson = serializeColors(this.usedColors);
      if (colorsJson != null && colorsJson.length() > 0) {
        json.put(COLORS, colorsJson);
      }

      // Create the section that contains the metadata about the pages.
      JSONArray pagesJson = serializePages(pdf.getPages());
      if (pagesJson != null && pagesJson.length() > 0) {
        json.put(PAGES, pagesJson);
      }

      // Serialize the JSON object.
      result = json.toString(INDENT_LENGTH);
    }
    return result.getBytes(DEFAULT_ENCODING);
  }

  // ==============================================================================================

  /**
   * Serializes the elements of the given PDF document.
   * 
   * @param pdf
   *        The PDF document to process.
   * @param json
   *        The JSON object to which the serialization should be appended.
   */
  public void serializePdfElements(PdfDocument pdf, JSONObject json) {
    switch (this.textUnit) {
      case CHARACTER:
        serializeCharacters(pdf, json);
        break;
      case WORD:
        serializeWords(pdf, json);
        break;
      case PARAGRAPH:
      default:
        serializeParagraphs(pdf, json);
        break;
    }
  }

  // ==============================================================================================

  /**
   * Serializes the paragraphs of the given PDF document.
   * 
   * @param pdf
   *        The PDF document to process.
   * @param json
   *        The JSON object to which the serialization should be appended.
   */
  protected void serializeParagraphs(PdfDocument pdf, JSONObject json) {
    JSONArray result = new JSONArray();

    if (pdf != null) {
      for (Paragraph paragraph : pdf.getParagraphs()) {
        // Ignore the paragraph if its role doesn't match the roles filter.
        if (!hasRelevantRole(paragraph)) {
          continue;
        }

        JSONObject paragraphJson = serializeParagraph(paragraph);
        if (paragraphJson != null) {
          result.put(paragraphJson);
        }
      }
    }

    json.put(PARAGRAPHS, result);
  }

  /**
   * Serializes the given paragraph.
   * 
   * @param paragraph
   *        The paragraph to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializeParagraph(Paragraph paragraph) {
    JSONObject result = new JSONObject();
    JSONObject paragraphJson = serializePdfElement(paragraph);
    // Wrap the JSON object with a JSON object that describes a paragraph.
    if (paragraphJson != null && paragraphJson.length() > 0) {
      result.put(PARAGRAPH, paragraphJson);
    }
    return result;
  }

  // ==============================================================================================

  /**
   * Serializes the words of the given PDF document.
   * 
   * @param pdf
   *        The PDF document to process.
   * @param json
   *        The JSON object to which the serialization should be appended.
   */
  protected void serializeWords(PdfDocument pdf, JSONObject json) {
    JSONArray result = new JSONArray();

    if (pdf != null) {
      for (Paragraph paragraph : pdf.getParagraphs()) {
        // Ignore the paragraph if its role doesn't match the roles filter.
        if (!hasRelevantRole(paragraph)) {
          continue;
        }

        for (Word word : paragraph.getWords()) {
          JSONObject wordJson = serializeWord(word);
          if (wordJson != null) {
            result.put(wordJson);
          }
        }
      }
    }

    json.put(WORDS, result);
  }

  /**
   * Serializes the given word.
   * 
   * @param word
   *        The word to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializeWord(Word word) {
    JSONObject result = new JSONObject();
    JSONObject wordJson = serializePdfElement(word);
    // Wrap the JSON object with a JSON object that describes a word.
    if (wordJson != null && wordJson.length() > 0) {
      result.put(WORD, wordJson);
    }
    return result;
  }

  // ==============================================================================================

  /**
   * Serializes the characters of the given PDF document.
   * 
   * @param pdf
   *        The PDF document to process.
   * @param json
   *        The JSON object to which the serialization should be appended.
   */
  protected void serializeCharacters(PdfDocument pdf, JSONObject json) {
    JSONArray result = new JSONArray();

    if (pdf != null) {
      for (Paragraph paragraph : pdf.getParagraphs()) {
        // Ignore the paragraph if its role doesn't match the roles filter.
        if (!hasRelevantRole(paragraph)) {
          continue;
        }

        for (Word word : paragraph.getWords()) {
          for (Character character : word.getCharacters()) {
            JSONObject characterJson = serializeCharacter(character);
            if (characterJson != null) {
              result.put(characterJson);
            }
          }
        }
      }
    }

    json.put(CHARACTERS, result);
  }

  /**
   * Serializes the given character.
   * 
   * @param character
   *        The character to serialize.
   *
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializeCharacter(Character character) {
    JSONObject result = new JSONObject();
    JSONObject characterJson = serializePdfElement(character);
    // Wrap the JSON object with a JSON object that describes a character.
    if (characterJson != null && characterJson.length() > 0) {
      result.put(CHARACTER, characterJson);
    }
    return result;
  }

  // ==============================================================================================

  /**
   * Serializes the given PDF element.
   * 
   * @param element
   *        The element to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializePdfElement(Element element) {
    JSONObject result = new JSONObject();

    if (element != null) {
      // Serialize the list of positions of the element, if there is any.
      if (element instanceof HasPositions) {
        HasPositions hasPositions = (HasPositions) element;
        List<Position> positions = hasPositions.getPositions();

        JSONArray serialized = serializePositions(positions);
        if (serialized != null && serialized.length() > 0) {
          result.put(POSITIONS, serialized);
        }
      }

      // Serialize the single position of the element, if there is any.
      if (element instanceof HasPosition) {
        HasPosition hasPosition = (HasPosition) element;
        Position position = hasPosition.getPosition();

        JSONArray serialized = serializePositions(position);
        if (serialized != null && serialized.length() > 0) {
          result.put(POSITIONS, serialized);
        }
      }

      // Serialize the role of the element, if there is any.
      if (element instanceof HasSemanticRole) {
        HasSemanticRole hasRole = (HasSemanticRole) element;
        SemanticRole role = hasRole.getSemanticRole();

        if (role != null) {
          result.put(ROLE, role.getName());
        }
      }

      // Serialize the font face of the element, if there is any.
      if (element instanceof HasFontFace) {
        HasFontFace hasFontFace = (HasFontFace) element;
        FontFace fontFace = hasFontFace.getFontFace();

        if (fontFace != null) {
          Font font = fontFace.getFont();
          if (font != null) {
            String fontId = font.getId();
            float size = fontFace.getFontSize();
            if (fontId != null && size > 0) {
              JSONObject fontJson = new JSONObject();
              fontJson.put(ID, fontId);
              fontJson.put(FONTSIZE, size);
              result.put(FONT, fontJson);
              this.usedFonts.add(font);
            }
          }
        }
      }

      // Serialize the color of the element, if there is any.
      if (element instanceof HasColor) {
        HasColor hasColor = (HasColor) element;
        Color color = hasColor.getColor();

        if (color != null) {
          String colorId = color.getId();
          if (colorId != null) {
            JSONObject colorJson = new JSONObject();
            colorJson.put(ID, colorId);
            result.put(COLOR, colorJson);
            this.usedColors.add(color);
          }
        }
      }

      // Serialize the text of the element, if there is any.
      if (element instanceof HasText) {
        HasText hasText = (HasText) element;
        String text = hasText.getText();

        if (text != null) {
          result.put(TEXT, text);
        }
      }
    }

    return result;
  }

  // ==============================================================================================

  /**
   * Serializes the given PDF positions.
   * 
   * @param positions
   *        The positions to serialize.
   * 
   * @return A JSON array that represents the serialization.
   */
  protected JSONArray serializePositions(Position... positions) {
    return serializePositions(Arrays.asList(positions));
  }

  /**
   * Serializes the given list of PDF positions.
   * 
   * @param positions
   *        The list of positions to serialize.
   * 
   * @return A JSON array that represents the serialization.
   */
  protected JSONArray serializePositions(List<Position> positions) {
    JSONArray result = new JSONArray();
    if (positions != null) {
      for (Position position : positions) {
        JSONObject positionJson = serializePosition(position);
        if (positionJson != null && positionJson.length() > 0) {
          result.put(positionJson);
        }
      }
    }
    return result;
  }

  /**
   * Serializes the given PDF position.
   * 
   * @param position
   *        The position to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializePosition(Position position) {
    JSONObject positionJson = new JSONObject();

    if (position != null) {
      Page page = position.getPage();
      int pageNumber = page.getPageNumber();
      Rectangle rect = position.getRectangle();

      if (pageNumber > 0 && rect != null) {
        positionJson.put(PAGE, pageNumber);

        // If we pass primitive floats here, the values would be casted to
        // double values (yielding in inaccurate numbers). So transform the
        // values to Float objects.
        positionJson.put(MIN_X, Float.valueOf(rect.getMinX()));
        positionJson.put(MIN_Y, Float.valueOf(rect.getMinY()));
        positionJson.put(MAX_X, Float.valueOf(rect.getMaxX()));
        positionJson.put(MAX_Y, Float.valueOf(rect.getMaxY()));
      }
    }
    return positionJson;
  }

  // ==============================================================================================

  /**
   * Serializes the given fonts.
   * 
   * @param fonts
   *        The fonts to serialize.
   * 
   * @return A JSON array that represents the serialization.
   */
  protected JSONArray serializeFonts(Font... fonts) {
    return serializeFonts(new HashSet<>(Arrays.asList(fonts)));
  }

  /**
   * Serializes the given fonts.
   * 
   * @param fonts
   *        The fonts to serialize.
   * 
   * @return A JSON array that represents the serialization.
   */
  protected JSONArray serializeFonts(Set<Font> fonts) {
    JSONArray result = new JSONArray();
    if (fonts != null) {
      for (Font font : fonts) {
        JSONObject fontJson = serializeFont(font);
        if (fontJson != null && fontJson.length() > 0) {
          result.put(fontJson);
        }
      }
    }
    return result;
  }

  /**
   * Serializes the given font.
   * 
   * @param font
   *        The font to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializeFont(Font font) {
    JSONObject fontJson = new JSONObject();
    if (font != null) {
      String fontId = font.getId();
      String fontName = font.getNormalizedName();

      if (fontId != null && fontName != null) {
        fontJson.put(ID, fontId);
        fontJson.put(NAME, fontName);
      }
    }
    return fontJson;
  }

  // ==============================================================================================
  // Methods to serialize colors.

  /**
   * Serializes the given colors.
   * 
   * @param colors
   *        The color to serialize.
   * 
   * @return A JSON array that represents the serialization.
   */
  protected JSONArray serializeColors(Color... colors) {
    return serializeColors(new HashSet<>(Arrays.asList(colors)));
  }

  /**
   * Serializes the given colors.
   * 
   * @param colors
   *        The colors to serialize.
   * 
   * @return A JSON array that represents the serialization.
   */
  protected JSONArray serializeColors(Set<Color> colors) {
    JSONArray result = new JSONArray();

    if (colors != null) {
      for (Color color : colors) {
        if (color != null) {
          JSONObject colorJson = serializeColor(color);
          if (colorJson != null && colorJson.length() > 0) {
            result.put(colorJson);
          }
        }
      }
    }
    return result;
  }

  /**
   * Serializes the given color.
   * 
   * @param color
   *        The color to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializeColor(Color color) {
    JSONObject colorJson = new JSONObject();
    if (color != null) {
      String colorId = color.getId();
      float[] rgb = color.getRGB();

      if (colorId != null && rgb != null && rgb.length == 3) {
        colorJson.put(ID, colorId);
        colorJson.put(R, rgb[0]);
        colorJson.put(G, rgb[1]);
        colorJson.put(B, rgb[2]);
      }
    }
    return colorJson;
  }

  // ==============================================================================================
  // Methods to serialize the metadata of pages.

  /**
   * Serializes the metadata of the given pages.
   * 
   * @param pages
   *        The pages to serialize.
   * 
   * @return A JSON array that represents the serialization.
   */
  protected JSONArray serializePages(Page... pages) {
    return serializePages(Arrays.asList(pages));
  }

  /**
   * Serializes the given pages.
   * 
   * @param pages
   *        The pages to serialize.
   * 
   * @return A JSON array that represents the serialization.
   */
  protected JSONArray serializePages(List<Page> pages) {
    JSONArray result = new JSONArray();

    if (pages != null) {
      for (Page page : pages) {
        if (page != null) {
          JSONObject pageJson = serializePage(page);
          if (pageJson != null && pageJson.length() > 0) {
            result.put(pageJson);
          }
        }
      }
    }
    return result;
  }

  /**
   * Serializes the given page.
   * 
   * @param page
   *        The page to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializePage(Page page) {
    JSONObject pageJson = new JSONObject();
    if (page != null) {
      pageJson.put(ID, page.getPageNumber());
      pageJson.put(WIDTH, page.getWidth());
      pageJson.put(HEIGHT, page.getHeight());
    }
    return pageJson;
  }

  // ==============================================================================================

  @Override
  public TextUnit getTextUnit() {
    return this.textUnit;
  }

  @Override
  public void setTextUnit(TextUnit textUnit) {
    this.textUnit = textUnit;
  }

  // ==============================================================================================

  @Override
  public Set<SemanticRole> getSemanticRolesFilter() {
    return this.rolesFilter;
  }

  @Override
  public void setSemanticRolesFilter(Set<SemanticRole> rolesFilter) {
    this.rolesFilter = rolesFilter;
  }

  // ==============================================================================================

  /**
   * Checks if the semantic role of the given element matches the semantic roles
   * filter of this serializer.
   * 
   * @param element
   *        The element to check.
   * 
   * @return True, if the role of the given element matches the semantic roles
   *         filter of this serializer, false otherwise.
   */
  protected boolean hasRelevantRole(HasSemanticRole element) {
    if (element == null) {
      return false;
    }

    if (this.rolesFilter == null || this.rolesFilter.isEmpty()) {
      // No filter is given -> The element is relevant.
      return true;
    }

    SemanticRole role = element.getSemanticRole();
    if (role == null) {
      return false;
    }

    return this.rolesFilter.contains(role);
  }

  // ==============================================================================================
  // Remaining methods.
  
  /**
   * Serializes the given text block.
   * 
   * @param block
   *        The text block to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializeTextBlock(TextBlock block) {
    JSONObject result = new JSONObject();
    JSONObject blockJson = serializePdfElement(block);
    // Wrap the JSON object with a JSON object that describes a text block.
    if (blockJson != null && blockJson.length() > 0) {
      result.put(TEXT_BLOCK, blockJson);
    }
    return result;
  }

  /**
   * Serializes the given text line.
   * 
   * @param line
   *        The text line to serialize.
   * 
   * @return A JSON object that represents the serialization.
   */
  protected JSONObject serializeTextLine(TextLine line) {
    JSONObject result = new JSONObject();
    JSONObject lineJson = serializePdfElement(line);
    // Wrap the JSON object with a JSON object that describes a text line.
    if (lineJson != null && lineJson.length() > 0) {
      result.put(TEXT_LINE, lineJson);
    }
    return result;
  }
}

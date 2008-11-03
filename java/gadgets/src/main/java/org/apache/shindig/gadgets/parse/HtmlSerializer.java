package org.apache.shindig.gadgets.parse;

import org.w3c.dom.Document;

import java.io.StringWriter;

/**
 * Serialize a w3c document. An implementation of this interface should be bound
 * to the document produced by an implementor of HtmlParser and retrieveable via
 * document.getUserData(HtmlSerializer.KEY)
 */
public abstract class HtmlSerializer {

  /**
   * Used to key an instance of HtmlSerializer in
   * document.getUserData
   */
  private static final String KEY = "serializer";

  /**
   * Used by a parser to record the original length of the content it parsed
   * Can be used to optimize output buffers
   */
  private static final String ORIGINAL_LENGTH = "original-length";

  /**
   * Attach a serializer instance to the document
   * @param doc
   * @param serializer
   * @param originalContent may be null
   */
  public static void attach(Document doc, HtmlSerializer serializer, String originalContent) {
    doc.setUserData(KEY, serializer, null);
    if (originalContent != null) {
      doc.setUserData(ORIGINAL_LENGTH, originalContent.length(), null);
    }
  }

  /**
   * Get the length of the original version of the document
   * @param doc
   * @return
   */
  protected static int getOriginalLength(Document doc) {
    Integer length = (Integer)doc.getUserData(ORIGINAL_LENGTH);
    if (length == null) return -1;
    return length;
  }

  /**
   * Create a writer sized to the original length of the document
   * @param doc
   * @return
   */
  protected static StringWriter createWriter(Document doc) {
    int originalLength = getOriginalLength(doc);
    if (originalLength == -1) {
      return new StringWriter(8192);
    } else {
      // Typically rewriting makes a document larger
      return new StringWriter((originalLength * 11) / 10);
    }
  }

  /**
   * Call the attached serializer and output the document
   * @param doc
   * @return
   */
  public static String serialize(Document doc) {
    return ((HtmlSerializer)doc.getUserData(KEY)).serializeImpl(doc);
  }

  /**
   * Overridden by implementations
   * @param doc
   * @return
   */
  protected abstract String serializeImpl(Document doc);

}

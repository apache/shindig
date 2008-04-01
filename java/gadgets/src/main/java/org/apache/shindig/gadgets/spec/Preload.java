package org.apache.shindig.gadgets.spec;

import org.apache.shindig.util.XmlUtil;
import org.w3c.dom.Element;

import java.net.URI;
import java.util.Map;

/**
 * Represents an addressable piece of content that can be preloaded by the server
 * to satisfy makeRequest calls
 */
public class Preload {

  /**
   * Preload@href
   */
  private final URI href;
  public URI getHref() {
    return href;
  }

  /**
   * Produces an xml representation of the Preload.
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<Preload href=\"").append(href).append("\"/>");
    return buf.toString();
  }
  /**
   * Creates a new Preload from an xml node.
   *
   * @param preload The Preload to create
   * @throws SpecParserException When the href is not specified
   */
  public Preload(Element preload) throws SpecParserException {
    href = XmlUtil.getUriAttribute(preload, "href");
    if (href == null) {
      throw new SpecParserException("Preload@href is required.");
    }
  }



}
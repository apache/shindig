/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.common.xml;

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility class for simplifying parsing of xml documents. Documents are not validated, and
 * loading of external files (xinclude, external entities, DTDs, etc.) are disabled.
 */
public final class XmlUtil {
  private static final String CLASSNAME = XmlUtil.class.getName();
  private static final Logger LOG = Logger.getLogger(CLASSNAME, MessageKeys.MESSAGES);

  // Handles xml errors so that they're not logged to stderr.
  private static final ErrorHandler ERROR_HANDLER = new ErrorHandler() {
    public void error(SAXParseException exception) throws SAXException {
      throw exception;
    }
    public void fatalError(SAXParseException exception) throws SAXException {
      throw exception;
    }
    public void warning(SAXParseException exception) {
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, CLASSNAME, "warning", MessageKeys.ERROR_PARSING_XML, exception);
      }
    }
  };

  private static boolean canReuseBuilders = false;

  private static final DocumentBuilderFactory BUILDER_FACTORY
      = DocumentBuilderFactory.newInstance();

  private static final ThreadLocal<DocumentBuilder> REUSABLE_BUILDER
      = new ThreadLocal<DocumentBuilder>() {
          @Override
          protected DocumentBuilder initialValue() {
            try {
              if (LOG.isLoggable(Level.FINE))
                LOG.fine("Created a new document builder");
              return BUILDER_FACTORY.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
              throw new RuntimeException(e);
            }
          }
        };

  static {
    // Namespace support is required for <os:> elements
    BUILDER_FACTORY.setNamespaceAware(true);

    // Disable various insecure and/or expensive options.
    BUILDER_FACTORY.setValidating(false);

    // Can't disable doctypes entirely because they're usually harmless. External entity
    // resolution, however, is both expensive and insecure.
    try {
      BUILDER_FACTORY.setAttribute(
          "http://xml.org/sax/features/external-general-entities", false);
    } catch (IllegalArgumentException e) {
      // Not supported by some very old parsers.
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, CLASSNAME, "static block", MessageKeys.ERROR_PARSING_EXTERNAL_GENERAL_ENTITIES);
      }
    }

    try {
      BUILDER_FACTORY.setAttribute(
          "http://xml.org/sax/features/external-parameter-entities", false);
    } catch (IllegalArgumentException e) {
      // Not supported by some very old parsers.
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, CLASSNAME, "static block", MessageKeys.ERROR_PARSING_EXTERNAL_PARAMETER_ENTITIES);
      }
    }

    try {
      BUILDER_FACTORY.setAttribute(
          "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    } catch (IllegalArgumentException e) {
      // Only supported by Apache's XML parsers.
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, CLASSNAME, "static block", MessageKeys.ERROR_PARSING_EXTERNAL_DTD);
      }
    }

    try {
      BUILDER_FACTORY.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (IllegalArgumentException e) {
      // Not supported by older parsers.
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, CLASSNAME, "static block", MessageKeys.ERROR_PARSING_SECURE_XML);
      }
    }

    try {
      DocumentBuilder builder = BUILDER_FACTORY.newDocumentBuilder();
      builder.reset();
      canReuseBuilders = true;
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, CLASSNAME, "static block", MessageKeys.REUSE_DOC_BUILDERS);
      }
    } catch (UnsupportedOperationException e) {
      // Only supported by newer parsers (xerces 2.8.x+ for instance).
      canReuseBuilders = false;
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, CLASSNAME, "static block", MessageKeys.NOT_REUSE_DOC_BUILDERS);
      }
    } catch (ParserConfigurationException e) {
      // Only supported by newer parsers (xerces 2.8.x+ for instance).
      canReuseBuilders = false;
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, CLASSNAME, "static block", MessageKeys.NOT_REUSE_DOC_BUILDERS);
      }
    }
  }

  private XmlUtil() {}

  /**
   * Extracts an attribute from a node.
   *
   * @param node
   * @param attr
   * @param def
   * @return The value of the attribute, or def
   */
  public static String getAttribute(Node node, String attr, String def) {
    NamedNodeMap attrs = node.getAttributes();
    Node val = attrs.getNamedItem(attr);
    if (val != null) {
      return val.getNodeValue();
    }
    return def;
  }

  /**
   * @param node
   * @param attr
   * @return The value of the given attribute, or null if not present.
   */
  public static String getAttribute(Node node, String attr) {
    return getAttribute(node, attr, null);
  }

  /**
   * Retrieves an attribute as a URI.
   * @param node
   * @param attr
   * @return The parsed uri, or def if the attribute doesn't exist or can not
   *     be parsed as a URI.
   */
  public static Uri getUriAttribute(Node node, String attr, Uri def) {
    String uri = getAttribute(node, attr);
    if (uri != null) {
      try {
        return Uri.parse(uri);
      } catch (IllegalArgumentException e) {
        return def;
      }
    }
    return def;
  }

  /**
   * Retrieves an attribute as a URI.
   * @param node
   * @param attr
   * @return The parsed uri, or null.
   */
  public static Uri getUriAttribute(Node node, String attr) {
    return getUriAttribute(node, attr, null);
  }

  /**
   * Retrieves an attribute as a URI, and verifies that the URI is an http
   * or https URI.
   * @param node
   * @param attr
   * @param base
   *@param def  @return the parsed uri, or def if the attribute is not a valid http or
   * https URI.
   */
  public static Uri getHttpUriAttribute(Node node, String attr, Uri base, Uri def) {
    Uri uri = getUriAttribute(node, attr, def);
    if (uri == null) {
      return def;
    }
    // resolve to base before checking
    if (base != null) {
      uri = base.resolve(uri);
    }
    if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
      return def;
    }
    return uri;
  }

  /**
   * Retrieves an attribute as a URI, and verifies that the URI is an http or https URI.
   * @param node
   * @param attr
   * @param base
   * @return the parsed uri, or null if the attribute is not a valid http or
   * https URI.
   */
  public static Uri getHttpUriAttribute(Node node, String attr, Uri base) {
    return getHttpUriAttribute(node, attr, base, null);
  }

  /**
   * Retrieves an attribute as a boolean.
   *
   * @param node
   * @param attr
   * @param def
   * @return True if the attribute exists and is not equal to "false"
   *    false if equal to "false", and def if not present.
   */
  public static boolean getBoolAttribute(Node node, String attr, boolean def) {
    String value = getAttribute(node, attr);
    if (value == null) {
      return def;
    }
    return Boolean.parseBoolean(value);
  }

  /**
   * @param node
   * @param attr
   * @return True if the attribute exists and is not equal to "false"
   *    false otherwise.
   */
  public static boolean getBoolAttribute(Node node, String attr) {
    return getBoolAttribute(node, attr, false);
  }

  /**
   * @return An attribute coerced to an integer.
   */
  public static int getIntAttribute(Node node, String attr, int def) {
    String value = getAttribute(node, attr);
    if (value == null) {
      return def;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return def;
    }
  }

  /**
   * @return An attribute coerced to an integer.
   */
  public static int getIntAttribute(Node node, String attr) {
    return getIntAttribute(node, attr, 0);
  }

  /**
   * Fetch a builder from the pool, creating a new one only if necessary.
   */
  private static DocumentBuilder getBuilder() throws ParserConfigurationException {
    DocumentBuilder builder;
    if (canReuseBuilders) {
      builder = REUSABLE_BUILDER.get();
      builder.reset();
    } else {
      builder = BUILDER_FACTORY.newDocumentBuilder();
    }
    builder.setErrorHandler(ERROR_HANDLER);
    return builder;
  }

  /**
   * Attempts to parse the input xml into a single element.
   * @param xml
   * @return The document object
   * @throws XmlException if a parse error occured.
   */
  public static Element parse(String xml) throws XmlException {
    DocumentBuilder builder = null;
    try {
      builder = getBuilder();
      InputSource is = new InputSource(new StringReader(xml.trim()));
      return builder.parse(is).getDocumentElement();
    } catch (SAXParseException e) {
      throw new XmlException(
          e.getMessage() + " At: (" + e.getLineNumber() + ',' + e.getColumnNumber() + ')', e);
    } catch (SAXException e) {
      throw new XmlException(e);
    } catch (ParserConfigurationException e) {
      throw new XmlException(e);
    } catch (IOException e) {
      throw new XmlException(e);
    } finally {
      // Remove reference to XmlUtils class to insure classes can be unloaded
      if (builder != null) {
        builder.setErrorHandler(null);
      }
    }
  }

  /**
   * Same as {@link #parse(String)}, but throws a RuntimeException instead of XmlException.
   */
  public static Element parseSilent(String xml) {
    try {
      return parse(xml);
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
  }
}

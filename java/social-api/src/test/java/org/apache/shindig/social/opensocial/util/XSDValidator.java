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
package org.apache.shindig.social.opensocial.util;

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.util.CharsetUtil;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Validator utility for testing.
 */
public class XSDValidator {
  /**
   * The schema language being used.
   */
  private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  /**
   * The XML declaration
   */
  public static final String XMLDEC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

  //class name for logging purpose
  private static final String classname = XSDValidator.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  /**
   * Validate a xml string against a supplied schema.
   *
   * @param xml
   *          the xml presented as a string
   * @param schema
   *          an input stream containing the xsd
   * @return a list of errors or a 0 lenght string if none.
   */
  public static String validate(String xml, InputStream schema) {
    return validate(new ByteArrayInputStream(CharsetUtil.getUtf8Bytes(xml)), schema);
  }

  /**
   * Validate a xml input stream against a supplied schema.
   *
   * @param xml
   *          a stream containing the xml
   * @param schema
   *          a stream containing the schema
   * @return a list of errors or warnings, a 0 lenght string if none.
   */
  public static String validate(InputStream xml, InputStream schema) {

    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(true);
    final StringBuilder errors = new StringBuilder();
    try {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA);
      Schema s = schemaFactory.newSchema(new StreamSource(schema));

      Validator validator = s.newValidator();
      final LSResourceResolver lsr = validator.getResourceResolver();
      validator.setResourceResolver(new LSResourceResolver() {

        public LSInput resolveResource(String arg0, String arg1, String arg2,
            String arg3, String arg4) {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "resolveResource", MessageKeys.RESOLVE_RESOURCE, new Object[] {arg0, arg1, arg2, arg3, arg4});
          }
          return lsr.resolveResource(arg0, arg1, arg2, arg3, arg4);
        }

      });

      validator.validate(new StreamSource(xml));
    } catch (IOException e) {
      // ignore
    } catch (SAXException e) {
      errors.append(e.getMessage()).append('\n');
    }

    return errors.toString();
  }

  /**
   * Process the response string to strip the container element and insert the
   * opensocial schema.
   *
   * @param xml
   * @return
   */
  public static String insertSchema(String xml, String schemaStatement,
      boolean removeContainer) {
    if (xml == null || xml.trim().length() == 0) {
      return xml;
    }

    int start = 0;
    if ( xml.startsWith("<?") ) {
      start = xml.indexOf('>')+1;
      int gt = xml.indexOf('>',start);
      if (gt > 0) {
        return xml.substring(0, gt) + schemaStatement
            + xml.substring(gt);
      }
    } else {
      int gt = xml.indexOf('>',start);
      if (gt > 0) {
        return XMLDEC + xml.substring(0, gt) + schemaStatement
            + xml.substring(gt);
      }

    }
    return xml;
  }

  /**
   * @param xmlFragment
   * @return a list of errors
   */
  public static String validate(String xmlFragment, String schemaStatement,
      String schemaResource, boolean removeContainer) {

    String xml = XSDValidator.insertSchema(xmlFragment, schemaStatement, removeContainer);
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Validating " + xml);
    }
    String errors = XSDValidator.validate(xml, XSDValidator.class
        .getResourceAsStream(schemaResource));
    if (!"".equals(errors)) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.logp(Level.SEVERE, classname, "resolveResource", MessageKeys.FAILED_TO_VALIDATE, new Object[] {xml});
      }
    }
    if (!"".equals(errors)) {
      throw new Error("XML document does not validate \n" + errors + '\n' + xml);
    }
    return xml;
  }

  public static String validateOpenSocial(String xmlFragment) {
    String XMLSCHEMA = " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
    + " xsi:schemaLocation=\"http://ns.opensocial.org/2008/opensocial classpath:opensocial.xsd\" ";

    String xml = XSDValidator.insertSchema(xmlFragment, XMLSCHEMA, true);
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Validating " + xml);
    }
    String errors = XSDValidator.validate(xml, XSDValidator.class
        .getResourceAsStream("opensocial.xsd"));
    if (!"".equals(errors)) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.logp(Level.SEVERE, classname, "resolveResource", MessageKeys.FAILED_TO_VALIDATE, new Object[] {xml});
      }
    }
    if (!"".equals(errors)) {
      throw new Error("XML document does not validate \n" + errors + '\n' + xml);
    }
    return xml;

  }
}

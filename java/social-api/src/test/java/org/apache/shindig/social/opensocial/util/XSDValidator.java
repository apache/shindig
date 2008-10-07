/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.opensocial.util;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 *
 */
public class XSDValidator {
  private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

  private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  public static String validate(String xml, InputStream schema) {
    try {
      return validate(new ByteArrayInputStream(xml.getBytes("UTF-8")), schema);
    } catch (UnsupportedEncodingException e) {
      return e.getMessage();
    }
  }
  public static String validate(InputStream xml, InputStream schema) {
  
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(true);
    final StringBuilder errors = new StringBuilder();
    try {
      
      SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA);
      Schema s = schemaFactory.newSchema(new StreamSource(schema));
      System.err.println("Schema is "+s);
      Validator validator = s.newValidator();
      validator.validate(new StreamSource(xml));
    } catch (IOException e) {
    } catch (SAXException e) {
      errors.append(e.getMessage()).append("\n");
    }

    return errors.toString();
  }
}

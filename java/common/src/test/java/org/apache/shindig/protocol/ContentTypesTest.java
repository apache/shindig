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
package org.apache.shindig.protocol;

import junit.framework.TestCase;

/**
 * Test content type checks
 */
public class ContentTypesTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
  }

  public void testAllowJson() throws Exception {
    ContentTypes.checkContentTypes(ContentTypes.ALLOWED_JSON_CONTENT_TYPES,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE, true);
  }

  public void testAllowJsonRpc() throws Exception {
    ContentTypes.checkContentTypes(ContentTypes.ALLOWED_JSON_CONTENT_TYPES,
        "application/json-rpc", true);
  }

  public void testAllowAtom() throws Exception {
    ContentTypes.checkContentTypes(ContentTypes.ALLOWED_ATOM_CONTENT_TYPES,
        ContentTypes.OUTPUT_ATOM_CONTENT_TYPE, true);
  }

  public void testAllowXml() throws Exception {
    ContentTypes.checkContentTypes(ContentTypes.ALLOWED_XML_CONTENT_TYPES,
        ContentTypes.OUTPUT_XML_CONTENT_TYPE, true);
  }

  public void testAllowMultipart() throws Exception {
    ContentTypes.checkContentTypes(ContentTypes.ALLOWED_MULTIPART_CONTENT_TYPES,
        ContentTypes.MULTIPART_FORM_CONTENT_TYPE, true);
  }

  public void testForbidden() throws Exception {
    try {
      ContentTypes.checkContentTypes(ContentTypes.ALLOWED_JSON_CONTENT_TYPES,
          "application/x-www-form-urlencoded", false);
      fail("Should fail as form encoded is forbidden");
    } catch (ContentTypes.InvalidContentTypeException icte) {
    }
  }

  public void testStrictDisallowUnknown() throws Exception {
    try {
      ContentTypes.checkContentTypes(ContentTypes.ALLOWED_JSON_CONTENT_TYPES,
          "text/plain", true);
      fail("Should fail with strict content type checking enabled");
    } catch (ContentTypes.InvalidContentTypeException icte) {
    }
  }

  public void testNonStrictAllowUnknown() throws Exception {
    ContentTypes.checkContentTypes(ContentTypes.ALLOWED_JSON_CONTENT_TYPES,
        "text/plain", false);
  }

  public void textExtractMimePart() throws Exception {
    assertEquals("text/xml", ContentTypes.extractMimePart("Text/Xml ; charset = ISO-8859-1;x=y"));
  }
}
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

package org.apache.shindig.gadgets.parse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import junit.framework.TestCase;
import org.w3c.dom.Document;

import java.io.IOException;

/**
 * Base test fixture for HTML parsing and serialization.
 */
public abstract class AbstractParserAndSerializerTest extends TestCase {

  /** The vm line separator */
  private static final String EOL = System.getProperty("line.separator");

  protected String loadFile(String path) throws IOException {
    return IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream(path));
  }

  protected void parseAndCompareBalanced(String content, String expected, GadgetHtmlParser parser)
      throws Exception {
    Document document = parser.parseDom(content);
    expected = StringUtils.replace(expected, EOL, "\n");
    assertEquals(expected.trim(), HtmlSerialization.serialize(document).trim());
  }
}

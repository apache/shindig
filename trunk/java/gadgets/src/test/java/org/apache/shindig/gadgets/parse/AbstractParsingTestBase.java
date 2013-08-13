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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

/**
 * Simple base class providing test helpers for parsing/serializing tests.
 */
public abstract class AbstractParsingTestBase {
  /** The vm line separator */
  private static final String EOL = System.getProperty("line.separator");

  protected String loadFile(String path) throws IOException {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(path);
    // ENABLE THIS if you have troubles in your IDE loading resources.
    /*if (is == null) {
      is = new FileInputStream(new File("/path/to/your/files/" + path));
    }*/
    return IOUtils.toString(is);
  }

  protected void parseAndCompareBalanced(String content, String expected, GadgetHtmlParser parser)
      throws Exception {
    Document document = parser.parseDom(content);
    expected = expected.replace(EOL, "\n");
    String serialized = HtmlSerialization.serialize(document);
    assertHtmlEquals(expected, serialized);
  }

  private void assertHtmlEquals(String expected, String serialized) {
    // Compute the diff of expected vs. serialized, and disregard constructs that we don't
    // care about, such as whitespace deltas and differently-computed escape sequences.
    diff_match_patch dmp = new diff_match_patch();
    LinkedList<Diff> diffs = dmp.diff_main(expected, serialized);
    while (!diffs.isEmpty()) {
      Diff cur = diffs.removeFirst();
      switch (cur.operation) {
      case DELETE:
        if (StringUtils.isBlank(cur.text) || "amp;".equalsIgnoreCase(cur.text)) {
          continue;
        }
        if (diffs.isEmpty()) {
          // End of the set: assert known failure.
          assertEquals(expected, serialized);
        }
        Diff next = diffs.removeFirst();
        if (next.operation != Operation.INSERT) {
          // Next operation isn't a paired insert: assert known failure.
          assertEquals(expected, serialized);
        }
        if (!equivalentEntities(cur.text, next.text) &&
            !cur.text.equalsIgnoreCase(next.text)) {
          // Delete/insert pair: fail unless each's text is equivalent
          // either in terms of case or entity equivalence.
          assertEquals(expected, serialized);
        }
        break;
      case INSERT:
        // Assert known failure unless insert is whitespace/blank.
        if (StringUtils.isBlank(cur.text) || "amp;".equalsIgnoreCase(cur.text)) {
          continue;
        }
        assertEquals(expected, serialized);
        break;
      default:
        // EQUALS: move on.
        break;
      }
    }
  }

  private boolean equivalentEntities(String prev, String cur) {
    if (!prev.endsWith(";") && !cur.endsWith(";")) {
      return false;
    }
    String prevEnt = StringEscapeUtils.unescapeHtml4(prev);
    String curEnt = StringEscapeUtils.unescapeHtml4(cur);
    return prevEnt.equals(curEnt);
  }
}

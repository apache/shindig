/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets;

import org.apache.shindig.common.uri.Uri;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.PatternSyntaxException;

public class BasicGadgetBlacklistTest extends Assert {

  private Uri someUri;

  @Before
  public void setUp() throws Exception {
    someUri = Uri.parse("http://bla.com/foo.xml");
  }

  private GadgetBlacklist createBlacklist(String contents) throws IOException {
    File temp = File.createTempFile("blacklist_test", ".txt");
    temp.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
    out.write(contents);
    out.close();
    return new BasicGadgetBlacklist(temp);
  }

  @Test
  public void testEmptyBlacklist() throws Exception {
    GadgetBlacklist bl = createBlacklist("");
    assertFalse(bl.isBlacklisted(someUri));
  }

  @Test
  public void testExactMatches() throws Exception {
    GadgetBlacklist bl = createBlacklist(someUri + "\nhttp://baz.com/foo.xml");
    assertFalse(bl.isBlacklisted(Uri.parse("http://random.com/uri.xml")));
    assertTrue(bl.isBlacklisted(someUri));
  }

  @Test
  public void testExactMatchesWithCaseMixture() throws Exception {
    GadgetBlacklist bl = createBlacklist(someUri + "\nhttp://BAZ.com/foo.xml");
    assertTrue(bl.isBlacklisted(someUri));
    assertTrue(bl.isBlacklisted(Uri.parse("http://BLA.com/foo.xml")));
    assertTrue(bl.isBlacklisted(Uri.parse("http://baz.com/foo.xml")));
  }

  @Test
  public void testIgnoredCommentsAndWhitespace() throws Exception {
    GadgetBlacklist bl = createBlacklist(
        "# comment\n  \t" + someUri + " \n  # comment\n\n");
    assertTrue(bl.isBlacklisted(someUri));
  }

  @Test
  public void testRegexpMatches() throws Exception {
    GadgetBlacklist bl = createBlacklist("REGEXP http://bla.com/.*");
    assertTrue(bl.isBlacklisted(someUri));
    assertTrue(bl.isBlacklisted(Uri.parse("http://bla.com/bar.xml")));
    assertFalse(bl.isBlacklisted(Uri.parse("http://blo.com/bar.xml")));
  }

  @Test(expected=PatternSyntaxException.class)
  public void testInvalidRegularExpression() throws Exception {
    createBlacklist("REGEXP +http://bla.com/.*");
  }
}

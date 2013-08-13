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
package org.apache.shindig.gadgets.variables;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.render.FakeMessageBundleFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.junit.Assert;
import org.junit.Test;

public class BidiSubstituterTest extends Assert {

  @Test
  public void testBidiWithRtl() throws Exception {
    assertRightToLeft(BidiSubstituter.RTL);
  }

  @Test
  public void testBidiWithLtr() throws Exception {
    assertLeftToRight(BidiSubstituter.LTR);
  }

  @Test(expected=SpecParserException.class)
  public void testBidiWithEmpty() throws Exception {
    assertLeftToRight("");
  }

  private void assertRightToLeft(String direction) throws Exception {
    assertSubstitutions(direction, BidiSubstituter.RIGHT,
        BidiSubstituter.LEFT, BidiSubstituter.RTL, BidiSubstituter.LTR);
  }

  private void assertLeftToRight(String direction) throws Exception {
    assertSubstitutions(direction, BidiSubstituter.LEFT,
        BidiSubstituter.RIGHT, BidiSubstituter.LTR, BidiSubstituter.RTL);
  }

  private void assertSubstitutions(String direction,
      String startEdge, String endEdge, String dir, String reverseDir) throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        "  <Locale language_direction='" + direction + "' />" +
        "</ModulePrefs>" +
        "<Content />" +
        "</Module>";

    GadgetSpec spec = new GadgetSpec(Uri.parse("#"), xml);
    GadgetContext context = new GadgetContext();

    BidiSubstituter substituter = new BidiSubstituter(new FakeMessageBundleFactory());

    Substitutions substitutions = new Substitutions();
    substituter.addSubstitutions(substitutions, context, spec);

    assertEquals(startEdge, substitutions.getSubstitution(
        Substitutions.Type.BIDI, BidiSubstituter.START_EDGE));
    assertEquals(endEdge, substitutions.getSubstitution(
        Substitutions.Type.BIDI, BidiSubstituter.END_EDGE));
    assertEquals(dir, substitutions.getSubstitution(
        Substitutions.Type.BIDI, BidiSubstituter.DIR));
    assertEquals(reverseDir, substitutions.getSubstitution(
        Substitutions.Type.BIDI, BidiSubstituter.REVERSE_DIR));
  }
}

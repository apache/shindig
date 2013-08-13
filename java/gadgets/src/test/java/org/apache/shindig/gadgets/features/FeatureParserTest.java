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
package org.apache.shindig.gadgets.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;

import org.junit.Test;

public class FeatureParserTest {
  @Test
  public void parseCompleteFeatureFile() throws Exception {
    Uri parent = Uri.parse("scheme://host.com/root/path");
    String featureXml =
      "<feature>" +
      "  <name>the_feature</name>" +
      "  <dependency>myDep1</dependency>" +
      "  <dependency>mySecondDep</dependency>" +
      "  <gadget>" +
      "    <ignored>This tag is ignored</ignored>" +
      "    <api>" +
      "      <!-- This is a comment about the API -->" +
      "      <uses type=\"js\">foo.symbol</uses>" +
      "      <exports type=\"rpc\">rpc_service</exports>" +
      "      More extraneous crap." +
      "      <exports type=\"js\">bar.symbol</exports>" +
      "    </api>" +
      "    <script src=\"http://www.apache.org/file.js\"/>" +
      "    <api>" +
      "      <!-- A way of adding APIs together per-bundle but not tied to a resource -->" +
      "      <uses type=\"rpc\">uses_service</uses>" +
      "      <uses type=\"js\">last.symbol</uses>" +
      "    </api>" +
      "    <script src=\"relative/resource.js\" gadget_attrib=\"gadget_value\"/>" +
      "  </gadget>" +
      "  <gadget container=\"container1\">" +
      "    <!-- No child values, testing outlier case -->" +
      "  </gadget>" +
      "  <container randomAttrib=\"randomValue\" secondAttrib=\"secondValue\">" +
      "    <script src=\"/authority/relative.js\" r2_attr=\"r2_val\" r3_attr=\"r3_val\"></script>" +
      "    <script>Inlined content</script>" +
      "  </container>" +
      "  <other_type>" +
      "    <api> <!-- No actual API decls here --> </api>" +
      "    <script src=\"http://www.apache.org/two.js\"/>" +
      "    <script src=\"//extern/unchanged.dat\" inline=\"false\"/>" +
      "  </other_type>" +
      "</feature>";
    FeatureParser.ParsedFeature parsed = new FeatureParser().parse(parent, featureXml);

    // Top-level validation.
    assertEquals("the_feature", parsed.getName());
    assertEquals(2, parsed.getDeps().size());
    assertEquals("myDep1", parsed.getDeps().get(0));
    assertEquals("mySecondDep", parsed.getDeps().get(1));
    assertEquals(4, parsed.getBundles().size());

    // First gadget bundle.
    FeatureParser.ParsedFeature.Bundle bundle1 = parsed.getBundles().get(0);
    assertEquals("gadget", bundle1.getType());
    assertEquals(0, bundle1.getAttribs().size());
    assertEquals(2, bundle1.getResources().size());
    assertNull(bundle1.getResources().get(0).getContent());
    assertEquals(Uri.parse("http://www.apache.org/file.js"),
        bundle1.getResources().get(0).getSource());
    assertEquals(0, bundle1.getResources().get(0).getAttribs().size());
    assertNull(bundle1.getResources().get(1).getContent());
    assertEquals(Uri.parse("scheme://host.com/root/relative/resource.js"),
        bundle1.getResources().get(1).getSource());
    assertEquals(1, bundle1.getResources().get(1).getAttribs().size());
    assertEquals("gadget_value", bundle1.getResources().get(1).getAttribs().get("gadget_attrib"));
    assertEquals(5, bundle1.getApis().size());
    assertEquals(ApiDirective.Type.JS, bundle1.getApis().get(0).getType());
    assertTrue(bundle1.getApis().get(0).isUses());
    assertEquals("foo.symbol", bundle1.getApis().get(0).getValue());
    assertEquals(ApiDirective.Type.RPC, bundle1.getApis().get(1).getType());
    assertFalse(bundle1.getApis().get(1).isUses());
    assertEquals("rpc_service", bundle1.getApis().get(1).getValue());
    assertEquals(ApiDirective.Type.JS, bundle1.getApis().get(2).getType());
    assertFalse(bundle1.getApis().get(2).isUses());
    assertEquals("bar.symbol", bundle1.getApis().get(2).getValue());
    assertEquals(ApiDirective.Type.RPC, bundle1.getApis().get(3).getType());
    assertTrue(bundle1.getApis().get(3).isUses());
    assertEquals("uses_service", bundle1.getApis().get(3).getValue());
    assertEquals(ApiDirective.Type.JS, bundle1.getApis().get(4).getType());
    assertTrue(bundle1.getApis().get(4).isUses());
    assertEquals("last.symbol", bundle1.getApis().get(4).getValue());

    // Second gadget bundle.
    FeatureParser.ParsedFeature.Bundle bundle2 = parsed.getBundles().get(1);
    assertEquals("gadget", bundle2.getType());
    assertEquals(1, bundle2.getAttribs().size());
    assertEquals("container1", bundle2.getAttribs().get("container"));
    assertEquals(0, bundle2.getResources().size());
    assertEquals(0, bundle2.getApis().size());

    // Container bundle.
    FeatureParser.ParsedFeature.Bundle bundle3 = parsed.getBundles().get(2);
    assertEquals("container", bundle3.getType());
    assertEquals(2, bundle3.getAttribs().size());
    assertEquals("randomValue", bundle3.getAttribs().get("randomAttrib"));
    assertEquals("secondValue", bundle3.getAttribs().get("secondAttrib"));
    assertEquals(2, bundle3.getResources().size());
    assertNull(bundle3.getResources().get(0).getContent());
    assertEquals(Uri.parse("scheme://host.com/authority/relative.js"),
        bundle3.getResources().get(0).getSource());
    assertEquals(2, bundle3.getResources().get(0).getAttribs().size());
    assertEquals("r2_val", bundle3.getResources().get(0).getAttribs().get("r2_attr"));
    assertEquals("r3_val", bundle3.getResources().get(0).getAttribs().get("r3_attr"));
    assertNull(bundle3.getResources().get(1).getSource());
    assertEquals("Inlined content", bundle3.getResources().get(1).getContent());
    assertEquals(0, bundle3.getResources().get(1).getAttribs().size());
    assertEquals(0, bundle3.getApis().size());

    // Other_type bundle.
    FeatureParser.ParsedFeature.Bundle bundle4 = parsed.getBundles().get(3);
    assertEquals("other_type", bundle4.getType());
    assertEquals(0, bundle4.getAttribs().size());
    assertEquals(2, bundle4.getResources().size());
    assertNull(bundle4.getResources().get(0).getContent());
    assertEquals(Uri.parse("http://www.apache.org/two.js"),
        bundle4.getResources().get(0).getSource());
    assertNull(bundle4.getResources().get(1).getContent());
    assertEquals(Uri.parse("//extern/unchanged.dat"),
        bundle4.getResources().get(1).getSource());
    assertEquals(0, bundle4.getResources().get(0).getAttribs().size());
    assertEquals(0, bundle4.getApis().size());
  }

  @Test(expected=GadgetException.class)
  public void parseInvalidXml() throws GadgetException {
    // Should failed to parse invalid XML");
    new FeatureParser().parse(Uri.parse(""), "This is not valid XML.");
  }
}

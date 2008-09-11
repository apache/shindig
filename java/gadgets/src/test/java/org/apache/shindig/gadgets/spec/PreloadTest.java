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
package org.apache.shindig.gadgets.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.Substitutions;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for Preload
 */
public class PreloadTest {
  private final static String HREF = "http://example.org/preload.xml";
  private final static Set<String> VIEWS
      = new HashSet<String>(Arrays.asList("view0", "view1"));

  @Test
  public void basicPreload() throws Exception {
    String xml = "<Preload href='" + HREF + "'/>";

    Preload preload = new Preload(XmlUtil.parse(xml));

    assertEquals(HREF, preload.getHref().toString());
    assertEquals(AuthType.NONE, preload.getAuthType());
    assertEquals(0, preload.getAttributes().size());
    assertTrue("Default value for sign_owner should be true.",
                preload.isSignOwner());
    assertTrue("Default value for sign_viewer should be true.",
               preload.isSignViewer());
  }

  @Test
  public void authzSigned() throws Exception {
    String xml = "<Preload href='" + HREF + "' authz='signed'/>";

    Preload preload = new Preload(XmlUtil.parse(xml));

    assertEquals(AuthType.SIGNED, preload.getAuthType());
  }

  @Test
  public void authzOAuth() throws Exception {
    String xml = "<Preload href='" + HREF + "' authz='oauth'/>";

    Preload preload = new Preload(XmlUtil.parse(xml));

    assertEquals(AuthType.OAUTH, preload.getAuthType());
  }

  @Test
  public void authzUnknownTreatedAsNone() throws Exception {
    String xml = "<Preload href='foo' authz='bad-bad-bad value!'/>";

    Preload preload = new Preload(XmlUtil.parse(xml));

    assertEquals(AuthType.NONE, preload.getAuthType());
  }

  @Test
  public void multipleViews() throws Exception {
    String xml = "<Preload href='" + HREF + '\'' +
    		     " views='" + StringUtils.join(VIEWS, ',') + "'/>";

    Preload preload = new Preload(XmlUtil.parse(xml));

    assertEquals(VIEWS, preload.getViews());
  }

  @Test
  public void substitutionsOk() throws Exception {
    String xml = "<Preload href='__MSG_preload__'/>";

    Preload preload = new Preload(XmlUtil.parse(xml));
    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Substitutions.Type.MESSAGE, "preload", HREF);
    Preload substituted = preload.substitute(substituter);

    assertEquals(HREF, substituted.getHref().toString());
  }

  @Test
  public void arbitraryAttributes() throws Exception {
    String xml = "<Preload href='" + HREF + "' foo='bar' yo='momma' sub='__MSG_preload__'/>";

    Preload preload = new Preload(XmlUtil.parse(xml));
    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Substitutions.Type.MESSAGE, "preload", "stuff");
    Preload substituted = preload.substitute(substituter);
    assertEquals("bar", substituted.getAttributes().get("foo"));
    assertEquals("momma", substituted.getAttributes().get("yo"));
    assertEquals("stuff", substituted.getAttributes().get("sub"));
  }

  @Test
  public void toStringIsSane() throws Exception {
    String xml = "<Preload" +
    		     " href='" + HREF + '\'' +
    		     " authz='signed'" +
    		     " views='" + StringUtils.join(VIEWS, ',') + "'" +
    		     " some_attribute='yes' />";

    Preload preload = new Preload(XmlUtil.parse(xml));
    Preload preload2 = new Preload(XmlUtil.parse(preload.toString()));

    assertEquals(VIEWS, preload2.getViews());
    assertEquals(HREF, preload2.getHref().toString());
    assertEquals(AuthType.SIGNED, preload2.getAuthType());
    assertEquals("yes", preload2.getAttributes().get("some_attribute"));
  }

  @Test(expected = SpecParserException.class)
  public void missingHrefThrows() throws Exception {
    String xml = "<Preload/>";
    new Preload(XmlUtil.parse(xml));
  }

  @Test(expected = SpecParserException.class)
  public void malformedHrefThrows() throws Exception {
    String xml = "<Preload href='@$%@$%$%'/>";
    new Preload(XmlUtil.parse(xml));
  }
}

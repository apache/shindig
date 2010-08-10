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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;

import org.junit.Test;

public class ApplicationManifestTest {
  private static final Uri BASE_URI = Uri.parse("http://example.org/manifest.xml");

  @Test
  public void resolveRelativeUri() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <version>1.0</version>" +
        "  <spec>app.xml</spec>" +
        "</gadget></app>";

    ApplicationManifest manifest = new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));

    assertEquals(BASE_URI.resolve(Uri.parse("app.xml")), manifest.getGadget("1.0"));
    assertEquals(BASE_URI, manifest.getUri());
  }

  @Test(expected = SpecParserException.class)
  public void malformedUriThrows() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <version>1.0</version>" +
        "  <spec><![CDATA[$%&$%*$%&$%(]]></spec>" +
        "</gadget></app>";

    new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));
  }

  @Test
  public void getVersion() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>development</label>" +
        "  <version>2.0</version>" +
        "  <spec>whatever</spec>" +
        "</gadget>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <version>1.0</version>" +
        "  <spec>whatever</spec>" +
        "</gadget></app>";

    ApplicationManifest manifest = new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));

    assertEquals("1.0", manifest.getVersion("production"));
    assertEquals("2.0", manifest.getVersion("development"));
  }

  @Test(expected = SpecParserException.class)
  public void missingVersion() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <spec>whatever</spec>" +
        "</gadget></app>";

    new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));
  }

  @Test(expected = SpecParserException.class)
  public void tooManyVersions() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <version>1.0</version>" +
        "  <version>2.0</version>" +
        "  <spec>whatever</spec>" +
        "</gadget></app>";

    new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));
  }

  @Test
  public void getGadget() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>development</label>" +
        "  <version>2.0</version>" +
        "  <spec>app2.xml</spec>" +
        "</gadget>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <version>1.0</version>" +
        "  <spec>app.xml</spec>" +
        "</gadget></app>";

    ApplicationManifest manifest = new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));

    assertEquals(BASE_URI.resolve(Uri.parse("app.xml")), manifest.getGadget("1.0"));
    assertEquals(BASE_URI.resolve(Uri.parse("app2.xml")), manifest.getGadget("2.0"));
  }

  @Test(expected = SpecParserException.class)
  public void missingSpec() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <version>1.0</version>" +
        "</gadget></app>";

    new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));
  }

  @Test(expected = SpecParserException.class)
  public void tooManySpecs() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <version>1.0</version>" +
        "  <spec>whoever</spec>" +
        "  <spec>whatever</spec>" +
        "</gadget></app>";

    new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));
  }

  @Test(expected = SpecParserException.class)
  public void selfReferencingManifest() throws Exception {
    String xml =
        "<app xmlns='" + ApplicationManifest.NAMESPACE + "'>" +
        "<gadget>" +
        "  <label>production</label>" +
        "  <version>1.0</version>" +
        "  <spec>whoever</spec>" +
        "  <spec>" + BASE_URI + "</spec>" +
        "</gadget></app>";

    new ApplicationManifest(BASE_URI, XmlUtil.parse(xml));
  }

}

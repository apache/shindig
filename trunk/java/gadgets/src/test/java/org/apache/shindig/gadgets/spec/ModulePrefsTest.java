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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Map;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.variables.Substitutions;
import org.junit.Test;
import org.w3c.dom.Node;

import com.google.common.collect.Multimap;

public class ModulePrefsTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");
  private static final String FULL_XML
      = "<ModulePrefs" +
        " title='title'" +
        " title_url='title_url'" +
        " description='description'" +
        " author='author'" +
        " author_email='author_email'" +
        " screenshot='screenshot'" +
        " thumbnail='thumbnail'" +
        " directory_title='directory_title'" +
        " width='1'" +
        " height='2'" +
        " scrolling='true'" +
        " category='category'" +
        " category2='category2'" +
        " author_affiliation='author_affiliation'" +
        " author_location='author_location'" +
        " author_photo='author_photo'" +
        " author_aboutme='author_aboutme'" +
        " author_quote='author_quote'" +
        " author_link='author_link'" +
        " show_stats='true'" +
        " show_in_directory='true'" +
        " singleton='true'>" +
        "  <Require feature='require'/>" +
        "  <Optional feature='optional'/>" +
        "  <Preload href='http://example.org' authz='signed'/>" +
        "	 <Require feature='requiredview1' views='default, view1'/>" +
        "	 <Require feature='requiredview2' views='view2'/>" +
        "	 <Require feature='require' views='view2'>" +
        "	 		<Param name='param_name'>param_value</Param>" +
        "  </Require>" +
        "  <Icon/>" +
        "  <Locale/>" +
        "  <Link rel='link' href='http://example.org/link'/>" +
        "  <OAuth>" +
        "    <Service name='serviceOne'>" +
        "      <Request url='http://www.example.com/request'" +
        "          method='GET' param_location='auth-header' />" +
        "      <Authorization url='http://www.example.com/authorize'/>" +
        "      <Access url='http://www.example.com/access' method='GET'" +
        "          param_location='auth-header' />" +
        "    </Service>" +
        "  </OAuth>" +
        "  <NavigationItem title=\"moo\"><AppParameter key=\"test\" value=\"1\"/></NavigationItem>" +
        "</ModulePrefs>";

  private void doAsserts(ModulePrefs prefs) {
    assertEquals("title", prefs.getTitle());
    assertEquals(SPEC_URL.resolve(Uri.parse("title_url")), prefs.getTitleUrl());
    assertEquals("description", prefs.getDescription());
    assertEquals("author", prefs.getAuthor());
    assertEquals("author_email", prefs.getAuthorEmail());
    assertEquals(SPEC_URL.resolve(Uri.parse("screenshot")), prefs.getScreenshot());
    assertEquals(SPEC_URL.resolve(Uri.parse("thumbnail")), prefs.getThumbnail());
    assertEquals("directory_title", prefs.getDirectoryTitle());
    assertEquals(1, prefs.getWidth());
    assertEquals(2, prefs.getHeight());
    assertTrue(prefs.getScrolling());
    assertFalse(prefs.getScaling());
    assertEquals("category", prefs.getCategories().get(0));
    assertEquals("category2", prefs.getCategories().get(1));
    assertEquals("author_affiliation", prefs.getAuthorAffiliation());
    assertEquals("author_location", prefs.getAuthorLocation());
    assertEquals(SPEC_URL.resolve(Uri.parse("author_photo")), prefs.getAuthorPhoto());
    assertEquals(SPEC_URL.resolve(Uri.parse("author_link")), prefs.getAuthorLink());
    assertEquals("author_aboutme", prefs.getAuthorAboutme());
    assertEquals("author_quote", prefs.getAuthorQuote());
    assertTrue(prefs.getShowStats());
    assertTrue(prefs.getShowInDirectory());
    assertTrue(prefs.getSingleton());

    assertTrue(prefs.getFeatures().get("require").getRequired());
    assertFalse(prefs.getFeatures().get("optional").getRequired());

    assertEquals("http://example.org",
        prefs.getPreloads().get(0).getHref().toString());

    assertEquals(1, prefs.getIcons().size());

    assertEquals(1, prefs.getLocales().size());

    assertEquals(Uri.parse("http://example.org/link"), prefs.getLinks().get("link").getHref());

    OAuthService oauth = prefs.getOAuthSpec().getServices().get("serviceOne");
    assertEquals(Uri.parse("http://www.example.com/request"), oauth.getRequestUrl().url);
    assertEquals(OAuthService.Method.GET, oauth.getRequestUrl().method);
    assertEquals(OAuthService.Method.GET, oauth.getAccessUrl().method);
    assertEquals(OAuthService.Location.HEADER, oauth.getAccessUrl().location);
    assertEquals(Uri.parse("http://www.example.com/authorize"), oauth.getAuthorizationUrl());

    Multimap<String, Node> extra = prefs.getExtraElements();

    assertTrue(extra.containsKey("NavigationItem"));
    assertEquals(1, extra.get("NavigationItem").iterator().next().getChildNodes().getLength());
  }

  @Test
  public void substitutionsCopyConstructor() throws Exception{
    ModulePrefs basePrefs = new ModulePrefs(XmlUtil.parse(FULL_XML), SPEC_URL);
    Substitutions substituter = new Substitutions();
    String gadgetXml = "<Module>" +
    FULL_XML +
    "<Content type=\"html\"></Content>" +
    "</Module>";
    GadgetSpec baseSpec = new GadgetSpec(SPEC_URL, gadgetXml);
    GadgetSpec spec = baseSpec.substitute(substituter);
    ModulePrefs subsPrefs = spec.getModulePrefs();
    assertEquals(basePrefs.toString(), subsPrefs.toString());
    doAsserts(subsPrefs);
  }

  @Test
  public void basicElementsParseOk() throws Exception {
    doAsserts(new ModulePrefs(XmlUtil.parse(FULL_XML), SPEC_URL));
  }

  @Test
  public void getAttribute() throws Exception {
    String xml = "<ModulePrefs title='title' some_attribute='attribute' " +
        "empty_attribute=''/>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("title", prefs.getAttribute("title"));
    assertEquals("attribute", prefs.getAttribute("some_attribute"));
    assertEquals("", prefs.getAttribute("empty_attribute"));
    assertNull(prefs.getAttribute("gobbledygook"));
  }

  @Test
  public void getGlobalLocale() throws Exception {
    String xml = "<ModulePrefs title='locales'>" +
                 "  <Locale lang='en' country='UK' messages='en.xml'/>" +
                 "  <Locale lang='foo' language_direction='rtl'/>" +
                 "</ModulePrefs>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    LocaleSpec spec = prefs.getGlobalLocale(new Locale("en", "UK"));
    assertEquals("http://example.org/en.xml", spec.getMessages().toString());

    spec = prefs.getGlobalLocale(new Locale("foo", "ALL"));
    assertEquals("rtl", spec.getLanguageDirection());

    spec = prefs.getGlobalLocale(new Locale("en", "notexist"));
    assertNull(spec);
  }

  @Test
  public void getViewLocale() throws Exception {
    String xml = "<ModulePrefs title='locales'>" +
                 "  <Locale lang='en' country='UK' messages='en.xml' views=\"view1\"/>" +
                 "  <Locale lang='en' country='US' messages='en_US.xml' views=\"view2\"/>" +
                 "  <Locale lang='foo' language_direction='rtl'/>" +
                 "</ModulePrefs>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    LocaleSpec spec = prefs.getGlobalLocale(new Locale("en", "UK"));
    assertNull(spec);
    spec = prefs.getLocale(new Locale("en", "UK"),"view1");
    assertEquals("http://example.org/en.xml", spec.getMessages().toString());
    spec = prefs.getLocale(new Locale("en", "UK"),"view2");
    assertNull(spec);
    spec = prefs.getLocale(new Locale("en", "US"),"view2");
    assertEquals("http://example.org/en_US.xml", spec.getMessages().toString());
    spec = prefs.getLocale(new Locale("foo", "ALL"),"view2");
    assertEquals("rtl", spec.getLanguageDirection());
  }

  @Test
  public void getLinks() throws Exception {
    String link1Rel = "foo";
    String link2Rel = "bar";
    Uri link1Href = Uri.parse("http://example.org/foo");
    Uri link2Href = Uri.parse("/bar");
    String xml = "<ModulePrefs title='links'>" +
                 "  <Link rel='" + link1Rel + "' href='" + link1Href + "'/>" +
                 "  <Link rel='" + link2Rel + "' href='" + link2Href + "'/>" +
                 "</ModulePrefs>";

    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL)
        .substitute(new Substitutions());

    assertEquals(link1Href, prefs.getLinks().get(link1Rel).getHref());
    assertEquals(SPEC_URL.resolve(link2Href), prefs.getLinks().get(link2Rel).getHref());
  }

  @Test
  public void getViewFeatures() throws Exception {
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(FULL_XML), SPEC_URL);
    Map<String, Feature> features = prefs.getViewFeatures("default");
    assertEquals(5, features.size());
    assertTrue(features.containsKey("requiredview1"));
    assertTrue(features.containsKey("require"));
    assertTrue(features.containsKey("optional"));
    assertTrue(features.containsKey("core"));
    assertTrue(features.containsKey("security-token"));
    assertTrue(!features.containsKey("requiredview2"));

    features = prefs.getViewFeatures("view2");
    assertEquals(5, features.size());
    assertTrue(features.containsKey("requiredview2"));
    assertTrue(features.containsKey("require"));
    assertTrue(features.containsKey("optional"));
    assertTrue(features.containsKey("core"));
    assertTrue(features.containsKey("security-token"));
    assertTrue(!features.containsKey("requiredview1"));
  }

  @Test
  public void getViewFeatureParams() throws Exception {
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(FULL_XML), SPEC_URL);
    Map<String, Feature> features = prefs.getViewFeatures("view2");
    String paramValue = features.get("require").getParam("param_name");
    assertNotNull(paramValue);
    assertEquals("param_value", paramValue);
  }

  @Test
  public void doSubstitution() throws Exception {
    String xml = "<ModulePrefs title='__MSG_title__'>" +
                 "  <Icon>__MSG_icon__</Icon>" +
                 "  <Link rel='__MSG_rel__' href='__MSG_link_href__'/>" +
                 "  <Preload href='__MSG_pre_href__'/>" +
                 "    <Require feature=\"testFeature\">" +
                 "           <Param name=\"test_param\">__MSG_test_param__</Param>" +
                 "  </Require>" +
                 "</ModulePrefs>";
    String title = "blah";
    String icon = "http://example.org/icon.gif";
    String rel = "foo-bar";
    String linkHref = "http://example.org/link.html";
    String preHref = "http://example.org/preload.html";
    String testParam = "bar-foo";

    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    Substitutions subst = new Substitutions();
    subst.addSubstitution(Substitutions.Type.MESSAGE, "title", title);
    subst.addSubstitution(Substitutions.Type.MESSAGE, "icon", icon);
    subst.addSubstitution(Substitutions.Type.MESSAGE, "rel", rel);
    subst.addSubstitution(Substitutions.Type.MESSAGE, "link_href", linkHref);
    subst.addSubstitution(Substitutions.Type.MESSAGE, "pre_href", preHref);
    subst.addSubstitution(Substitutions.Type.MESSAGE, "test_param", testParam);
    prefs = prefs.substitute(subst);

    assertEquals(title, prefs.getTitle());
    assertEquals(icon, prefs.getIcons().get(0).getContent());
    assertEquals(rel, prefs.getLinks().get(rel).getRel());
    assertEquals(linkHref, prefs.getLinks().get(rel).getHref().toString());
    assertEquals(preHref, prefs.getPreloads().get(0).getHref().toString());
    assertEquals(testParam, prefs.getFeatures().get("testFeature").getParam("test_param"));
  }

  @Test
  public void malformedIntAttributeTreatedAsZero() throws Exception {
    String xml = "<ModulePrefs title='' height='100px' width='foobar' arbitrary='0xff'/>";

    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);

    assertEquals(0, prefs.getHeight());
    assertEquals(0, prefs.getWidth());
    assertEquals(0, prefs.getIntAttribute("arbitrary"));
  }

  @Test
  public void missingTitleOkay() throws Exception {
    String xml = "<ModulePrefs/>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    assertNotNull("Empty ModulePrefs Parses", prefs);
    assertEquals("Title is empty string", "", prefs.getTitle());
  }

  @Test
  public void coreInjectedAutomatically() throws Exception {
    String xml = "<ModulePrefs title=''><Require feature='foo'/></ModulePrefs>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    assertEquals(2, prefs.getFeatures().size());
    assertTrue(prefs.getFeatures().containsKey("foo"));
    assertTrue(prefs.getFeatures().containsKey("core"));
  }

  @Test
  public void coreNotInjectedOnSplitCoreInclusion() throws Exception {
    String xml = "<ModulePrefs title=''><Require feature='core.config'/></ModulePrefs>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    assertEquals(1, prefs.getFeatures().size());
    assertTrue(prefs.getFeatures().containsKey("core.config"));
  }

  @Test
  public void securityTokenInjectedOnOAuthTag() throws Exception {
    // Make sure it is injected when it should be
    String xml =
        "<ModulePrefs title=''>" +
        "  <OAuth>" +
        "    <Service name='serviceOne'>" +
        "      <Request url='http://www.example.com/request'" +
        "          method='GET' param_location='auth-header' />" +
        "      <Authorization url='http://www.example.com/authorize'/>" +
        "      <Access url='http://www.example.com/access' method='GET'" +
        "          param_location='auth-header' />" +
        "    </Service>" +
        "  </OAuth>" +
        "</ModulePrefs>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    Map<String, Feature> features = prefs.getFeatures();
    assertEquals(2, features.size());
    assertTrue(features.containsKey("core"));
    assertTrue(features.containsKey("security-token"));

    Map<String, Feature> viewFeatures = prefs.getViewFeatures("default");
    assertEquals(2, viewFeatures.size());
    assertTrue(viewFeatures.containsKey("core"));
    assertTrue(viewFeatures.containsKey("security-token"));
  }

  @Test
  public void securityTokenNotInjectedByDefault() throws Exception {
    // Make sure the token isn't injected when it shouldn't be, i.e., not required/optional and no
    // OAuth
    String xml =
      "<ModulePrefs title=''>" +
      "</ModulePrefs>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    Map<String, Feature> features = prefs.getFeatures();
    assertEquals(1, features.size());
    assertTrue(features.containsKey("core"));

    Map<String, Feature> viewFeatures = prefs.getViewFeatures("default");
    assertEquals(1, viewFeatures.size());
    assertTrue(viewFeatures.containsKey("core"));
  }

  @Test
  public void toStringIsSane() throws Exception {
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(FULL_XML), SPEC_URL);
    doAsserts(new ModulePrefs(XmlUtil.parse(prefs.toString()), SPEC_URL));
  }

  @Test
  public void needsUserPrefSubstInTitle() throws Exception {
    String xml = "<ModulePrefs title='Title __UP_foo__'/>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    assertTrue(prefs.needsUserPrefSubstitution());
  }

  @Test
  public void needsUserPrefSubstInTitleUrl() throws Exception {
    String xml = "<ModulePrefs title='foo' title_url='http://__UP_url__'/>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    assertTrue(prefs.needsUserPrefSubstitution());
  }

  @Test
  public void needsUserPrefSubstInPreload() throws Exception {
    String xml = "<ModulePrefs title='foo'>" +
        "  <Preload href='__UP_foo__' authz='signed'/></ModulePrefs>";
    ModulePrefs prefs = new ModulePrefs(XmlUtil.parse(xml), SPEC_URL);
    assertTrue(prefs.needsUserPrefSubstitution());
  }
}

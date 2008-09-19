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
package org.apache.shindig.gadgets.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.json.JSONObject;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for RenderingContentRewriter.
 */
public class RenderingContentRewriterTest {
  private static final String BODY_CONTENT = "Some body content";
  private static final Pattern EXPECTED_DOCUMENT_PATTERN = Pattern.compile(
      "(.*</head>)(.*</body>)(.*)", Pattern.DOTALL);
  private final IMocksControl control = EasyMock.createNiceControl();
  private final ContainerConfig config = control.createMock(ContainerConfig.class);
  private final FakeMessageBundleFactory messageBundleFactory = new FakeMessageBundleFactory();
  private final RenderingContentRewriter rewriter
      = new RenderingContentRewriter(messageBundleFactory);

  @Test
  public void defaultOutput() throws Exception {
    String gadgetXml =
        "<Module><ModulePrefs title=''/>" +
        "<Content type='html'>" + BODY_CONTENT + "</Content>" +
        "</Module>";

    GadgetSpec spec = new GadgetSpec(URI.create("#"), gadgetXml);
    Gadget gadget
        = new Gadget(new GadgetContext(), spec, Collections.<JsLibrary>emptySet(), config, null);

    control.replay();

    assertEquals(0, rewriter.rewrite(gadget).getCacheTtl());

    Matcher matcher = EXPECTED_DOCUMENT_PATTERN.matcher(gadget.getContent());
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("Missing opening html tag", matcher.group(1).contains("<html>"));
    assertTrue("Default head content is missing.",
        matcher.group(1).contains(RenderingContentRewriter.DEFAULT_HEAD_CONTENT));
    // Not very accurate -- could have just been user prefs.
    assertTrue("Default javascript not included.", matcher.group(1).contains("<script>"));
    assertTrue("Original document not preserved.", matcher.group(2).contains(BODY_CONTENT));
    assertTrue("Missing closing html tag.", matcher.group(3).contains("</html>"));
  }

  @Test
  public void bidiSettings() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      " <Locale language_direction='rtl'/>" +
      "</ModulePrefs>" +
      "<Content type='html'>" + BODY_CONTENT + "</Content>" +
      "</Module>";

    GadgetSpec spec = new GadgetSpec(URI.create("#"), gadgetXml);
    Gadget gadget
        = new Gadget(new GadgetContext(), spec, Collections.<JsLibrary>emptySet(), config, null);

    control.replay();

    rewriter.rewrite(gadget);

    assertTrue("Bi-directional locale settings not preserved.",
        gadget.getContent().contains("<body dir='rtl'>"));
  }

  @Test
  public void jsConfigurationInjected() throws Exception {
    // TODO
  }

  @Test
  public void userPrefsInitializationInjected() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Locale>" +
      "    <msg name='one'>foo</msg>" +
      "    <msg name='two'>bar</msg>" +
      "  </Locale>" +
      "</ModulePrefs>" +
      "<Content type='html'>" + BODY_CONTENT + "</Content>" +
      "</Module>";

    GadgetSpec spec = new GadgetSpec(URI.create("#"), gadgetXml);
    Gadget gadget
        = new Gadget(new GadgetContext(), spec, Collections.<JsLibrary>emptySet(), config, null);

    control.replay();

    rewriter.rewrite(gadget);

    Pattern prefsPattern = Pattern.compile("(?:.*)gadgets\\.Prefs\\.setMessages_\\((.*)\\);(?:.*)");
    Matcher matcher = prefsPattern.matcher(gadget.getContent());
    assertTrue("gadgets.Prefs.setMessages_ not invoked.", matcher.matches());
    JSONObject json = new JSONObject(matcher.group(1));
    assertEquals("foo", json.get("one"));
    assertEquals("bar", json.get("two"));
  }

  /**
   * Simple message bundle factory -- only honors inline bundles.
   */
  private static class FakeMessageBundleFactory implements MessageBundleFactory {
    public MessageBundle getBundle(GadgetSpec spec, Locale locale, boolean ignoreCache) {
      LocaleSpec localeSpec = spec.getModulePrefs().getLocale(locale);
      if (localeSpec == null) {
        return MessageBundle.EMPTY;
      }
      return spec.getModulePrefs().getLocale(locale).getMessageBundle();
    }
  }
}

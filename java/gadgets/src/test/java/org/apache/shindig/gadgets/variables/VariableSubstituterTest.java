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

import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.UserPrefs;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Locale;

public class VariableSubstituterTest {
  private final FakeMessageBundleFactory messageBundleFactory = new FakeMessageBundleFactory();
  private final VariableSubstituter substituter = new VariableSubstituter(ImmutableList.<Substituter>of(
    new MessageSubstituter(messageBundleFactory),
    new UserPrefSubstituter(),
    new ModuleSubstituter(),
    new BidiSubstituter(messageBundleFactory)
  ));

  private GadgetSpec substitute(String xml) throws Exception {
    return substituter.substitute(new GadgetContext(), new GadgetSpec(Uri.parse("#"), xml));
  }

  @Test
  public void messageBundlesSubstituted() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        "  <Locale>" +
        "    <msg name='foo'>bar</msg>" +
        "    <msg name='bar'>baz</msg>" +
        "  </Locale>" +
        "</ModulePrefs>" +
        "<Content>__MSG_foo__ - __MSG_bar__</Content>" +
        "</Module>";
    GadgetSpec spec = substitute(xml);

    assertEquals("bar - baz", spec.getView("default").getContent());
  }

  @Test
  public void bidiSubstituted() throws Exception {
    String xml = "<Module><ModulePrefs title='__BIDI_END_EDGE__ way'/><Content/></Module>";
    GadgetSpec spec = substitute(xml);

    assertEquals("right way", spec.getModulePrefs().getTitle());
  }

  @Test
  public void moduleIdSubstituted() throws Exception {
    String xml = "<Module><ModulePrefs title='Module is: __MODULE_ID__'/><Content/></Module>";
    GadgetSpec spec = substitute(xml);

    assertEquals("Module is: 0", spec.getModulePrefs().getTitle());
  }

  @Test
  public void userPrefsSubstituted() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title='I heart __UP_foo__'/>" +
                 "<UserPref name='foo'/>" +
                 "<Content/>" +
                 "</Module>";
    GadgetSpec spec = new GadgetSpec(Uri.parse("#"), xml);
    GadgetContext context = new GadgetContext() {
      @Override
      public UserPrefs getUserPrefs() {
        return new UserPrefs(ImmutableMap.of("foo", "shindig"));
      }
    };

    spec = substituter.substitute(context, spec);

    assertEquals("I heart shindig", spec.getModulePrefs().getTitle());
  }

  @Test
  public void nestedMessageBundleInUserPrefSubstituted() throws Exception {
    String xml =
      "<Module>" +
      " <ModulePrefs title='__UP_title__ for __MODULE_ID__'>" +
      "  <Locale>" +
      "   <msg name='title'>Gadget title</msg>" +
      "  </Locale>" +
      " </ModulePrefs>" +
      " <UserPref name='title' default_value='__MSG_title__' />" +
      " <Content />" +
      "</Module>";

    GadgetSpec spec = substitute(xml);

    assertEquals("Gadget title for 0", spec.getModulePrefs().getTitle());
  }

  private static class FakeMessageBundleFactory implements MessageBundleFactory {

    public MessageBundle getBundle(GadgetSpec spec, Locale locale, boolean ignoreCache, String container, String view)
        throws GadgetException {
      LocaleSpec localeSpec = spec.getModulePrefs().getLocale(locale, view);
      if (localeSpec == null) {
        return MessageBundle.EMPTY;
      }
      return localeSpec.getMessageBundle();
    }
  }
}

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
package org.apache.shindig.gadgets;

import static org.easymock.EasyMock.expect;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Tests for Gadget
 */
public class GadgetTest extends EasyMockTestCase {
  private final static String SPEC_URL = "http://example.org/gadget.xml";
  private final static String SPEC_XML
      = "<Module>" +
        "<ModulePrefs title='title'>" +
        "  <Preload href='http://example.org/foo'/>" +
        "  <Locale>" +
        "    <msg name='name'>VALUE</msg>" +
        "  </Locale>" +
        "</ModulePrefs>" +
        "<Content type='html'>DEFAULT VIEW</Content>" +
        "<Content view='one' type='html'>VIEW ONE</Content>" +
        "<Content view='two' type='html'>VIEW TWO</Content>" +
        "</Module>";

  private final DummyContext context = new DummyContext();

  @Test
  public void getLocale() throws Exception {
    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(new GadgetSpec(Uri.parse(SPEC_URL), SPEC_XML));

    LocaleSpec localeSpec = gadget.getLocale();
    assertEquals("VALUE", localeSpec.getMessageBundle().getMessages().get("name"));
  }

  private GadgetFeature makeFeature(String name, List<String> deps)
      throws GadgetException {
    JsLibrary lib = JsLibrary.create(JsLibrary.Type.INLINE, name, name, null);
    if (deps == null) {
      deps = Lists.newArrayList();
    }
    return new GadgetFeature(name, Arrays.asList(lib), deps);
  }

  @Test
  public void testGetFeatures() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"hello\">" +
                 "<Require feature=\"required1\"/>" +
                 "<Require feature=\"required2\"/>" +
                 "</ModulePrefs>" +
                 "<Content type=\"html\"/>" +
                 "</Module>";
    List<GadgetFeature> features = Lists.newArrayList(
        makeFeature("required1", Lists.newArrayList("required2", "required3")),
        makeFeature("required2", Lists.newArrayList("required3", "required4", "required5")),
        makeFeature("required3", Lists.newArrayList("required4", "required5")),
        makeFeature("required4", null),
        makeFeature("required4", null));
    GadgetFeatureRegistry registry = mock(GadgetFeatureRegistry.class);
    Gadget gadget = new Gadget()
        .setContext(context)
        .setGadgetFeatureRegistry(registry)
        .setSpec(new GadgetSpec(Uri.parse(SPEC_URL), xml));
    Set<String> needed = Sets.newHashSet("required1", "required2");
    expect(registry.getFeatures(needed)).andReturn(features).anyTimes();
    replay(registry);
    List<String> requiredFeatures = gadget.getAllFeatures();
    assertEquals(5, requiredFeatures.size());
    // make sure the dependencies are in order.
    assertTrue(requiredFeatures.get(0).equals("required4") || requiredFeatures.get(0).equals("required5"));
    assertTrue(requiredFeatures.get(1).equals("required4") || requiredFeatures.get(0).equals("required5"));
    assertEquals("required3", requiredFeatures.get(2));
    assertEquals("required2", requiredFeatures.get(3));
    assertEquals("required1", requiredFeatures.get(4));
    // make sure we do the registry.getFeatures only once
    assertTrue(requiredFeatures == gadget.getAllFeatures());
  }


  private static class DummyContext extends GadgetContext {
    public String view = super.getView();
    public String container = super.getContainer();

    protected DummyContext() {
    }

    @Override
    public String getView() {
      return view;
    }

    @Override
    public String getContainer() {
      return container;
    }
  }
}

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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.View;
import org.junit.Test;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

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

  @Test
  public void testGetFeatures() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"hello\">" +
                 "<Require feature=\"required1\"/>" +
                 "</ModulePrefs>" +
                 "<Content type=\"html\"/>" +
                 "</Module>";
    FeatureRegistry registry = mock(FeatureRegistry.class, true);
    Gadget gadget = new Gadget()
        .setContext(context)
        .setGadgetFeatureRegistry(registry)
        .setSpec(new GadgetSpec(Uri.parse(SPEC_URL), xml));
    Collection<String> needed = Lists.newArrayList(gadget.getSpec().getModulePrefs().getFeatures().keySet());
    List<String> returned = Lists.newArrayList(needed);
    // Call should only happen once, and be cached from there on out.
    expect(registry.getFeatures(eq(needed))).andReturn(returned).anyTimes();
    replay();
    List<String> requiredFeatures1 = gadget.getAllFeatures();
    assertEquals(returned, requiredFeatures1);
    List<String> requiredFeatures2 = gadget.getAllFeatures();
    assertSame(returned, requiredFeatures2);
    verify();
  }

  @Test
  public void testGetView1Features() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"hello\">" +
                 "<Require feature=\"required1\"/>" +
                 "<Require feature=\"requiredview1\" views=\"default\"/>" +
                 "<Require feature=\"requiredview2\" views=\"view2\"/>" +
                 "</ModulePrefs>" +
                 "<Content views=\"view1, default\" type=\"html\"/>" +
                 "<Content views=\"view2\" type=\"html\"/>" +
                 "</Module>";
    FeatureRegistry registry = mock(FeatureRegistry.class, true);
    Gadget gadget = new Gadget()
    		.setContext(context)
        .setGadgetFeatureRegistry(registry)
        .setSpec(new GadgetSpec(Uri.parse(SPEC_URL), xml));
    Collection<String> needed = Lists.newArrayList(gadget.getSpec().getModulePrefs().getViewFeatures(GadgetSpec.DEFAULT_VIEW).keySet());
    List<String> returned = Lists.newArrayList(needed);
    // Call should only happen once, and be cached from there on out.
    expect(registry.getFeatures(eq(needed))).andReturn(returned).anyTimes();
    replay();
    List<String> requiredFeatures = Lists.newArrayList(gadget.getViewFeatures().keySet());
    assertEquals(returned, requiredFeatures);
    assertTrue(requiredFeatures.contains("requiredview1"));
    assertTrue(requiredFeatures.contains("core"));
    assertTrue(!requiredFeatures.contains("requiredview2"));

    verify();
  }

  @Test
  public void testGetView2Features() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"hello\">" +
                 "<Require feature=\"required\"/>" +
                 "<Require feature=\"requiredview1\" views=\"default\"/>" +
                 "<Require feature=\"requiredview2\" views=\"view2\"/>" +
                 "</ModulePrefs>" +
                 "<Content views=\"view1, default\" type=\"html\"/>" +
                 "<Content views=\"view2\" type=\"html\"/>" +
                 "</Module>";
    FeatureRegistry registry = mock(FeatureRegistry.class, true);
    Gadget gadget = new Gadget()
        .setContext(context)
        .setGadgetFeatureRegistry(registry)
        .setSpec(new GadgetSpec(Uri.parse(SPEC_URL), xml));
    List<Element> viewEles = Lists.newArrayList();
    gadget.setCurrentView(new View("view2", viewEles, null));
    Collection<String> needed = Lists.newArrayList(gadget.getSpec().getModulePrefs().getViewFeatures("view2").keySet());
    List<String> returned = Lists.newArrayList(needed);
    // Call should only happen once, and be cached from there on out.
    expect(registry.getFeatures(eq(needed))).andReturn(returned).anyTimes();
    replay();
    List<String> requiredFeatures = Lists.newArrayList(gadget.getViewFeatures().keySet());
    assertEquals(returned, requiredFeatures);
    assertEquals(3, requiredFeatures.size());
    assertTrue(!requiredFeatures.contains("requiredview1"));
    assertTrue(requiredFeatures.contains("required"));
    assertTrue(requiredFeatures.contains("core"));
    assertTrue(requiredFeatures.contains("requiredview2"));

    verify();
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

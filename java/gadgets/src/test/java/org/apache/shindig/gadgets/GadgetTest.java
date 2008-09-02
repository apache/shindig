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
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.Preload;
import org.apache.shindig.gadgets.spec.View;

import org.easymock.classextension.EasyMock;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Tests for Gadget
 */
public class GadgetTest {
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

  private final ContainerConfig config
      = EasyMock.createNiceMock(ContainerConfig.class);
  private final DummyContext context = new DummyContext();

  private GadgetSpec spec;
  private Gadget gadget;
  private List<JsLibrary> libraries;

  @Before
  public void setUp() throws Exception {
    JsLibrary lib
        = JsLibrary.create(JsLibrary.Type.INLINE, "var foo='bar';", "core", null);

    spec = new GadgetSpec(URI.create(SPEC_URL), SPEC_XML);
    libraries = Arrays.asList(lib);
    gadget = new Gadget(context, spec, libraries, config);
  }

  @Test
  public void getView() {
    context.view = "two";

    replay(config);
    View view = gadget.getView(config);

    assertEquals("VIEW TWO", view.getContent());
  }

  @Test
  public void getDefaultView() {
    context.view = "unknown";

    replay(config);
    View view = gadget.getView(config);

    assertEquals("DEFAULT VIEW", view.getContent());
  }

  @Test
  public void getAliasedView() {
    context.view = "unknown";
    context.container = "foo";

    String aliasStr = "gadgets.features/views/unknown/aliases";
    JSONArray aliases = new JSONArray().put("blah").put("one");

    expect(config.getJsonArray(context.container, aliasStr)).andReturn(aliases);
    replay(config);

    View view = gadget.getView(config);

    assertEquals("VIEW ONE", view.getContent());
  }

  @Test
  public void getLocale() {
    LocaleSpec localeSpec = gadget.getLocale();
    assertEquals("VALUE", localeSpec.getMessageBundle().getMessages().get("name"));
  }

  @Test
  public void contextIsAPassThrough() {
    assertEquals(context, gadget.getContext());
  }

  @Test
  public void getJsLibrariesNotAltered() {
    assertEquals(libraries, gadget.getJsLibraries());
  }

  @Test
  public void getSpecNotAltered() {
    assertEquals(spec.toString(), gadget.getSpec().toString());
  }

  @Test
  public void preloadMapIsJustADummyMap() throws Exception {
    HttpResponse response = HttpResponse.error();

    Preload preload = spec.getModulePrefs().getPreloads().get(0);

    gadget.getPreloadMap().put(preload, new DummyFuture<HttpResponse>(response));

    assertEquals(response, gadget.getPreloadMap().get(preload).get());
  }

  private static class DummyContext extends GadgetContext {
    public String view = super.getView();
    public String container = super.getContainer();

    @Override
    public String getView() {
      return view;
    }

    @Override
    public String getContainer() {
      return container;
    }
  }

  private static class DummyFuture<T> implements Future<T> {

    private final T value;

    public T get() {
      return value;
    }

    public T get(long timeout, TimeUnit unit) {
      return value;
    }

    public boolean isDone() {
      return true;
    }

    public boolean isCancelled() {
      return false;
    }

    public boolean cancel(boolean ignore) {
      return false;
    }

    public DummyFuture(T value) {
      this.value = value;
    }
  }
}

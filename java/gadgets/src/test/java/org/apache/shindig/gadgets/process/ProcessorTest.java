/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.ContainerConfigException;
import org.apache.shindig.common.JsonContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetBlacklist;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.variables.VariableSubstituter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;

public class ProcessorTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final Uri TYPE_URL_HREF = Uri.parse("http://example.org/gadget.php");
  private static final String BASIC_HTML_CONTENT = "Hello, World!";
  protected static final String GADGET =
      "<Module>" +
      " <ModulePrefs title='foo'/>" +
      " <Content view='html' type='html'>" + BASIC_HTML_CONTENT + "</Content>" +
      " <Content view='url' type='url' href='" + TYPE_URL_HREF + "'/>" +
      " <Content view='alias' type='html'>" + BASIC_HTML_CONTENT + "</Content>" +
      "</Module>";

  private final FakeGadgetSpecFactory gadgetSpecFactory = new FakeGadgetSpecFactory();
  private final FakeVariableSubstituter substituter = new FakeVariableSubstituter();
  private final FakeBlacklist blacklist = new FakeBlacklist();

  private FakeContainerConfig containerConfig;
  private Processor processor;

  @Before
  public void setUp() throws Exception {
    containerConfig = new FakeContainerConfig();
    processor = new Processor(gadgetSpecFactory, substituter, containerConfig, blacklist);
  }

  private GadgetContext makeContext(final String view, final Uri specUrl) {
    return new GadgetContext() {
      @Override
      public URI getUrl() {
        if (specUrl == null) {
          return null;
        }
        return specUrl.toJavaUri();
      }

      @Override
      public String getView() {
        return view;
      }
    };
  }

  private GadgetContext makeContext(final String view) {
    return makeContext(view, SPEC_URL);
  }

  @Test
  public void normalProcessing() throws Exception {
    Gadget gadget = processor.process(makeContext("html"));
    assertEquals(BASIC_HTML_CONTENT, gadget.getCurrentView().getContent());
  }

  @Test(expected = ProcessingException.class)
  public void handlesGadgetExceptionGracefully() throws Exception {
    gadgetSpecFactory.exception = new GadgetException(GadgetException.Code.INVALID_PATH);
    processor.process(makeContext("url"));
  }

  @Test
  public void doViewAliasing() throws Exception {
    JSONArray aliases = new JSONArray(Arrays.asList("some-alias", "alias"));
    containerConfig.json.put("gadgets.features/views/aliased/aliases", aliases);
    Gadget gadget = processor.process(makeContext("aliased"));
    assertEquals(BASIC_HTML_CONTENT, gadget.getCurrentView().getContent());
  }

  @Test
  public void noSupportedViewHasNoCurrentView() throws Exception {
    Gadget gadget = processor.process(makeContext("not-real-view"));
    assertNull(gadget.getCurrentView());
  }

  @Test
  public void substitutionsPerformedTypeHtml() throws Exception {
    processor.process(makeContext("html"));
    assertTrue("Substitutions not performed", substituter.wasSubstituted);
  }

  @Test
  public void substitutionsPerformedTypeUrl() throws Exception {
    processor.process(makeContext("url"));
    assertTrue("Substitutions not performed", substituter.wasSubstituted);
  }

  @Test
  public void blacklistChecked() throws Exception {
    processor.process(makeContext("url"));
    assertTrue("Blacklist not checked", blacklist.wasChecked);
  }

  @Test(expected = ProcessingException.class)
  public void blacklistedGadgetThrows() throws Exception {
    blacklist.isBlacklisted = true;
    processor.process(makeContext("html"));
  }

  @Test(expected = ProcessingException.class)
  public void nullUrlThrows() throws ProcessingException {
    processor.process(makeContext("html", null));
  }

  @Test(expected = ProcessingException.class)
  public void nonHttpOrHttpsThrows() throws ProcessingException {
    processor.process(makeContext("html", Uri.parse("file://foo")));
  }

  private static class FakeBlacklist implements GadgetBlacklist {
    protected boolean wasChecked;
    protected boolean isBlacklisted;

    protected FakeBlacklist() {
    }

    public boolean isBlacklisted(URI gadgetUri) {
      wasChecked = true;
      return isBlacklisted;
    }
  }

  private static class FakeContainerConfig extends JsonContainerConfig {
    protected final JSONObject json = new JSONObject();

    public FakeContainerConfig() throws ContainerConfigException {
      super(null);
    }

    @Override
    public Object getJson(String container, String parameter) {
      return json.opt(parameter);
    }
  }

  private static class FakeGadgetSpecFactory implements GadgetSpecFactory {
    protected GadgetException exception;

    protected FakeGadgetSpecFactory() {
    }

    public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
      if (exception != null) {
        throw exception;
      }
      return new GadgetSpec(Uri.fromJavaUri(context.getUrl()), GADGET);
    }

    public GadgetSpec getGadgetSpec(URI uri, boolean ignoreCache) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FakeVariableSubstituter extends VariableSubstituter {
    protected boolean wasSubstituted;

    protected FakeVariableSubstituter() {
      super(null);
    }

    @Override
    public GadgetSpec substitute(GadgetContext context, GadgetSpec spec) {
      wasSubstituted = true;
      return spec;
    }
  }
}



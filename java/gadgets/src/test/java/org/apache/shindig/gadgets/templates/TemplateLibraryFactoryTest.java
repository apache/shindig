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
package org.apache.shindig.gadgets.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.templates.tags.TagHandler;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableMap;

public class TemplateLibraryFactoryTest {

  public static final Uri SPEC_URL = Uri.parse("http://www.example.org/dir/g.xml");
  public static final Uri TEMPLATE_URL = Uri.parse("http://www.example.org/dir/template.xml");
  public static final String NAMESPACE_URI = "#my";
  private static final String TEMPLATE_LIBRARY =
          "<Templates xmlns:my='#my'>" + '\n' +
          "  <Namespace prefix='my' url='#my'/>" + '\n' +
          "  <JavaScript>script;\nscript2</JavaScript>" + '\n' +
          "  <Style>style\nstyle2</Style>" + '\n' +
          "  <Template tag='my:Tag1'>external1</Template>" + '\n' +
          "  <Template tag='my:Tag2'>external2</Template>" + '\n' +
          "  <Template tag='my:Tag3'>external3</Template>" + '\n' +
          "  <Template tag='my:Tag4'>external4</Template>" + '\n' +
          "</Templates>";

  @Test
  public void testTemplateRequestAnonymousSecurityToken() throws GadgetException {
    CapturingPipeline pipeline = new CapturingPipeline();
    TemplateLibraryFactory factory = new TemplateLibraryFactory( pipeline, null );
    GadgetContext context = new GadgetContext() {
      @Override
      public Uri getUrl() {
        return SPEC_URL;
      }

      @Override
      public String getContainer() {
        return "default";
      }

      @Override
      public boolean getDebug() {
        return false;
      }

      @Override
      public boolean getIgnoreCache() {
        return true;
      }
    };

    factory.loadTemplateLibrary(context, TEMPLATE_URL);
    assertNotNull( pipeline.request );
    SecurityToken st = pipeline.request.getSecurityToken();
    assertNotNull( st );
    assertTrue( st.isAnonymous() );
    assertEquals( SPEC_URL.toString(), st.getAppUrl() );
    assertTrue(pipeline.request.getIgnoreCache());
  }

  @Test
  public void testTemplateLibraryRewrite() throws GadgetException, XmlException {
    CapturingPipeline pipeline = new CapturingPipeline();
    TemplateLibraryFactory factory = new TemplateLibraryFactory( pipeline, null );
    GadgetContext context = new GadgetContext() {
      @Override
      public Uri getUrl() {
        return SPEC_URL;
      }

      @Override
      public String getContainer() {
        return "default";
      }

      @Override
      public boolean getDebug() {
        return false;
      }

      @Override
      public boolean getIgnoreCache() {
        return true;
      }
    };

    TemplateLibrary library = factory.loadTemplateLibrary(context, TEMPLATE_URL);
    TagRegistry registry = library.getTagRegistry();

    assertNotNull(registry.getHandlerFor(new TagRegistry.NSName(NAMESPACE_URI, "Tag1")));
    assertNotNull(registry.getHandlerFor(new TagRegistry.NSName(NAMESPACE_URI, "Tag2")));
    assertNotNull(registry.getHandlerFor(new TagRegistry.NSName(NAMESPACE_URI, "Tag3")));
    assertNotNull(registry.getHandlerFor(new TagRegistry.NSName(NAMESPACE_URI, "Tag4")));

    TagHandler handler = registry.getHandlerFor(new TagRegistry.NSName(NAMESPACE_URI, "Tag1"));
    Element doc = XmlUtil.parse(TEMPLATE_LIBRARY);
    Node result = doc.getOwnerDocument().createDocumentFragment();
    Element tag = doc.getOwnerDocument().createElement("test");
    final TemplateContext templateContext = new TemplateContext(null, ImmutableMap.<String, Object>of());
    TemplateProcessor processor = new DefaultTemplateProcessor(Expressions.forTesting()) {
      @Override
      public TemplateContext getTemplateContext() {
        return templateContext;
      }
    };
    handler.process(result, tag, processor);

    assertEquals("<STYLE>stylestyle2</STYLE>" +
                 "<JAVASCRIPT>script;script2</JAVASCRIPT>", serializeResources(templateContext));
  }

  private String serializeResources(TemplateContext context) {
    StringBuilder builder = new StringBuilder();
    for (TemplateResource resource : context.getResources()) {
      builder.append(resource);
    }

    return builder.toString();
  }

  private static class CapturingPipeline implements RequestPipeline {
    HttpRequest request;

    public HttpResponse execute(HttpRequest request) {
      this.request = request;
      return new HttpResponseBuilder().setHttpStatusCode( HttpResponse.SC_OK ).setResponseString( TEMPLATE_LIBRARY ).create();
    }
  }

}


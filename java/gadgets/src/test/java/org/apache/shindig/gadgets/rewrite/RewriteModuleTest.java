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
package org.apache.shindig.gadgets.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.admin.GadgetAdminModule;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.oauth2.OAuth2Module;
import org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerModule;
import org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2PersistenceModule;
import org.apache.shindig.gadgets.oauth2.OAuth2MessageModule;
import org.apache.shindig.gadgets.render.CajaResponseRewriter;
import org.apache.shindig.gadgets.render.SanitizingResponseRewriter;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterList.RewriteFlow;
import org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter;
import org.apache.shindig.gadgets.uri.AccelUriManager;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Tests for RewriteModule. Tests the flows and the associated rewriters.
 */
public class RewriteModuleTest {
  Injector injector;

  public static class TestClass {
    public final ResponseRewriterRegistry defaultPipelineRegistry;
    public final ResponseRewriterRegistry requestPipelineRegistry;
    public final ResponseRewriterRegistry accelPipelineRegistry;

    @Inject
    public TestClass(@RewriterRegistry(rewriteFlow = RewriteFlow.REQUEST_PIPELINE)
                     ResponseRewriterRegistry requestPipelineRegistry,
                     @RewriterRegistry(rewriteFlow = RewriteFlow.DEFAULT)
                     ResponseRewriterRegistry defaultPipelineRegistry,
                     @RewriterRegistry(rewriteFlow = RewriteFlow.ACCELERATE)
                     ResponseRewriterRegistry accelPipelineRegistry) {
      this.defaultPipelineRegistry = defaultPipelineRegistry;
      this.requestPipelineRegistry = requestPipelineRegistry;
      this.accelPipelineRegistry = accelPipelineRegistry;
    }
  }

  @Before
  public void setUp() {
    injector = Guice.createInjector(
        new PropertiesModule(),
        new GadgetAdminModule(),
        new DefaultGuiceModule(), new OAuthModule(), new OAuth2Module(), new OAuth2PersistenceModule(), new OAuth2MessageModule(), new OAuth2HandlerModule());
  }

  @Test
  public void testDefaultRewriters() throws Exception {
    ContextAwareRegistry defaultPipelineRegistry = (ContextAwareRegistry)
        injector.getInstance(TestClass.class).defaultPipelineRegistry;

    List<ResponseRewriter> list = defaultPipelineRegistry.getResponseRewriters(
        ContainerConfig.DEFAULT_CONTAINER);
    assertEquals(7, list.size());
    assertTrue(list.get(0) instanceof AbsolutePathReferenceRewriter);
    assertTrue(list.get(1) instanceof StyleTagExtractorContentRewriter);
    assertTrue(list.get(2) instanceof StyleAdjacencyContentRewriter);
    assertTrue(list.get(3) instanceof ProxyingContentRewriter);
    assertTrue(list.get(4) instanceof CssResponseRewriter);
    assertTrue(list.get(5) instanceof SanitizingResponseRewriter);
    assertTrue(list.get(6) instanceof CajaResponseRewriter);

    list = defaultPipelineRegistry.getResponseRewriters(AccelUriManager.CONTAINER);
    assertEquals(3, list.size());
    assertTrue(list.get(0) instanceof AbsolutePathReferenceRewriter);
    assertTrue(list.get(1) instanceof StyleTagProxyEmbeddedUrlsRewriter);
    assertTrue(list.get(2) instanceof ProxyingContentRewriter);
  }

  @Test
  public void testRequestPipelineRewriters() throws Exception {
    ContextAwareRegistry requestPipelineRegistry = (ContextAwareRegistry)
        injector.getInstance(TestClass.class).requestPipelineRegistry;

    List<ResponseRewriter> list = requestPipelineRegistry.getResponseRewriters(
        ContainerConfig.DEFAULT_CONTAINER);
    assertEquals(1, list.size());
    assertTrue(list.get(0) instanceof BasicImageRewriter);

    list = requestPipelineRegistry.getResponseRewriters(AccelUriManager.CONTAINER);
    assertEquals(1, list.size());
    assertTrue(list.get(0) instanceof BasicImageRewriter);
  }

  @Test
  public void testAccelRewriters() throws Exception {
    ContextAwareRegistry accelPipelineRegistry = (ContextAwareRegistry)
        injector.getInstance(TestClass.class).accelPipelineRegistry;

    List<ResponseRewriter> list = accelPipelineRegistry.getResponseRewriters(
        AccelUriManager.CONTAINER);
    assertEquals(3, list.size());
    assertTrue(list.get(0) instanceof AbsolutePathReferenceRewriter);
    assertTrue(list.get(1) instanceof StyleTagProxyEmbeddedUrlsRewriter);
    assertTrue(list.get(2) instanceof ProxyingContentRewriter);

    list = accelPipelineRegistry.getResponseRewriters(ContainerConfig.DEFAULT_CONTAINER);
    assertEquals(0, list.size());
  }
}

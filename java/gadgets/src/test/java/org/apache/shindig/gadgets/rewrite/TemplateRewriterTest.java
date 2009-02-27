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

import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.SocialMarkupHtmlParser;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.apache.shindig.gadgets.templates.DefaultTemplateProcessor;
import org.apache.shindig.gadgets.templates.TagHandler;
import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;

import java.util.Set;

/** 
 * Tests for TemplateRewriter
 */
public class TemplateRewriterTest {
  
  private GadgetSpec gadgetSpec;
  private Gadget gadget;
  private MutableContent content;
  private TemplateRewriter rewriter;
  
  private static final Uri GADGET_URI = Uri.parse("http://example.org/gadget.php");
  
  private static final String CONTENT_PLAIN =
    "<script type='text/os-template'>Hello, ${user.name}</script>";  

  private static final String CONTENT_REQUIRE =
    "<script type='text/os-template' require='user'>Hello, ${user.name}</script>";  
  
  private static final String CONTENT_REQUIRE_MISSING =
    "<script type='text/os-template' require='foo'>Hello, ${user.name}</script>";  

  private static final String CONTENT_WITH_NAME =
    "<script type='text/os-template' name='myTemplate'>Hello, ${user.name}</script>";  
  
  private static final String CONTENT_WITH_TAG =
    "<script type='text/os-template' tag='foo:Bar'>Hello, ${user.name}</script>";  

  
  @Before
  public void setUp() {
    rewriter = new TemplateRewriter(
        new Provider<TemplateProcessor>() {
          public TemplateProcessor get() {
            Set<TagHandler> handlers = ImmutableSet.of();
            return new DefaultTemplateProcessor(Expressions.sharedInstance(), 
                new TagRegistry(handlers));
          }
        });
  }
  
  @Test
  public void simpleTemplate() throws Exception {
    // Render a simple template
    testExpectingTransform(getGadgetXml(CONTENT_PLAIN), "simple");
  }
  
  @Test
  public void noTemplateFeature() throws Exception {
    // Without opensocial-templates feature, shouldn't render
    testExpectingNoTransform(getGadgetXml(CONTENT_PLAIN, false), "no feature");
  }
  
  @Test
  public void requiredDataPresent() throws Exception {
    // Required data is present - render 
    testExpectingTransform(getGadgetXml(CONTENT_REQUIRE), "required data");
  }
  
  @Test
  public void requiredDataMissing() throws Exception {
    // Required data is missing - don't render
    testExpectingNoTransform(getGadgetXml(CONTENT_REQUIRE_MISSING), "missing data");
  }
  
  @Test
  public void nameAttributePresent() throws Exception {
    // Don't render templates with a @name
    testExpectingNoTransform(getGadgetXml(CONTENT_WITH_NAME), "with @name");
  }
  
  @Test
  public void tagAttributePresent() throws Exception {
    // Don't render templates with a @tag
    testExpectingNoTransform(getGadgetXml(CONTENT_WITH_TAG), "with @tag");
  }
   
  private void testExpectingTransform(String code, String condition) throws Exception {
    setupGadget(code);
    rewriter.rewrite(gadget, content);
    assertTrue("Template wasn't transformed (" + condition + ")", 
        content.getContent().indexOf("Hello, John") > 0);
    assertTrue("Template tag wasn't removed (" + condition + ")",
        !content.getContent().contains("text/os-template"));
  }

  private void testExpectingNoTransform(String code, String condition) throws Exception {
    setupGadget(code);
    rewriter.rewrite(gadget, content);
    assertTrue("Template was transformed (" + condition + ")", 
        content.getContent().indexOf("${user.name}") > 0);
    assertTrue("Template tag was removed (" + condition + ")", 
        content.getContent().indexOf("text/os-template") > 0);
  }
  
  private void setupGadget(String gadgetXml) throws SpecParserException, JSONException {
    gadgetSpec = new GadgetSpec(GADGET_URI, gadgetXml);
    gadget = new Gadget();
    gadget.setSpec(gadgetSpec);
    gadget.setContext(new GadgetContext() {});
    gadget.setCurrentView(gadgetSpec.getView("default"));

    content = new MutableContent(new SocialMarkupHtmlParser(
        new ParseModule.DOMImplementationProvider().get()), gadget.getCurrentView().getContent());
    putPipelinedData("user", new JSONObject("{ name: 'John'}"));
  }
  
  private void putPipelinedData(String key, JSONObject data) {
    content.addPipelinedData(key, data);
  }
  
  private static String getGadgetXml(String content) {
    return getGadgetXml(content, true);
  }
  
  private static String getGadgetXml(String content, boolean requireFeature) {
    String feature = requireFeature ?
        "<Require feature='opensocial-templates'>" +
        "  <Param name='" + TemplateRewriter.SERVER_TEMPLATING_PARAM + "'>true</Param>" +
        "</Require>" : "";
    return "<Module>" + "<ModulePrefs title='Title'>"
        + feature + "</ModulePrefs>"
        + "<Content>"
        + "    <![CDATA[" + content + "]]>"
        + "</Content></Module>";
  }
}

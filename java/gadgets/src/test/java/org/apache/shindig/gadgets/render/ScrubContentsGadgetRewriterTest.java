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

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableSet;
import com.google.inject.util.Providers;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewriterTestBase;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.uri.PassthruManager;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Tests for {@link ScrubContentsGadgetRewriter}.
 */
public class ScrubContentsGadgetRewriterTest extends RewriterTestBase {

  private static final ImmutableSet<String> ALLOWED_TAGS = ImmutableSet.of("html", "head", "body", "b");
  private static final Pattern BODY_REGEX = Pattern.compile(".*<body>(.+)</body>.*");

  @Override
  protected Class<? extends GadgetHtmlParser> getParserClass() {
    return CajaHtmlParser.class;
  }
  
  private String rewrite(final boolean sanitize, String markup) throws Exception {
    ContentRewriterFeature.Factory rewriterFeatureFactory =
      new ContentRewriterFeature.Factory(null,
        Providers.of(new ContentRewriterFeature.DefaultConfig(
          ".*", "", "HTTP", "embed,img,script,link,style", false, false, false)));
  
    ScrubContentsGadgetRewriter rewriter = new ScrubContentsGadgetRewriter(
        ALLOWED_TAGS, ImmutableSet.<String>of(), rewriterFeatureFactory,
        new CajaCssSanitizer(new CajaCssParser()), new PassthruManager("host.com", "/proxy"));
    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        return Param.SANITIZE.getKey().equals(name) && sanitize ? "1" : null;
      }
      
      @Override
      public String getContainer() {
        return "mockContainer";
      }
    };
    Gadget gadget =
        new Gadget().setContext(context).setSpec(
            new GadgetSpec(Uri.parse("http://www.example.org/gadget.xml"),
                "<Module><ModulePrefs title=''/><Content type='x-html-sanitized'/></Module>"));
    
    MutableContent content = new MutableContent(parser, markup);
    rewriter.rewrite(gadget, content);
    
    Matcher matcher = BODY_REGEX.matcher(content.getContent());
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return content.getContent();
  }
  
  @Test
  public void testCommentsUntouchedWithoutSanitize() throws Exception {
    String markup = "<b>Good<!-- bad --></b>";
    assertEquals(markup, rewrite(false, markup));    
  }
  
  @Test
  public void testCommentsDroppedWithSanitize() throws Exception {
    String markup = "<b>Good<!-- bad --></b>";
    assertEquals("<b>Good</b>", rewrite(true, markup));
  }
  
  @Test
  public void testCommentsInMidTagDroppedWithSanitize() throws Exception {
    String markup = "<<!-- -->b>Good<<!-- -->/b>";
    assertEquals("<b>Good</b>", rewrite(true, markup));    
  }
  
  @Test
  public void testSlightlyExoticCommentsAlsoDropped() throws Exception {
    String markup = "<b>Good<!-- bad -- \n ></b>";
    assertEquals("<b>Good</b>", rewrite(true, markup));
  }
  
  @Test
  public void testNestedCommentsAreAlsoRemoved() throws Exception {
    String markup = "<<<!-- -->!-- -->b>Good<<<!-- -->!-- -->/b>";
    assertEquals("<b>Good</b>", rewrite(true, markup));        
  }
  
  @Test
  public void testEvilStuffInsideTagIsRemoved() throws Exception {
    String markup = "<b>Good</b><<evil></evil>evil>Evil<<evil></evil>/evil>";
    assertEquals("<b>Good</b>", rewrite(true, markup));
  }
}

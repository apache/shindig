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
package org.apache.shindig.gadgets.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.parse.caja.CajaCssLexerParser;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 *
 */
public class HTMLContentRewriterTest extends BaseRewriterTestCase {
  private HTMLContentRewriter rewriter;
  private HTMLContentRewriter rewriterNoOverrideExpires;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    ContentRewriterFeature overrideFeatureNoOverrideExpires = rewriterFeatureFactory
        .get(createSpecWithRewrite(".*", ".*exclude.*", null,
            HTMLContentRewriter.TAGS));
    ContentRewriterFeatureFactory factoryNoOverrideExpires = mockContentRewriterFeatureFactory(overrideFeatureNoOverrideExpires);

    rewriterNoOverrideExpires = new HTMLContentRewriter(
        factoryNoOverrideExpires, new CssRequestRewriter(
            factoryNoOverrideExpires, new CajaCssLexerParser(),
            new DefaultProxyingLinkRewriterFactory(rewriterUris)),
        new DefaultConcatLinkRewriterFactory(rewriterUris),
        new DefaultProxyingLinkRewriterFactory(rewriterUris));
    
    ContentRewriterFeature overrideFeature = rewriterFeatureFactory
        .get(createSpecWithRewrite(".*", ".*exclude.*", "3600",
            HTMLContentRewriter.TAGS));
    ContentRewriterFeatureFactory factory = mockContentRewriterFeatureFactory(overrideFeature);

    rewriter = new HTMLContentRewriter(factory, new CssRequestRewriter(factory,
        new CajaCssLexerParser(), new DefaultProxyingLinkRewriterFactory(
            rewriterUris)), new DefaultConcatLinkRewriterFactory(rewriterUris),
        new DefaultProxyingLinkRewriterFactory(rewriterUris));
  }

  @Test
  public void testScriptsBasic() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritescriptbasic.html"));
    Document doc = rewriteContent(rewriter, content, null).getDocument();

    XPathWrapper wrapper = new XPathWrapper(doc);

    // Head should contain 1 script tag
    assertEquals("headScript1", wrapper.getValue("/html/head/script"));
    assertEquals(1, wrapper.getNodeList("/html/head/script").getLength());

    // Body should contain 11 script tags after rewrite
    assertEquals(11, wrapper.getNodeList("/html/body/script").getLength());

    assertEquals("bodyScript1", wrapper.getValue("/html/body/script[1]"));

    // Second script should contain two concatenated urls
    assertEquals(wrapper.getValue("/html/body/script[2]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F1.js" +
            "&2=http%3A%2F%2Fwww.example.org%2F2.js");

    assertEquals("bodyScript2", wrapper.getValue("/html/body/script[3]"));

    // Fourth script should contain one concatenated url
    assertEquals(wrapper.getValue("/html/body/script[4]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F3.js");

    // Fifth script should contain a retained comment
    assertEquals("<!-- retain-comment -->",
        wrapper.getValue("/html/body/script[5]"));

    // An excluded URL between contiguous tags prevents them being concatentated
    assertEquals(wrapper.getValue("/html/body/script[6]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F4.js");

    // Excluded URL is untouched
    assertEquals("http://www.example.org/excluded/5.js",
        wrapper.getValue("/html/body/script[7]/@src"));

    assertEquals(wrapper.getValue("/html/body/script[8]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F6.js" +
            "&2=http%3A%2F%2Fwww.example.org%2F10.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&3=http%3A%2F%2Fwww.example.org%2F11.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&4=http%3A%2F%2Fwww.example.org%2F12.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&5=http%3A%2F%2Fwww.example.org%2F13.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&6=http%3A%2F%2Fwww.example.org%2F14.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&7=http%3A%2F%2Fwww.example.org%2F15.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&8=http%3A%2F%2Fwww.example.org%2F16.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&9=http%3A%2F%2Fwww.example.org%2F17.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&10=http%3A%2F%2Fwww.example.org%2F18.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&11=http%3A%2F%2Fwww.example.org%2F19.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");

    // Did it split the request
    assertEquals(wrapper.getValue("/html/body/script[9]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F20.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&2=http%3A%2F%2Fwww.example.org%2F21.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&3=http%3A%2F%2Fwww.example.org%2F22.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&4=http%3A%2F%2Fwww.example.org%2F23.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
    
    // Handle long requests
    assertEquals(wrapper.getValue("/html/body/script[10]/@src"),
        "http://www.example.org/23-long.js&0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");

    // Resume concating
    assertEquals(wrapper.getValue("/html/body/script[11]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F24.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&2=http%3A%2F%2Fwww.example.org%2F25.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
  }
  
  @Test
  public void testScriptsBasicNoCache() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritescriptbasic.html"));
    Document doc = rewriteContent(rewriter, content, null, false, true).getDocument();

    XPathWrapper wrapper = new XPathWrapper(doc);

    // Second script should contain two concatenated urls with nocache
    assertEquals(wrapper.getValue("/html/body/script[2]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&nocache=1&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F1.js" +
            "&2=http%3A%2F%2Fwww.example.org%2F2.js");

    // And check the last script
    assertEquals(wrapper.getValue("/html/body/script[11]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&nocache=1&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F24.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&2=http%3A%2F%2Fwww.example.org%2F25.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
  }

  @Test
  public void testScriptsBasicNoCacheAndDebug() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritescriptbasic.html"));
    Document doc = rewriteContent(rewriter, content, null, true, true).getDocument();

    XPathWrapper wrapper = new XPathWrapper(doc);

    // Second script should contain two concatenated urls with nocache and debug
    assertEquals(wrapper.getValue("/html/body/script[2]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&debug=1&nocache=1&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F1.js" +
            "&2=http%3A%2F%2Fwww.example.org%2F2.js");

    // and check the last script
    assertEquals(wrapper.getValue("/html/body/script[11]/@src"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/javascript&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml" +
            "&fp=1150739864&debug=1&nocache=1&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2F24.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "&2=http%3A%2F%2Fwww.example.org%2F25.js%260123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
  }
  
  @Test
  public void testScriptsForContainer() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritescriptbasic.html"));
    String text = rewriteContent(rewriter, content, MOCK_CONTAINER).getContent();
    // Verify that the correct concat base was used
    assertFalse(text.contains(DEFAULT_CONCAT_BASE));
    assertTrue(text.contains(MOCK_CONCAT_BASE));
  }
  
  @Test
  public void testLinksBasic() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritelinksbasic.html"));
    Document doc = rewriteContent(rewriter, content, null).getDocument();

    XPathWrapper wrapper = new XPathWrapper(doc);

    // Image is rewritten to proxy, relative path is resolved
    assertEquals(wrapper.getValue("//img[1]/@src"),
        "http://www.test.com/dir/proxy?" +
            "url=http%3A%2F%2Fwww.example.org%2Fimg.gif" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&refresh=3600");

    // Excluded image is untouched
    assertEquals("http://www.example.org/excluded/img.gif", wrapper.getValue("//img[2]/@src"));

    // Embed target is rewritten to proxy
    assertEquals(wrapper.getValue("//embed[1]/@src"),
        "http://www.test.com/dir/proxy?" +
            "url=http%3A%2F%2Fwww.example.org%2Fsome.swf" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&refresh=3600");

    // Excluded embed is untouched
    assertEquals("http://www.example.org/excluded/some.swf", wrapper.getValue("//embed[2]/@src"));
  }

  @Test
  public void testLinksBasicNoOverrideExpires() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritelinksbasic.html"));
    Document doc = rewriteContent(rewriterNoOverrideExpires, content, null).getDocument();

    XPathWrapper wrapper = new XPathWrapper(doc);

    // Image is rewritten to proxy, relative path is resolved
    assertEquals(wrapper.getValue("//img[1]/@src"),
        "http://www.test.com/dir/proxy?" +
            "url=http%3A%2F%2Fwww.example.org%2Fimg.gif" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&refresh=86400");

    // Excluded image is untouched
    assertEquals("http://www.example.org/excluded/img.gif", wrapper.getValue("//img[2]/@src"));

    // Embed target is rewritten to proxy
    assertEquals(wrapper.getValue("//embed[1]/@src"),
        "http://www.test.com/dir/proxy?" +
            "url=http%3A%2F%2Fwww.example.org%2Fsome.swf" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&refresh=86400");

    // Excluded embed is untouched
    assertEquals("http://www.example.org/excluded/some.swf", wrapper.getValue("//embed[2]/@src"));
  }

  @Test
  public void testLinksBasicNoCache() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritelinksbasic.html"));
    Document doc = rewriteContent(rewriter, content, null, false, true).getDocument();

    XPathWrapper wrapper = new XPathWrapper(doc);

    // Image is rewritten to proxy, relative path is resolved
    assertEquals(wrapper.getValue("//img[1]/@src"),
        "http://www.test.com/dir/proxy?" +
            "url=http%3A%2F%2Fwww.example.org%2Fimg.gif" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&nocache=1&refresh=3600");

    // Excluded image is untouched
    assertEquals("http://www.example.org/excluded/img.gif", wrapper.getValue("//img[2]/@src"));

    // Embed target is rewritten to proxy
    assertEquals(wrapper.getValue("//embed[1]/@src"),
        "http://www.test.com/dir/proxy?" +
            "url=http%3A%2F%2Fwww.example.org%2Fsome.swf" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&nocache=1&refresh=3600");

    // Excluded embed is untouched
    assertEquals("http://www.example.org/excluded/some.swf", wrapper.getValue("//embed[2]/@src"));
  }

  @Test
  public void testLinksBasicNoCacheAndDebug() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritelinksbasic.html"));
    Document doc = rewriteContent(rewriter, content, null, true, true).getDocument();

    XPathWrapper wrapper = new XPathWrapper(doc);

    // Image is rewritten to proxy, relative path is resolved
    assertEquals(wrapper.getValue("//img[1]/@src"),
        "http://www.test.com/dir/proxy?" +
            "url=http%3A%2F%2Fwww.example.org%2Fimg.gif" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&debug=1&nocache=1&refresh=3600");

    // Excluded image is untouched
    assertEquals("http://www.example.org/excluded/img.gif", wrapper.getValue("//img[2]/@src"));

    // Embed target is rewritten to proxy
    assertEquals(wrapper.getValue("//embed[1]/@src"),
        "http://www.test.com/dir/proxy?" +
            "url=http%3A%2F%2Fwww.example.org%2Fsome.swf" +
            "&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&debug=1&nocache=1&refresh=3600");

    // Excluded embed is untouched
    assertEquals("http://www.example.org/excluded/some.swf", wrapper.getValue("//embed[2]/@src"));
  }

  @Test
  public void testLinksForContainer() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritelinksbasic.html"));
    String text = rewriteContent(rewriter, content, MOCK_CONTAINER).getContent();
    // Verify that the correct proxy base was used
    assertFalse(text.contains(DEFAULT_PROXY_BASE));
    assertTrue(text.contains(MOCK_PROXY_BASE));
  }
  
  @Test
  public void testStyleBasic() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritestylebasic.html"));
    MutableContent mc = rewriteContent(rewriter, content, null);
    Document doc = mc.getDocument();

    XPathWrapper wrapper = new XPathWrapper(doc);

    // ALL style links and @import targets are rewritten to concat
    // Note that relative URLs are fully resolved
    assertEquals(wrapper.getValue("//link[1]/@href"),
        "http://www.test.com/dir/concat?" +
            "rewriteMime=text/css&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=1150739864&refresh=3600" +
            "&1=http%3A%2F%2Fwww.example.org%2Flinkedstyle1.css" +
            "&2=http%3A%2F%2Fwww.example.org%2Flinkedstyle3.css" +
            "&3=http%3A%2F%2Fwww.example.org%2Fimportedstyle1.css" +
            "&4=http%3A%2F%2Fwww.example.org%2Fimportedstyle3.css" +
            "&5=http%3A%2F%2Fwww.example.org%2Fimportedstyle4.css");

    // Untouched link target
    assertEquals("http://www.example.org/excluded/linkedstyle2.css",
        wrapper.getValue("//link[2]/@href"));

    // Untouched @import taget converted to a link
    assertEquals("/excluded/importedstyle2.css",
        wrapper.getValue("//link[3]/@href"));

    // Body should contain 1 style element
    assertEquals(1, wrapper.getNodeList("//style").getLength());

    // All @imports are stripped
    assertEquals("div { color : black; }", wrapper.getValue("//style[1]").trim());
  }

  @Test
  public void testStyleBasic2() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritestyle2.html"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritestyle2-expected.html"));
    MutableContent mc = rewriteContent(rewriter, content, null);
    assertEquals(StringUtils.deleteWhitespace(mc.getContent()),
                 StringUtils.deleteWhitespace(expected));
  }

  @Test
  public void testStyleForContainer() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritestylebasic.html"));
    String text = rewriteContent(rewriter, content, MOCK_CONTAINER).getContent();
    // Verify that the correct concat base was used
    assertFalse(text.contains(DEFAULT_CONCAT_BASE));
    assertTrue(text.contains(MOCK_CONCAT_BASE));
  }

  @Test
  public void testNoRewriteUnknownMimeType() {
    MutableContent mc = control.createMock(MutableContent.class); 
    HttpRequest req = control.createMock(HttpRequest.class);
    EasyMock.expect(req.getRewriteMimeType()).andReturn("unknown");
    control.replay();
    assertFalse(rewriter.rewrite(req, fakeResponse, mc));
    control.verify();
  }
}

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

import static org.easymock.EasyMock.expect;
import org.easymock.classextension.EasyMock;

import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.GadgetHtmlNodeTest;
import org.apache.shindig.gadgets.parse.ParsedHtmlNode;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;

import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.LinkRewriter;
import org.apache.shindig.gadgets.rewrite.ParseTreeHtmlRewriter;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test the HTML rewriter foundation for basic operation
 */
public class ParseTreeHtmlRewriterTest extends BaseRewriterTestCase {

  private URI baseUri;
  private LinkRewriter pfxLinkRewriter;
  private LinkRewriter noOpLinkRewriter;
  private ContentRewriterFeature jsFeature;
  private ContentRewriterFeature styleFeature;
  private ContentRewriterFeature comboFeature;
  private GadgetSpec spec;
  private String concatBase;
  private static final String LINK_PREFIX = "px-";
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    baseUri = new URI("http://gadget.org/dir/gadget.xml");
    pfxLinkRewriter = new LinkRewriter() {
      public String rewrite(String uri, URI context) {
        // Just prefixes with LINK_PREFIX
        return LINK_PREFIX + uri;
      }
    };
    noOpLinkRewriter = new LinkRewriter() {
      public String rewrite(String uri, URI context) {
        return uri;
      }
    };
    jsFeature = makeFeature("script");
    styleFeature = makeFeature("style");
    comboFeature = makeFeature("script", "style");
    spec = EasyMock.createNiceMock(GadgetSpec.class);
    expect(spec.getUrl()).andReturn(baseUri).anyTimes();
    org.easymock.classextension.EasyMock.replay(spec);
    concatBase = new ParseTreeHtmlRewriter(null).getJsConcatBase(spec, jsFeature);
  }
  
  private ContentRewriterFeature makeFeature(String... includedTags) {
    ContentRewriterFeature rewriterFeature =
        EasyMock.createNiceMock(ContentRewriterFeature.class);
    Set<String> tags = new HashSet<String>();
    for (String tag : includedTags) {
      tags.add(tag);
    }
    expect(rewriterFeature.getIncludedTags()).andReturn(tags).anyTimes();
    expect(rewriterFeature.getFingerprint()).andReturn(-840722081).anyTimes();
    org.easymock.classextension.EasyMock.replay(rewriterFeature);
    return rewriterFeature;
  }

  private String rewriteHelper(String s, ParsedHtmlNode[] p,
      ContentRewriterFeature rf, LinkRewriter lw) throws Exception {
    GadgetHtmlParser parser = EasyMock.createNiceMock(GadgetHtmlParser.class);
    List<ParsedHtmlNode> expected = p != null ? Arrays.asList(p) : null;
    expect(parser.parse(s)).andReturn(expected).anyTimes();
    org.easymock.classextension.EasyMock.replay(parser);
    ParseTreeHtmlRewriter hr = new ParseTreeHtmlRewriter(parser);
    StringWriter sw = new StringWriter();
    hr.rewrite(s, baseUri, spec, rf, lw, sw);
    return sw.toString();
  }
  
  public void testPreserveJunk() throws Exception {
    String s = "<div id=notvalid name='horrorShow\" />\n"
        + "</br>\n"
        + "</div>";
    // Unparseable, with no ContentRewriterFeature assertions
    assertEquals(s, rewriteHelper(s, null, null, noOpLinkRewriter));
  }

  public void testPreserveScriptNoJSConcatNoLinkRewrite() throws Exception {
    String s = "<script src=\"http://a.b.com/1.js\"></script>"
        + "<script src=\"http://a.b.com/2.js\"></script>";
    String[][] script1attr = { { "src", "http://a.b.com/1.js" } };
    String[][] script2attr = { { "src", "http://a.b.com/2.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", script1attr, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", script2attr, null)
    };
    assertEquals(s, rewriteHelper(s, p, null, noOpLinkRewriter));
  }

  public void testPreserveCss() throws Exception {
    String s = "<html><style>body { background-color:#7f7f7f }</style></html>";
    ParsedHtmlNode[] styleKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("body { background-color:#7f7f7f }")
    };
    ParsedHtmlNode[] htmlKids = {
      GadgetHtmlNodeTest.makeParsedTagNode("style", null, styleKids)
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("html", null, htmlKids)
    };
    assertEquals(s, rewriteHelper(s, p, null, noOpLinkRewriter));
  }

  public void testPreserveComment() throws Exception {
    String s = "<script>  <!-- something here --></script>";
    ParsedHtmlNode[] comment = {
      GadgetHtmlNodeTest.makeParsedTextNode("  <!-- something here -->")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, comment)
    };
    assertEquals(s, rewriteHelper(s, p, null, noOpLinkRewriter));
  }

  public void testPreserveText() throws Exception {
    String s = "<script>dontcare</script>";
    ParsedHtmlNode[] text = {
      GadgetHtmlNodeTest.makeParsedTextNode("dontcare")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, text)
    };
    assertEquals(s, rewriteHelper(s, p, null, noOpLinkRewriter));
  }

  // Eventaully we want the opposite.
  public void testPreserveUselessWhitespace() throws Exception {
    String s = "   <script>  \n</script>\n ";
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("  \n")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTextNode("   "),
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids),
      GadgetHtmlNodeTest.makeParsedTextNode("\n ")
    };
    assertEquals(s, rewriteHelper(s, p, null, noOpLinkRewriter));
  }
  
  // Formerly from JavascriptTagMergerTest
  // This will be refactored into its own Rewriter test once the Rewriter passes
  // themselves are separated out.
  public void testJSMergePreserveNoExternal() throws Exception {
    String s = "<script>\n"
        + "doSomething\n"
        + "</script>";
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("\ndoSomething\n")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids)
    };
    assertEquals(s, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }

  public void testJSMergePreserveNoScript() throws Exception {
    String s
        = "<html><div id=\"test\">ceci ne pas une script</div></html>";
    String[][] attribs = { { "id", "test" } };
    ParsedHtmlNode[] divKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("ceci ne pas une script")
    };
    ParsedHtmlNode[] htmlKids = {
      GadgetHtmlNodeTest.makeParsedTagNode("div", attribs, divKids)
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("html", null, htmlKids)
    };
    assertEquals(s, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }

  public void testJSMergePreserveWithComment() throws Exception {
    String s = "<script>" +
        "<!--\ndoSomething\n-->" +
        "</script>";
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("<!--\ndoSomething\n-->")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids)
    };
    assertEquals(s, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }

  public void testJSMergeSingleScriptReWrite() throws Exception {
    String s = "<script src=\"http://a.b.com/1.js\"></script>";
    String[][] attribs = { { "src", "http://a.b.com/1.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attribs, null)
    };
    String rewritten
        = "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js\"></script>";
    assertEquals(rewritten, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }

  public void testJSMergeTwoScriptReWriteWithWhitespace() throws Exception {
    String s = "<script src=\"http://a.b.com/1.js\"></script>\n"
        + "<script src=\"http://a.b.com/2.js\"></script>";
    String[][] attr1 = { { "src", "http://a.b.com/1.js" } };
    String[][] attr2 = { { "src", "http://a.b.com/2.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null),
      GadgetHtmlNodeTest.makeParsedTextNode("\n"),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr2, null)
    };
    String rewritten
        = "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></script>";
    assertEquals(rewritten, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }

  public void testJSMergeLeadAndTrailingScriptReWrite() throws Exception {
    String s = "<script>\n"
        + "doSomething\n"
        + "</script>"
        + "<script src=\"http://a.b.com/1.js\"></script>"
        + "<script src=\"http://a.b.com/2.js\"></script>"
        + "<script>"
        + "doSomething\n"
        + "</script>";
    String[][] attr1 = { { "src", "http://a.b.com/1.js" } };
    String[][] attr2 = { { "src", "http://a.b.com/2.js" } };
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("\ndoSomething\n")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr2, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids)
    };
    String rewritten = "<script>\n"
        + "doSomething\n"
        + "</script>"
        + "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></script>"
        + "<script>\n"
        + "doSomething\n"
        + "</script>";
    assertEquals(rewritten, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }

  public void testJSMergeInterspersed() throws Exception {
    String s = "<script src=\"http://a.b.com/1.js\"></script>"
        + "<script src=\"http://a.b.com/2.js\"></script>"
        + "<script><!-- doSomething --></script>"
        + "<script src=\"http://a.b.com/3.js\"></script>"
        + "<script src=\"http://a.b.com/4.js\"></script>";
    String[][] attr1 = { { "src", "http://a.b.com/1.js" } };
    String[][] attr2 = { { "src", "http://a.b.com/2.js" } };
    String[][] attr3 = { { "src", "http://a.b.com/3.js" } };
    String[][] attr4 = { { "src", "http://a.b.com/4.js" } };
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("<!-- doSomething -->")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr2, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr3, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr4, null)
    };
    String rewritten =
        "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></script>" +
        "<script><!-- doSomething --></script>" +
        "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F3.js&2=http%3A%2F%2Fa.b.com%2F4.js\"></script>";
    assertEquals(rewritten, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }

  public void testJSMergeDerelativizeHostRelative() throws Exception {
    String s = "<script src=\"/1.js\"></script>";
    String[][] attr1 = { { "src", "/1.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null)  
    };
    String rewritten
        = "<script src=\"" + concatBase + "1=http%3A%2F%2Fgadget.org%2F1.js\"></script>";
    assertEquals(rewritten, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }

  public void testJSMergeDerelativizePathRelative() throws Exception {
    String s = "<script src=\"1.js\"></script>";
    String[][] attr1 = { { "src", "1.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null)  
    };
    String rewritten
        = "<script src=\"" + concatBase + "1=http%3A%2F%2Fgadget.org%2Fdir%2F1.js\"></script>";
    assertEquals(rewritten, rewriteHelper(s, p, jsFeature, noOpLinkRewriter));
  }
  
  // Formerly from LinkingTagRewriterTest
  public void testLinkingTagStandardRewrite() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif\"></img>\n"
        + "<IMG src=\"http://a.b.com/img2.gif\"/>\n"
        + "<eMbeD src=\"http://a.b.com/some.mov\"/>\n"
        + "<link href=\"http://a.b.com/link.html\"></link>";
    String[][] img1attrib = { { "src", "http://a.b.com/img.gif" } };
    String[][] img2attrib = { { "src", "http://a.b.com/img2.gif" } };
    String[][] emb1attrib = { { "src", "http://a.b.com/some.mov" } };
    String[][] href1attr = { { "href", "http://a.b.com/link.html" } };
    ParsedHtmlNode[] p = {
        GadgetHtmlNodeTest.makeParsedTagNode("img", img1attrib, null),
        GadgetHtmlNodeTest.makeParsedTextNode("\n"),
        GadgetHtmlNodeTest.makeParsedTagNode("IMG", img2attrib, null),
        GadgetHtmlNodeTest.makeParsedTextNode("\n"),
        GadgetHtmlNodeTest.makeParsedTagNode("eMbeD", emb1attrib, null),
        GadgetHtmlNodeTest.makeParsedTextNode("\n"),
        GadgetHtmlNodeTest.makeParsedTagNode("link", href1attr, null)
    };
    String rewritten = "<img src=\"" + LINK_PREFIX + "http://a.b.com/img.gif\"/>\n"
        + "<IMG src=\"" + LINK_PREFIX + "http://a.b.com/img2.gif\"/>\n"
        + "<eMbeD src=\"" + LINK_PREFIX + "http://a.b.com/some.mov\"/>\n"
        + "<link href=\"" + LINK_PREFIX + "http://a.b.com/link.html\"/>";
    assertEquals(rewritten, rewriteHelper(s, p, null, pfxLinkRewriter));
  }

  // Tests style-tag rewriting
  public void testStyleTagRewrites() throws Exception {
    String css =
      "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String s = "<style>" + css + "</style>";
    ParsedHtmlNode[] styleKids = {
      GadgetHtmlNodeTest.makeParsedTextNode(css)
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("style", null, styleKids)
    };
    String rewritten =
      "<style>div {list-style-image:url(\"" + LINK_PREFIX + "http://a.b.com/bullet.gif\");list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(\"" + LINK_PREFIX + "http://a.b.com/bigimg.png\");float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}</style>";
    assertEquals(rewritten, rewriteHelper(s, p, styleFeature, pfxLinkRewriter));
  }
  
  public void testMixedRewritingJSMergeTagAndScript() throws Exception {
    String s = "<style> @import url(rewrite.css); </style>"
        + "<link href=\"rewritelink.css\"/>"
        + "<p id=\"foo\">foo</p>"
        + "<p id=\"bar\">bar</p>"
        + "<script src=\"http://a.b.com/1.js\"></script>"
        + "<script src=\"http://a.b.com/2.js\"></script>";
    ParsedHtmlNode[] styleKid = {
      GadgetHtmlNodeTest.makeParsedTextNode(" @import url( rewrite.css ); ")
    };
    ParsedHtmlNode[] fooKid = {
      GadgetHtmlNodeTest.makeParsedTextNode("foo")
    };
    ParsedHtmlNode[] barKid = {
      GadgetHtmlNodeTest.makeParsedTextNode("bar")
    };
    String[][] linkAttr = { { "href", "rewritelink.css" } };
    String[][] fooAttr = { { "id", "foo" } };
    String[][] barAttr = { { "id", "bar" } };
    String[][] attr1 = { { "src", "http://a.b.com/1.js" } };
    String[][] attr2 = { { "src", "http://a.b.com/2.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("style", null, styleKid),
      GadgetHtmlNodeTest.makeParsedTagNode("link", linkAttr, null),
      GadgetHtmlNodeTest.makeParsedTagNode("p", fooAttr, fooKid),
      GadgetHtmlNodeTest.makeParsedTagNode("p", barAttr, barKid),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr2, null)
    };
    String rewritten =
        "<style> @import url(\"" + LINK_PREFIX + "rewrite.css\"); </style>" +
        "<link href=\"" + LINK_PREFIX + "rewritelink.css\"/>" +
        "<p id=\"foo\">foo</p>" +
        "<p id=\"bar\">bar</p>" +
        "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></script>";
    assertEquals(rewritten, rewriteHelper(s, p, comboFeature, pfxLinkRewriter));
  }
  
  public void testEndToEndGadgetWithCajaParsing() throws Exception {
    String s = "<style type=\"text/css\"> @import url(rewrite.css); </style>\n"
      + "<link href=\"rewritelink.css\"/>\n"
      + "<p>A simple gadget to demonstrate the content rewriter</p>\n"
      + "<div>\n"
      + "  This is a URL in content that was not rewritten http://www.notrewritten.com\n"
      + "</div>\n"
      + "<div id=\"backgrdiv\">\n"
      + "  This div has a background <br/> image from imported CSS\n"
      + "</div>\n"
      + "<div id=\"backgrdiv2\">\n"
      + "  This div has a background <br/> image from linked CSS\n"
      + "</div>\n"
      + "<p> This <img src=\"img.png\"/> is an image tag that was rewritten</p>\n"
      + "<p id=\"foo\">foo</p>\n"
      + "<p id=\"bar\">bar</p>\n"
      + "<script src=\"/1.js\"></script>\n"
      + "<script src=\"/2.js\"></script>";
    CajaHtmlParser chp = new CajaHtmlParser();
    ParsedHtmlNode[] p = chp.parse(s).toArray(new ParsedHtmlNode[0]);
    String rewritten = "<style type=\"text/css\"> @import url(\"" + LINK_PREFIX + "rewrite.css\"); </style>\n"
      + "<link href=\"" + LINK_PREFIX + "rewritelink.css\"/>\n"
      + "<p>A simple gadget to demonstrate the content rewriter</p>\n"
      + "<div>\n"
      + "  This is a URL in content that was not rewritten http://www.notrewritten.com\n"
      + "</div>\n"
      + "<div id=\"backgrdiv\">\n"
      + "  This div has a background <br/> image from imported CSS\n"
      + "</div>\n"
      + "<div id=\"backgrdiv2\">\n"
      + "  This div has a background <br/> image from linked CSS\n"
      + "</div>\n"
      + "<p> This <img src=\"" + LINK_PREFIX + "img.png\"/> is an image tag that was rewritten</p>\n"
      + "<p id=\"foo\">foo</p>\n"
      + "<p id=\"bar\">bar</p>\n"
      + "<script src=\"" + concatBase + "1=http%3A%2F%2Fgadget.org%2F1.js&2=http%3A%2F%2Fgadget.org%2F2.js\"></script>";
    assertEquals(rewritten, rewriteHelper(s, p, comboFeature, pfxLinkRewriter));
  }
}

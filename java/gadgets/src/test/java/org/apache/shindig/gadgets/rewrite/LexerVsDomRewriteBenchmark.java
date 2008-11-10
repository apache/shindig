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

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.rewrite.lexer.DefaultContentRewriter;
import org.apache.shindig.gadgets.rewrite.lexer.HtmlTagTransformer;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Map;

/**
 * Compare performance of lexer rewriter and dom rewriter.
 */
public class LexerVsDomRewriteBenchmark {

  private int numRuns;
  private String content;

  private GadgetHtmlParser cajaParser = new CajaHtmlParser(
      new ParseModule.DOMImplementationProvider().get());

  private GadgetHtmlParser nekoParser = new NekoHtmlParser(
      new ParseModule.DOMImplementationProvider().get());

  private GadgetHtmlParser nekoSimpleParser = new NekoSimplifiedHtmlParser(
      new ParseModule.DOMImplementationProvider().get());

  // Caja lexer
  private Map<String, HtmlTagTransformer> defaultTransformerMap;

  private HTMLContentRewriter htmlRewriter;
  private boolean warmup;
  private ContentRewriterFeatureFactory factory;
  private DefaultContentRewriter lexerRewriter;
  private Gadget gadget;

  private LexerVsDomRewriteBenchmark(String file, int numRuns) throws Exception {
    File inputFile = new File(file);
    if (!inputFile.exists() || !inputFile.canRead()) {
      System.err.println("Input file: " + file + " not found or can't be read.");
      System.exit(1);
    }

    Injector injector = Guice.createInjector(new PropertiesModule(), new OAuthModule(),
        new DefaultGuiceModule());

    // Lexer setup
    lexerRewriter = injector.getInstance(DefaultContentRewriter.class);
    // End lexer setup

    // DOM setup
    this.htmlRewriter = injector.getInstance(HTMLContentRewriter.class);
    factory = injector.getInstance(ContentRewriterFeatureFactory.class);
    // End DOM setup

    final Uri url = Uri.parse("http://www.example.org/dummy.xml");
    GadgetSpec spec = new GadgetSpec(url,
        "<Module><ModulePrefs title=''/><Content><![CDATA[]]></Content></Module>");

    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return url.toJavaUri();
      }
    };

    gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);

    content = new String(IOUtils.toByteArray(new FileInputStream(file)));
    this.numRuns = numRuns;

    warmup = true;
    //runLexer();
    //run(cajaParser);
    run(nekoParser);
    //run(nekoSimpleParser);
    runNoParse(nekoSimpleParser);
    Thread.sleep(5000L);
    warmup = false;
    //System.out.println("Lexer------");
    //runLexer();
    //System.out.println("Caja-------");
    //run(cajaParser);
    //System.out.println("Neko-------");
    //run(nekoParser);
    System.out.println("NekoSimple-------");
    run(nekoSimpleParser);
    System.out.println("No-Parse rewrite full DOM-------");
    runNoParse(nekoParser);
    System.out.println("No-Parse rewrite simple DOM-------");
    runNoParse(nekoSimpleParser);
  }

  private void output(String content) {
    if (!warmup) {
      System.out.println(content);
    }
  }

  private void runLexer() throws Exception {
   long startTime = System.currentTimeMillis();
    for (int i = 0; i < numRuns; i++) {
      MutableContent mc = new MutableContent(null, content, null);
      lexerRewriter.rewrite(gadget, mc);
      mc.getContent();
    }
    long time = System.currentTimeMillis() - startTime;
    output("Lexer Rewrite [" + time + " ms total: " +
          ((double)time)/numRuns + "ms/run]");
  }

  private void run(GadgetHtmlParser parser) throws Exception {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numRuns; i++) {
      MutableContent mc = new MutableContent(parser, content, null);
      //linkRewriter.rewrite(gadget, mc);
      //jsConcatRewriter.rewrite(gadget, mc);
      //styleLinksRewriter.rewrite(gadget, mc);
      htmlRewriter.rewrite(gadget, mc);
      mc.getContent();
    }
    long time = System.currentTimeMillis() - startTime;
    output("DOM Rewrite [" + time + " ms total: " +
          ((double)time)/numRuns + "ms/run]");

  }

  private void runNoParse(GadgetHtmlParser parser) throws Exception {
    Document doc = parser.parseDom(content);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numRuns; i++) {
      MutableContent mc = new MutableContent(parser, null, doc);
      htmlRewriter.rewrite(gadget, mc);          
      mc.getContent();
    }
    long time = System.currentTimeMillis() - startTime;
    output("DOM no-parse Rewrite [" + time + " ms total: " +
          ((double)time)/numRuns + "ms/run]");

  }


  public static void main(String[] args) {
    // Test can be run as standalone program to test out serialization and parsing
    // performance numbers, using Caja as a parser.
    if (args.length != 2) {
      System.err.println("Args: <input-file> <num-runs>");
      System.exit(1);
    }

    String fileArg = args[0];
    String runsArg = args[1];
    int numRuns = -1;
    try {
      numRuns = Integer.parseInt(runsArg);
    } catch (Exception e) {
      System.err.println("Invalid num-runs argument: " + runsArg + ", reason: " + e);
    }
    try {
      new LexerVsDomRewriteBenchmark(fileArg, numRuns);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

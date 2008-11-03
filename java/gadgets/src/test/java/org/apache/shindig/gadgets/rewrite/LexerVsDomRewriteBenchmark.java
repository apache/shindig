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
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.rewrite.lexer.HtmlRewriter;
import org.apache.shindig.gadgets.rewrite.lexer.HtmlTagTransformer;
import org.apache.shindig.gadgets.rewrite.lexer.LinkingTagRewriter;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.HashMap;
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
  private URI dummyUri;

  private LinkingTagContentRewriter domRewriter;
  private boolean warmup;

  private LexerVsDomRewriteBenchmark(String file, int numRuns) throws Exception {
    File inputFile = new File(file);
    if (!inputFile.exists() || !inputFile.canRead()) {
      System.err.println("Input file: " + file + " not found or can't be read.");
      System.exit(1);
    }

     LinkRewriter linkRewriter = new LinkRewriter() {
      public String rewrite(String link, URI context) {
        return link;
      }
    };

    // Lexer setup
    dummyUri = new URI("http://www.w3c.org");
    URI relativeBase = new URI("http://a.b.com/");
    LinkingTagRewriter lexerRewriter = new LinkingTagRewriter(
        linkRewriter, new URI("http://a.b.com/"));
    defaultTransformerMap = new HashMap<String, HtmlTagTransformer>();
    for (String tag : lexerRewriter.getSupportedTags()) {
      defaultTransformerMap .put(tag, lexerRewriter);
    }
    // End lexer setup

    // DOM setup
    domRewriter = new LinkingTagContentRewriter(linkRewriter, null);
    // End DOM setup

    content = new String(IOUtils.toByteArray(new FileInputStream(file)));
    this.numRuns = numRuns;
    warmup = true;
    runLexer();
    //run(cajaParser);
    run(nekoParser);
    run(nekoSimpleParser);
    Thread.sleep(5000L);
    warmup = false;
    System.out.println("Lexer------");
    runLexer();
    //System.out.println("Caja-------");
    //run(cajaParser);
    System.out.println("Neko-------");
    run(nekoParser);
    System.out.println("NekoSimple-------");
    run(nekoSimpleParser);
  }

  private void output(String content) {
    if (!warmup) {
      System.out.println(content);
    }
  }

  private void runLexer() throws Exception {
   long startTime = System.currentTimeMillis();
    for (int i = 0; i < numRuns; i++) {
      HtmlRewriter.rewrite(content, dummyUri, defaultTransformerMap);
    }
    long time = System.currentTimeMillis() - startTime;
    output("Lexer Rewrite [" + time + " ms total: " +
          ((double)time)/numRuns + "ms/run]");
  }

  private void run(GadgetHtmlParser parser) throws Exception {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numRuns; i++) {
      Document document = parser.parseDom(content);
      domRewriter.rewrite(document, dummyUri);
      HtmlSerializer.serialize(document);
    }
    long time = System.currentTimeMillis() - startTime;
    output("DOM Rewrite [" + time + " ms total: " +
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

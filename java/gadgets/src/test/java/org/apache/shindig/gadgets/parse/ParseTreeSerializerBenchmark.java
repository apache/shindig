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
package org.apache.shindig.gadgets.parse;

import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoHtmlParser;
import org.apache.shindig.gadgets.GadgetException;

import org.apache.commons.io.IOUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Tests serialization and deserialization of parse trees.
 */
public class ParseTreeSerializerBenchmark {
  private DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
  private int numRuns;
  private String content;
  private GadgetHtmlParser cajaParser = new CajaHtmlParser(new ParseModule.HTMLDocumentProvider());
  private GadgetHtmlParser nekoParser = new NekoHtmlParser(new ParseModule.HTMLDocumentProvider());

  private ParseTreeSerializerBenchmark(String file, int numRuns) throws Exception {
    File inputFile = new File(file);
    if (!inputFile.exists() || !inputFile.canRead()) {
      System.err.println("Input file: " + file + " not found or can't be read.");
      System.exit(1);
    }
    content = new String(IOUtils.toByteArray(new FileInputStream(file)));
    this.numRuns = numRuns;

    System.out.println("Caja Parse------------------------");
    run(cajaParser);
    System.out.println("Neko Parse------------------------");
    run(nekoParser);
  }

  private void run(GadgetHtmlParser parser) throws Exception {

    // Some warmup runs with wait. Enough iterations to trigger the JIT
    // Wait to allow it to swap execution paths etc...
    timeParseDom(parser, false);
    timeParseOld(parser, false);
    runLSSerializationTiming(parser, false);
    Thread.sleep(1000L);

    //System.out.println("Press a key to continue");
    //System.in.read();
    //System.out.println("Continuing");

    timeParseOld(parser, true);
    timeParseDom(parser, true);
    runLSSerializationTiming(parser, true);

    /*
    System.out.println("Serializing and deserializing results of Caja run (" +
        nodes.size() + " top-level nodes, " + numRuns + " runs)\n");


    long serTime = 0, deserTime = 0;
    for (int i = 0; i < numRuns; ++i) {
      long serStart = System.currentTimeMillis();
      byte[] ser = pts.serialize(nodes);
      serTime += (System.currentTimeMillis() - serStart);
      long deserStart = System.currentTimeMillis();
      List<ParsedHtmlNode> outs = pts.deserialize(ser);
      deserTime += (System.currentTimeMillis() - deserStart);
      //checkListEquality(nodes, outs);
    }
    */
    
    //System.out.println("Serialization [" + serTime + " ms total: "
    //    + ((double)serTime)/numRuns + "ms/run]");
    //System.out.println("Deserialization [" + deserTime + " ms total: "
    //    + ((double)deserTime)/numRuns + "ms/run]");
  }

  private void timeParseDom(GadgetHtmlParser parser, boolean output) throws GadgetException {
    long parseStart = System.currentTimeMillis();
    for (int i = 0; i < 10; ++i) {
      parser.parseDom(content);
    }
    long parseMillis = System.currentTimeMillis() - parseStart;

    if (output) {
      System.out.println("Parsing W3C DOM [" + parseMillis + " ms total: " +
          ((double)parseMillis)/numRuns + "ms/run]");
    }
  }

  private void timeParseOld(GadgetHtmlParser parser, boolean output) throws GadgetException {
    long parseStart = System.currentTimeMillis();
    List<ParsedHtmlNode> nodes;
    for (int i = 0; i < numRuns; ++i) {
      nodes = parser.parse(content);
    }
    long parseMillis = System.currentTimeMillis() - parseStart;

    if (output) {
      System.out.println("Parsing [" + parseMillis + " ms total: " +
          ((double)parseMillis)/numRuns + "ms/run]");
    }
  }

  private void runLSSerializationTiming(GadgetHtmlParser parser, boolean outputResult) throws Exception {
    Node n = parser.parseDom(content);
    DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
    ByteArrayOutputStream baos;
    baos = new ByteArrayOutputStream(content.length() * 2);
    LSSerializer writer = impl.createLSSerializer();
    LSParser lsParser = impl.createLSParser(LSParser.ACTION_APPEND_AS_CHILDREN, null);

    long serTime = 0, deserTime = 0;
    for (int i = 0; i < numRuns; ++i) {
      long serStart = System.currentTimeMillis();
      LSOutput output = impl.createLSOutput();
      baos.reset();
      output.setByteStream(baos);
      writer.write(n, output);
      serTime += (System.currentTimeMillis() - serStart);
      LSInput input = impl.createLSInput();
      input.setByteStream(new ByteArrayInputStream(baos.toByteArray()));
      long deserStart = System.currentTimeMillis();
      //XmlUtil.parse(new String(baos.toByteArray()));
      lsParser.parse(input);
      deserTime += (System.currentTimeMillis() - deserStart);
      //checkListEquality(nodes, outs);
    }

    if (outputResult) {
      System.out.println("LS Serialization [" + serTime + " ms total: "
          + ((double)serTime)/numRuns + "ms/run]");
      System.out.println("LS Deserialization [" + deserTime + " ms total: "
          + ((double)deserTime)/numRuns + "ms/run]");
    }
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
      new ParseTreeSerializerBenchmark(fileArg, numRuns);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

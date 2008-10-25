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

import org.apache.commons.io.IOUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoHtmlParser;
import org.apache.xml.serialize.HTMLSerializer;
import org.cyberneko.html.parsers.SAXParser;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.*;

import java.io.*;

/**
 * Benchmarks for HTML parsing and serialization
 *
 * NOTE - Uncomment DOM4J bits to test that.
 */
public class ParseTreeSerializerBenchmark {
  private DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
  private int numRuns;
  private String content;
  private GadgetHtmlParser cajaParser = new CajaHtmlParser(new ParseModule.HTMLDocumentProvider());
  private GadgetHtmlParser nekoParser = new NekoHtmlParser(new ParseModule.HTMLDocumentProvider());
  private boolean warmup;
  private SAXParser saxParser;
  //private SAXReader saxReader;

  private ParseTreeSerializerBenchmark(String file, int numRuns) throws Exception {
    File inputFile = new File(file);
    if (!inputFile.exists() || !inputFile.canRead()) {
      System.err.println("Input file: " + file + " not found or can't be read.");
      System.exit(1);
    }
    content = new String(IOUtils.toByteArray(new FileInputStream(file)));

    saxParser = new SAXParser();
    //saxParser.setFeature("http://cyberneko.org/html/features/scanner/script/strip-comment-delims",true);
    saxParser.setFeature("http://cyberneko.org/html/features/scanner/notify-builtin-refs",true);
    //saxReader = new SAXReader(saxParser);
    //saxReader.setValidation(false);

    this.numRuns = 50;
    warmup = true;
    runCaja();
    runNeko();
    runLS();
    Thread.sleep(10000L);
    this.numRuns = 300; //numRuns;
    warmup = false;
    runCaja();
    runNeko();
    runLS();
  }

  private void runCaja() throws Exception {
    output("Caja-----------------");
    // Some warmup runs with wait. Enough iterations to trigger the JIT
    // Wait to allow it to swap execution paths etc...
    timeParseDom(cajaParser);
  }

  private void runNeko() throws Exception {
    output("Neko-----------------");
    timeParseDom(nekoParser);
    //timeParseDom4J();
    //timeParseDom4JSerialize();
    timeParseDomSerialize(nekoParser);
  }

  private void runLS() throws Exception {
    output("LOAD/STORE-----------------");
    runLSSerializationTiming(nekoParser);
  }

  private void output(String string) {
    if (!warmup) {
      System.out.println(string);
    }
  }

  private void timeParseDom(GadgetHtmlParser parser) throws GadgetException {
    long parseStart = System.currentTimeMillis();
    for (int i = 0; i < numRuns; ++i) {
      parser.parseDom(content);
    }
    long parseMillis = System.currentTimeMillis() - parseStart;

    output("Parsing W3C DOM [" + parseMillis + " ms total: " +
          ((double)parseMillis)/numRuns + "ms/run]");
  }

  /*
  private void timeParseDom4J() throws GadgetException {
    try {
      long parseStart = System.currentTimeMillis();
      for (int i = 0; i < numRuns; ++i) {
         saxReader.read(new InputSource(new StringReader(content)));
      }
      long parseMillis = System.currentTimeMillis() - parseStart;

      output("Parsing DOM4J [" + parseMillis + " ms total: " +
            ((double)parseMillis)/numRuns + "ms/run]");
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, e);
    }
  }
  */

  /*
  private void timeParseDom4JSerialize() throws GadgetException {
    try {
      Document document =  saxReader.read(new InputSource(new StringReader(content)));
      OutputFormat format = OutputFormat.createCompactFormat();
      format.setXHTML(false);

      long parseStart = System.currentTimeMillis();
      for (int i = 0; i < numRuns; ++i) {
        StringWriter sw = new StringWriter((content.length() * 11) / 10);
        HTMLWriter htmlWriter = new HTMLWriter(sw, format) {
          protected void writeEntity(Entity entity) throws IOException {
            writer.write("&");
            writer.write(entity.getName());
            writer.write(";");
            lastOutputNodeType = org.dom4j.Node.ENTITY_REFERENCE_NODE;
          }
        };
        //htmlWriter.setResolveEntityRefs(false);
        htmlWriter.setEscapeText(false);
        htmlWriter.write(document);
      }
      long parseMillis = System.currentTimeMillis() - parseStart;

      output("Serializing DOM4J [" + parseMillis + " ms total: " +
            ((double)parseMillis)/numRuns + "ms/run]");
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, e);
    }

  }
  */

  private void timeParseDomSerialize(GadgetHtmlParser parser) throws GadgetException {
    org.w3c.dom.Document document = parser.parseDom(content);

    try {
      long parseStart = System.currentTimeMillis();
      for (int i = 0; i < numRuns; ++i) {
        StringWriter sw = new StringWriter((content.length() * 11) / 10);
        HTMLSerializer xercesSerializer = new HTMLSerializer(sw, new org.apache.xml.serialize.OutputFormat());
        xercesSerializer.serialize(document);
      }
      long parseMillis = System.currentTimeMillis() - parseStart;

      output("Serializing Xerces [" + parseMillis + " ms total: " +
            ((double) parseMillis) / numRuns + "ms/run]");
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, e);
    }
  }

  /*
  private void timeParseOld(GadgetHtmlParser parser) throws GadgetException {
    long parseStart = System.currentTimeMillis();
    for (int i = 0; i < numRuns; ++i) {
      parser.parse(content);
    }
    long parseMillis = System.currentTimeMillis() - parseStart;

    output("Parsing [" + parseMillis + " ms total: " +
          ((double)parseMillis)/numRuns + "ms/run]");
  }
  */

  private void runLSSerializationTiming(GadgetHtmlParser parser) throws Exception {
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

    output("LS Serialization [" + serTime + " ms total: "
          + ((double)serTime)/numRuns + "ms/run]");
    output("LS Deserialization [" + deserTime + " ms total: "
          + ((double)deserTime)/numRuns + "ms/run]");
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

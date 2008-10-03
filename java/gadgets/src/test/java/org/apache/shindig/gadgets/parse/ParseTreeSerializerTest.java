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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Tests serialization and deserialization of parse trees.
 */
public class ParseTreeSerializerTest {
  private static ParseTreeSerializer pts = new ParseTreeSerializer();
  
  public static void main(String[] args) throws Exception {
    // Test can be run as standalone program to test out serialization and parsing
    // performance numbers, using Caja as a parser.
    if (args.length != 2) {
      System.err.println("Args: <input-file> <num-runs>");
      System.exit(1);
    }
    
    String fileArg = args[0];
    File inputFile = new File(fileArg);
    if (!inputFile.exists() || !inputFile.canRead()) {
      System.err.println("Input file: " + fileArg + " not found or can't be read.");
      System.exit(1);
    }
    
    String runsArg = args[1];
    int numRuns = -1;
    try {
      numRuns = Integer.parseInt(runsArg);
    } catch (Exception e) {
      System.err.println("Invalid num-runs argument: " + runsArg + ", reason: " + e);
    }
    
    FileInputStream fis = new FileInputStream(inputFile);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[65535];
    int read = -1;
    while ((read = fis.read(buf)) > 0) {
      baos.write(buf, 0, read);
    }
    String inputData = new String(baos.toByteArray());
    
    // Caja parser.
    System.out.println("Parsing contents of '" + fileArg + "' " + numRuns + " times...");
    CajaHtmlParser parser = new CajaHtmlParser();
    long parseStart = System.currentTimeMillis();
    List<ParsedHtmlNode> nodes = null;
    for (int i = 0; i < numRuns; ++i) {
      nodes = parser.parse(inputData);
    }
    long parseMillis = System.currentTimeMillis() - parseStart;
    
    // Serializer/deserializer
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
      checkListEquality(nodes, outs);
    }
    
    System.out.println("Parsing [" + parseMillis + " ms total: " + 
        ((double)parseMillis)/numRuns + "ms/run]");
    System.out.println("Serialization [" + serTime + " ms total: "
        + ((double)serTime)/numRuns + "ms/run]");
    System.out.println("Deserialization [" + deserTime + " ms total: "
        + ((double)deserTime)/numRuns + "ms/run]");
  }
  
  @Test
  public void fromTestTreeToBytesAndBack() throws Exception {
    List<ParsedHtmlNode> nodes = new LinkedList<ParsedHtmlNode>();
    nodes.add(getEverythingNode());
    nodes.add(getEverythingNode());
    checkSerializationPasses(nodes);
  }
  
  @Test
  public void cantDeserializeDifferentVersion() throws Exception {
    List<ParsedHtmlNode> nodes = new LinkedList<ParsedHtmlNode>();
    nodes.add(getEverythingNode());
    byte[] serialized = pts.serialize(nodes);
    List<ParsedHtmlNode> back = pts.deserialize(serialized);
    checkListEquality(nodes, back);
    
    // This never happens in a given run of code, but is used to simulate
    // the version number of cached data getting out of sync with processing code.
    serialized[0]++;
    assertNull(pts.deserialize(serialized));
  }
  
  @Test
  public void fromCajaTreeToBytesAndBack() throws Exception {
    String bigHTML = "";
    for (int i = 0; i < 100; ++i) {
      bigHTML += "<parent pkey=\"pval\">parentText<child ckey=\"cval\">childText</child></parent>";
    }
    checkSerializationPasses(new CajaHtmlParser().parse(bigHTML));
  }

  private ParsedHtmlNode getEverythingNode() {
    // Return node containing a text node and a child node with attributes.
    ParsedHtmlNode childText = TestParsedHtmlNode.getText("childText");
    String[] childNVs = { "child", "cval" };
    ParsedHtmlNode[] childChildren = { childText };
    ParsedHtmlNode child = TestParsedHtmlNode.getTag("childNode", childNVs, childChildren);
    
    ParsedHtmlNode parentText = TestParsedHtmlNode.getText("parentText");
    String[] parentNVs = { "parent", "pval" };
    ParsedHtmlNode[] children = { child };
    return TestParsedHtmlNode.getTag("parentNode", parentNVs, children);
  }
  
  private static void checkSerializationPasses(List<ParsedHtmlNode> raw) throws Exception {
    byte[] serialized = pts.serialize(raw);
    List<ParsedHtmlNode> fromTheDead = pts.deserialize(serialized);
    checkListEquality(raw, fromTheDead);
  }
  
  private static void checkListEquality(List<ParsedHtmlNode> raw, List<ParsedHtmlNode> outs) {
    List<ParsedHtmlNode> rawTestable = new LinkedList<ParsedHtmlNode>();
    for (ParsedHtmlNode rawNode : raw) {
      rawTestable.add(TestParsedHtmlNode.get(rawNode));
    }
    List<ParsedHtmlNode> outTestable = new LinkedList<ParsedHtmlNode>();
    for (ParsedHtmlNode inNode : outs) {
      outTestable.add(TestParsedHtmlNode.get(inNode));
    }
    assertEquals(rawTestable, outTestable);
  }
  
  // Test class providing both a fake ParsedHtmlNode class as well as
  // one that provides equality testing for ParsedHtmlNodes of any provenance
  private static class TestParsedHtmlNode implements ParsedHtmlNode {
    private String tag;
    private String text;
    private List<ParsedHtmlAttribute> attribs;
    private List<ParsedHtmlNode> children;
    
    public static ParsedHtmlNode get(ParsedHtmlNode in) {
      TestParsedHtmlNode node = new TestParsedHtmlNode();
      node.text = in.getText();
      if (node.text == null) {
        node.tag = in.getTagName();
        node.attribs = new LinkedList<ParsedHtmlAttribute>();
        for (ParsedHtmlAttribute pha : in.getAttributes()) {
          node.attribs.add(new TestParsedHtmlAttribute(pha.getName(), pha.getValue()));
        }
        node.children = new LinkedList<ParsedHtmlNode>();
        for (ParsedHtmlNode child : in.getChildren()) {
          node.children.add(TestParsedHtmlNode.get(child));
        }
      }
      return node;
    }
    
    public static ParsedHtmlNode getTag(String tag, String[] nvpairs, ParsedHtmlNode[] children) {
      TestParsedHtmlNode node = new TestParsedHtmlNode();
      node.tag = tag;
      node.attribs = new LinkedList<ParsedHtmlAttribute>();
      for (int i = 0; i < nvpairs.length; i += 2) {
        node.attribs.add(new TestParsedHtmlAttribute(nvpairs[i], nvpairs[i+1]));
      }
      // Just in case somehow Arrays.asList() doesn't return a List subclassing
      // AbstractList (whose .equals() method doesn't check list type)
      node.children = new LinkedList<ParsedHtmlNode>();
      node.children.addAll(Arrays.asList(children));
      return node;
    }
    
    public static ParsedHtmlNode getText(String text) {
      TestParsedHtmlNode node = new TestParsedHtmlNode();
      node.text = text;
      return node;
    }

    public List<ParsedHtmlAttribute> getAttributes() {
      return attribs;
    }

    public List<ParsedHtmlNode> getChildren() {
      return children;
    }

    public String getTagName() {
      return tag;
    }

    public String getText() {
      return text;
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof TestParsedHtmlNode)) {
        return false;
      }
      TestParsedHtmlNode onode = (TestParsedHtmlNode)other;
      if (this.text != null) {
        return this.text.equals(onode.text);
      }
      return (this.tag.equals(onode.tag) &&
              this.attribs.equals(onode.attribs) &&
              this.children.equals(onode.children));
    }
  }
  
  private static class TestParsedHtmlAttribute implements ParsedHtmlAttribute {
    private String name;
    private String value;
    
    private TestParsedHtmlAttribute(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof TestParsedHtmlAttribute)) {
        return false;
      }
      TestParsedHtmlAttribute oattr = (TestParsedHtmlAttribute)other;
      return (this.name.equals(oattr.name) &&
              this.value.equals(oattr.value));
    }
  }
}

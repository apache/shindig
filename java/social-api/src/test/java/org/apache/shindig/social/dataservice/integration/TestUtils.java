package org.apache.shindig.social.dataservice.integration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * Collection of utilities to assist in testing.
 */
public class TestUtils {
  
  /**
   * Loads the contents of the test fixture specified at the given path.
   * 
   * @param path specifies the file to load the contents of
   * @return String is the file contents
   * @throws IOException 
   */
  public static String loadTestFixture(String path) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    return sb.toString();
  }
  
  /**
   * Tests two JSON strings for equality by performing a deep comparison.
   * 
   * @param json1 represents a JSON object to compare with json2
   * @param json2 represents a JSON object to compare with json1
   * @return true if the JSON objects are equal, false otherwise
   */
  public static boolean jsonsEqual(String json1, String json2) throws Exception {
    Object obj1Converted = convertJsonElement(new JSONObject(json1));
    Object obj2Converted = convertJsonElement(new JSONObject(json2));
    return obj1Converted.equals(obj2Converted);
  }
  
  
  /**
   * Tests the DOMs represented by two XML strings for equality by performing
   * a deep comparison.  The two DOMs are considered equal if the paths to all
   * leaf nodes are equal and the values at such paths are equal.
   * 
   * @param xml1 represents the XML DOM to compare with xml2
   * @param xml2 represents the XML DOM to compare with xml1
   * return true if the represented DOMs are equal, false otherwise
   */
  public static boolean xmlsEqual(String xml1, String xml2) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    
    Document doc1 = db.parse(new InputSource(new StringReader(xml1)));
    Document doc2 = db.parse(new InputSource(new StringReader(xml2)));
    
    Map<String, String> paths1 = getLeafPaths(doc1.getDocumentElement(), "");
    Map<String, String> paths2 = getLeafPaths(doc2.getDocumentElement(), "");

    return paths1.equals(paths2);
  }
  
  // ---------------------------- PRIVATE HELPERS -----------------------------
  
  /*
   * Recursive utility to convert a JSONObject to an Object composed of Sets,
   * Maps, and the target types (e.g. Integer, String, Double).  Used to do a
   * deep comparison of two JSON objects.
   * 
   * @param Object is the JSON element to convert (JSONObject, JSONArray, or target type)
   * 
   * @return an Object representing the appropriate JSON element
   */
  @SuppressWarnings("unchecked")
  private static Object convertJsonElement(Object elem) throws JSONException {
    if (elem instanceof JSONObject) {
      JSONObject obj = (JSONObject) elem;
      Iterator<String> keys = obj.keys();
      Map<String, Object> jsonMap = new HashMap<String, Object>();
      while (keys.hasNext()) {
        String key = keys.next();
        jsonMap.put(key, convertJsonElement(obj.get(key)));
      }
      return jsonMap;
    } else if (elem instanceof JSONArray) {
      JSONArray arr = (JSONArray) elem;
      Set<Object> jsonSet = new HashSet<Object>();
      for (int i = 0; i < arr.length(); i++) {
        jsonSet.add(convertJsonElement(arr.get(i)));
      }
      return jsonSet;
    } else {
      return elem;
    }
  }
  
  /*
   * Recursive utility to map all leaf node paths to the values at each path
   * within an XML node.
   * 
   * @param node is the root node to find all leaf paths & values for
   * @param basePath is the path to the root node
   * @return Map<String, String> is a Map of leaf paths & values for each path
   */
  private static Map<String, String> getLeafPaths(Node node, String basePath) {    
    Map<String, String> paths = new HashMap<String, String>();
    if (!node.hasChildNodes()) {
      if (!node.getTextContent().trim().equals("")) {
        paths.put(basePath, node.getTextContent());
      }
    } else {
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        paths.putAll(getLeafPaths(children.item(i), basePath + "/" + node.getNodeName()));
      }
    }
    return paths;
  }
}

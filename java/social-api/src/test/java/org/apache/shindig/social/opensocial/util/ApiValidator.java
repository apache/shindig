/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.opensocial.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that loads a feature set from features into a Javascript Parser to
 * make the model available to validate JSON messages against.
 */
public class ApiValidator {

  private static final Log log = LogFactory.getLog(ApiValidator.class);
  private Context ctx;
  private ScriptableObject scope;

  /**
   * @param feature
   *                The name of the feature are eg "opensocial-reference", this
   *                is a classpath stub (not starting with /) where the location
   *                contains a feature.xml file.
   * @throws SAXException
   *                 if feature.xml is not parsable
   * @throws ParserConfigurationException
   *                 if the parsers are invalid
   * @throws IOException
   *                 if feature.xml or the javascript that represents the
   *                 feature is missing
   *
   */
  private ApiValidator(String feature) throws IOException,
      ParserConfigurationException, SAXException {
    ctx = Context.enter();
    scope = ctx.initStandardObjects();
    load(feature);
  }

  /**
   * Load the ApiValidator with no features, this avoids having features in the classpath
   * @throws IOException
   */
  public ApiValidator() throws IOException {
    ctx = Context.enter();
    scope = ctx.initStandardObjects();
  }

  /**
   * @param json
   *                The json to validate expected in a form { xyz: yyy } form
   * @param object
   *                The json Fields object specifying the structure of the json
   *                object, each field in this object contains the name of the
   *                json field in the json structure.
   * @param optionalFields
   *                If any of the fields that appear in the json structure are
   *                optional, then they should be defined in this parameter.
   * @param nullfields
   * @throws ApiValidatorExpcetion
   *                 if there is a problem validating the json
   * @return a map so string object pairs containing the fields at the top level
   *         of the json tree. Where these are native java objects, they will
   *         appear as native object. Complex json objects will appear as Rhino
   *         specific objects
   */
  public Map<String, Object> validate(String json, String object,
      String[] optionalFields, String[] nullfields)
      throws ApiValidatorExpcetion {

    /*
     * Object[] ids = ScriptableObject.getPropertyIds(scope); for (Object id :
     * ids) { Object o = ScriptableObject.getProperty(scope,
     * String.valueOf(id)); log.debug("ID is " + id + " class " + id.getClass() + "
     * is " + o); if (o instanceof ScriptableObject) {
     * listScriptable(String.valueOf(id), (ScriptableObject) o); } }
     */

    log.debug("Loading " + json);
    json = json.trim();
    if (!json.endsWith("}")) {
      json = json + "}";
    }
    if (!json.startsWith("{")) {
      json = "{" + json;
    }
    json = "( testingObject = " + json + " )";

    Object so = null;
    try {
      so = ctx.evaluateString(scope, json, "test json", 0, null);
    } catch (EvaluatorException ex) {
      log.error("Non parseable JSON " + json);
    }
    log.debug("Loaded " + so);

    ScriptableObject specification = getScriptableObject(object);
    log.debug("Looking for  " + object + " found " + specification);
    listScriptable(object, specification);
    Object[] fields = specification.getIds();
    String[] fieldNames = new String[fields.length];
    for (int i = 0; i < fields.length; i++) {
      Object fieldName = specification.get(String.valueOf(fields[i]), specification);
      fieldNames[i] = String.valueOf(fieldName);
    }

    return validateObject(so, fieldNames, optionalFields, nullfields);

  }

  /**
   * @param json
   *                The json to validate expected in a form { xyz: yyy } form
   * @param fieldNames
   *                An Array of field names that the oject should be tested against
   * @param optionalFields
   *                If any of the fields that appear in the json structure are
   *                optional, then they should be defined in this parameter.
   * @param nullfields
   * @throws ApiValidatorExpcetion
   *                 if there is a problem validating the json
   * @return a map so string object pairs containing the fields at the top level
   *         of the json tree. Where these are native java objects, they will
   *         appear as native object. Complex json objects will appear as Rhino
   *         specific objects
   */
  public Map<String, Object> validate(String json, String[] fieldNames,
      String[] optionalFields, String[] nullfields)
      throws ApiValidatorExpcetion {


    log.debug("Loading " + json);
    json = json.trim();
    if (!json.endsWith("}")) {
      json = json + "}";
    }
    if (!json.startsWith("{")) {
      json = "{" + json;
    }
    json = "( testingObject = " + json + " )";

    Object so = null;
    try {
      so = ctx.evaluateString(scope, json, "test json", 0, null);
    } catch (EvaluatorException ex) {
      log.error("Non parseable JSON " + json);
    }
    log.debug("Loaded " + so);


    return validateObject(so, fieldNames, optionalFields, nullfields);

  }

  /**
   * Validate an JSON Object extracted
   *
   * @param object
   * @param string
   * @param optional
   * @return
   * @throws ApiValidatorExpcetion
   */
  public Map<String, Object> validateObject(Object jsonObject, String[] fieldNames,
      String[] optionalFields, String[] nullFields)
      throws ApiValidatorExpcetion {
    Map<String, String> optional = Maps.newHashMap();
    for (String opt : optionalFields) {
      optional.put(opt, opt);
    }
    Map<String, String> nullf = Maps.newHashMap();
    for (String nf : nullFields) {
      nullf.put(nf, nf);
    }


    Map<String, Object> resultFields = Maps.newHashMap();

    if (jsonObject instanceof ScriptableObject) {
      ScriptableObject parsedJSONObject = (ScriptableObject) jsonObject;
      listScriptable("testingObject", parsedJSONObject);
      for (String fieldName : fieldNames) {
        Object o = parsedJSONObject.get(fieldName,
            parsedJSONObject);
        if (o == Scriptable.NOT_FOUND) {
          if (optional.containsKey(fieldName)) {
            log.warn("Missing Optional Field " + fieldName);
          } else if (!nullf.containsKey(fieldName)) {
            log.error("Missing Field " + fieldName);
            throw new ApiValidatorExpcetion("Missing Field " + fieldName);
          }
        } else {
          if (nullf.containsKey(fieldName)) {
            log.error("Field should have been null and was not");
          }
          if (o == null) {
            if (nullf.containsKey(fieldName)) {
              log.error("Null Fields has been serialized " + fieldName);
            }
            log.debug("Got a Null object for Field " + fieldName
                + " on json [[" + jsonObject + "]]");

          } else {

            log.debug("Got JSON Field  Field,"  + fieldName + " as "
                + o + " " + o.getClass());
          }
          resultFields.put(String.valueOf(fieldName), o);
        }
      }

    } else {
      throw new ApiValidatorExpcetion(
          "Parsing JSON resulted in invalid Javascript object, which was "
              + jsonObject + " JSON was [[" + jsonObject + "]]");
    }
    return resultFields;
  }

  /**
   * get an object from the json context and scope.
   *
   * @param object
   *                the name of the object specified as a path from the base
   *                object
   * @return the json object
   */
  private ScriptableObject getScriptableObject(String object) {
    String[] path = object.split("\\.");
    log.debug("Looking up " + object + " elements " + path.length);

    ScriptableObject s = scope;
    for (String pe : path) {
      log.debug("Looking up " + pe + " in " + s);
      s = (ScriptableObject) s.get(pe, s);
      log.debug("Looking for " + pe + " in found " + s);
    }
    return s;
  }

  /**
   * List a scriptable object at log debug level, constructors will not be
   * expanded as this loads to recursion.
   *
   * @param id
   *                The name of the object
   * @param scriptableObject
   *                the scriptable Object
   */
  private void listScriptable(String id, ScriptableObject scriptableObject) {
    log.debug("ID is Scriptable " + id);
    if (!id.endsWith("constructor")) {
      Object[] allIDs = scriptableObject.getAllIds();
      for (Object oid : allIDs) {
        log.debug(id + "." + oid);
        Object o = scriptableObject.get(String.valueOf(oid), scriptableObject);
        if (o instanceof ScriptableObject) {
          listScriptable(id + "." + String.valueOf(oid), (ScriptableObject) o);
        }
      }
    }
  }

  /**
   * Load a feature based on the spec
   *
   * @param spec
   *                The name of the location of the spec in the classpath, must
   *                not start with a '/' and must should contain a feature.xml
   *                file in the location
   * @throws IOException
   *                 If any of the resources cant be found
   * @throws ParserConfigurationException
   *                 If the parser has a problem being constructed
   * @throws SAXException
   *                 on a parse error on the features.xml
   */
  private void load(String spec) throws IOException, SAXException,
      ParserConfigurationException {

    List<String> scripts = getScripts(spec);

    List<Script> compiled = Lists.newArrayList();
    for (String script : scripts) {
      String scriptPath = spec + "/" + script;
      InputStream in = this.getClass().getClassLoader().getResourceAsStream(
          scriptPath);
      if (in == null) {
        in = this.getClass().getClassLoader().getResourceAsStream(
            "features/" + scriptPath);
        if (in == null) {
          throw new IOException("Cant load spec " + spec + " or features/"
              + spec + " from classpath");
        }
      }
      InputStreamReader reader = new InputStreamReader(in);
      Script compiledScript = ctx.compileReader(reader, spec, 0, null);
      compiled.add(compiledScript);
    }

    for (Script compiledScript : compiled) {
      compiledScript.exec(ctx, scope);
    }

  }

  /**
   * Add some javascript to the context, and execute it. If extra custom
   * javascript is wanted in the context or scope then this method will load it.
   *
   * @param javascript
   */
  public void addScript(String javascript) {
    Script compileScript = ctx.compileString(javascript, "AdditionalJS", 0,
        null);
    compileScript.exec(ctx, scope);
  }

  /**
   * Get an ordered list of javascript resources from a feature sets.
   *
   * @param spec
   *                The spec location
   * @return An ordered list of javascript resources, these are relative to
   *         specification file.
   * @throws IOException
   *                 If any of the resources can't be loaded.
   * @throws SAXException
   *                 Where the feature.xml file is not parsable
   * @throws ParserConfigurationException
   *                 where the parser can't be constructed.
   * @return An ordered list of script that need to be loaded and executed to
   *         make the feature available in the context.
   */
  private List<String> getScripts(String spec) throws SAXException,
      IOException, ParserConfigurationException {
    String features = spec + "/feature.xml";
    InputStream in = this.getClass().getClassLoader().getResourceAsStream(
        features);
    if (in == null) {
      in = this.getClass().getClassLoader().getResourceAsStream(
          "features/" + features);
      if (in == null) {
        throw new IOException("Cant find " + features + " or features/"
            + features + " in classpath ");
      }
    }
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory
        .newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document doc = documentBuilder.parse(in);
    NodeList nl = doc.getElementsByTagName("script");
    List<String> scripts = Lists.newArrayList();
    for (int i = 0; i < nl.getLength(); i++) {
      Node scriptNode = nl.item(i);
      NamedNodeMap attributes = scriptNode.getAttributes();
      Node scriptAttr = attributes.getNamedItem("src");
      String script = scriptAttr.getNodeValue();
      scripts.add(script);
    }
    return scripts;
  }

  /**
   * @param nameJSON
   */
  public static void dump(Map<?, ?> nameJSON) {
    if (log.isDebugEnabled()) {
      for (Entry<?, ?> entry : nameJSON.entrySet()) {
        Object k = entry.getKey();
        Object o = entry.getValue();
        log.info("Key [" + k + "] value:[" + (o == null ? "null" : o + ":" + o.getClass()) + "]");
      }
    }
  }

}

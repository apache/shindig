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
package org.apache.shindig.protocol.conversion.jsonlib;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

/**
 * A class that loads a feature set from features into a Javascript Parser to
 * make the model available to validate JSON messages against.
 */
public class ApiValidator {

  private static final Logger log = Logger.getLogger(ApiValidator.class.getName());
  private Context ctx;
  private ScriptableObject scope;

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
   * @throws ApiValidatorException
   *                 if there is a problem validating the json
   * @return a map so string object pairs containing the fields at the top level
   *         of the json tree. Where these are native java objects, they will
   *         appear as native object. Complex json objects will appear as Rhino
   *         specific objects
   */
  public Map<String, Object> validate(String json, String object,
      String[] optionalFields, String[] nullfields)
      throws ApiValidatorException {

    /*
     * Object[] ids = ScriptableObject.getPropertyIds(scope); for (Object id :
     * ids) { Object o = ScriptableObject.getProperty(scope,
     * String.valueOf(id)); log.fine("ID is " + id + " class " + id.getClass() + "
     * is " + o); if (o instanceof ScriptableObject) {
     * listScriptable(String.valueOf(id), (ScriptableObject) o); } }
     */

    if (log.isLoggable(Level.FINE)) {
      log.fine("Loading " + json);
    }
    json = json.trim();
    if (!json.endsWith("}")) {
      json = json + '}';
    }
    if (!json.startsWith("{")) {
      json = '{' + json;
    }
    json = "( testingObject = " + json + " )";

    Object so = null;
    try {
      so = ctx.evaluateString(scope, json, "test json", 0, null);
    } catch (EvaluatorException ex) {
      log.severe("Non parseable JSON " + json);
    }
    if (log.isLoggable(Level.FINE)) {
      log.fine("Loaded " + so);
    }

    ScriptableObject specification = getScriptableObject(object);
    if (log.isLoggable(Level.FINE)) {
      log.fine("Looking for  " + object + " found " + specification);
    }
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
   * @throws ApiValidatorException
   *                 if there is a problem validating the json
   * @return a map so string object pairs containing the fields at the top level
   *         of the json tree. Where these are native java objects, they will
   *         appear as native object. Complex json objects will appear as Rhino
   *         specific objects
   */
  public Map<String, Object> validate(String json, String[] fieldNames,
      String[] optionalFields, String[] nullfields)
      throws ApiValidatorException {


    if (log.isLoggable(Level.FINE)) {
      log.fine("Loading " + json);
    }
    json = json.trim();
    if (!json.endsWith("}")) {
      json = json + '}';
    }
    if (!json.startsWith("{")) {
      json = '{' + json;
    }
    json = "( testingObject = " + json + " )";

    Object so = null;
    try {
      so = ctx.evaluateString(scope, json, "test json", 0, null);
    } catch (EvaluatorException ex) {
      log.severe("Non parseable JSON " + json);
    }
    if (log.isLoggable(Level.FINE)) {
      log.fine("Loaded " + so);
    }


    return validateObject(so, fieldNames, optionalFields, nullfields);

  }

  /**
   * Validate an JSON Object extracted
  * @throws ApiValidatorException
   */
  public Map<String, Object> validateObject(Object jsonObject, String[] fieldNames,
      String[] optionalFields, String[] nullFields)
      throws ApiValidatorException {
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
            log.warning("Missing Optional Field " + fieldName);
          } else if (!nullf.containsKey(fieldName)) {
            log.severe("Missing Field " + fieldName);
            throw new ApiValidatorException("Missing Field " + fieldName);
          }
        } else {
          if (nullf.containsKey(fieldName)) {
            log.severe("Field should have been null and was not");
          }
          if (o == null) {
            if (nullf.containsKey(fieldName)) {
              log.severe("Null Fields has been serialized " + fieldName);
            }
            if (log.isLoggable(Level.FINE)) {
              log.fine("Got a Null object for Field " + fieldName
                  + " on json [[" + jsonObject + "]]");
            }

          } else {

            if (log.isLoggable(Level.FINE)) {
              log.fine("Got JSON Field  Field,"  + fieldName + " as "
                  + o + ' ' + o.getClass());
            }
          }
          resultFields.put(String.valueOf(fieldName), o);
        }
      }

    } else {
      throw new ApiValidatorException(
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
    if (log.isLoggable(Level.FINE)) {
      log.fine("Looking up " + object + " elements " + path.length);
    }

    ScriptableObject s = scope;
    for (String pe : path) {
      if (log.isLoggable(Level.FINE)) {
        log.fine("Looking up " + pe + " in " + s);
      }
      s = (ScriptableObject) s.get(pe, s);
      if (log.isLoggable(Level.FINE)) {
        log.fine("Looking for " + pe + " in found " + s);
      }
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
    if (log.isLoggable(Level.FINE)) {
      log.fine("ID is Scriptable " + id);
    }
    if (!id.endsWith("constructor")) {
      Object[] allIDs = scriptableObject.getAllIds();
      for (Object oid : allIDs) {
        if (log.isLoggable(Level.FINE)) {
          log.fine(id + '.' + oid);
        }
        Object o = scriptableObject.get(String.valueOf(oid), scriptableObject);
        if (o instanceof ScriptableObject) {
          listScriptable(id + '.' + oid, (ScriptableObject) o);
        }
      }
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
   * @param nameJSON
   */
  public static void dump(Map<?, ?> nameJSON) {
    if (log.isLoggable(Level.INFO)) {
      for (Entry<?, ?> entry : nameJSON.entrySet()) {
        Object k = entry.getKey();
        Object o = entry.getValue();
        log.info("Key [" + k + "] value:[" + (o == null ? "null" : o + ":" + o.getClass()) + ']');
      }
    }
  }

}

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

package org.apache.shindig.gadgets;

import org.apache.shindig.common.util.ResourceLoader;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a container configuration.
 *
 * See config/container.js for an example configuration.
 *
 * We use a cascading model, so you only have to specify attributes in
 * your config that you actually want to change.
 */
public class ContainerConfig {
  private final Map<String, JSONObject> config;
  public static final char FILE_SEPARATOR = ',';
  public static final String DEFAULT_CONTAINER = "default";
  public static final String PARENT_KEY = "parent";
  // TODO: Rename this to simply "container", gadgets.container is unnecessary.
  public static final String CONTAINER_KEY = "gadgets.container";
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  /**
   * @return The set of all containers that are currently registered.
   */
  public Collection<String> getContainers() {
    return Collections.unmodifiableSet(config.keySet());
  }

  /**
   * Fetches a configuration parameter as a JSON object, array, string, or
   * number, ensuring that it can be safely passed to javascript without any
   * additional filtering.
   *
   * @param container
   * @param parameter The value to fetch. May be specified as an x-path like
   *     object reference such as "gadgets/features/views".
   * @return A configuration parameter as a JSON object or null if not set or
   *     can't be interpreted as JSON.
   */
  public Object getJson(String container, String parameter) {
    JSONObject data = config.get(container);
    if (data == null) {
      return null;
    }
    if (parameter == null) {
      return data;
    }

    try {
      for (String param : parameter.split("/")) {
        Object next = data.get(param);
        if (next instanceof JSONObject) {
          data = (JSONObject)next;
        } else {
          return next;
        }
      }
      return data;
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Attempts to fetch a parameter for the given container, or the default
   * container if the specified container is not supported.
   *
   * @param container
   * @param parameter
   * @return A configuration parameter as a string, or null if not set.
   */
  public String get(String container, String parameter) {
    Object data = getJson(container, parameter);
    return data == null ? null : data.toString();
  }

  /**
   * @param container
   * @param parameter
   * @return A configuration parameter as a JSON object or null if not set or
   *     can't be interpreted as JSON.
   */
  public JSONObject getJsonObject(String container, String parameter) {
    Object data = getJson(container, parameter);
    if (data instanceof JSONObject) {
      return (JSONObject)data;
    }
    return null;
  }

  /**
   * @param container
   * @param parameter
   * @return A configuration parameter as a JSON object or null if not set or
   *     can't be interpreted as JSON.
   */
  public JSONArray getJsonArray(String container, String parameter) {
    Object data = getJson(container, parameter);
    if (data instanceof JSONArray) {
      return (JSONArray)data;
    }
    return null;
  }

  /**
   * Loads containers from directories recursively.
   *
   * Only files with a .js or .json extension will be loaded.
   *
   * @param files The files to examine.
   * @throws GadgetException
   */
  private void loadFiles(File[] files) throws GadgetException {
    try {
      for (File file : files) {
        logger.info("Reading container config: " + file.getName());
        if (file.isDirectory()) {
          loadFiles(file.listFiles());
        } else if (file.getName().endsWith(".js") ||
                   file.getName().endsWith(".json")) {
          loadFromString(ResourceLoader.getContent(file));
        }
      }
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, e);
    }
  }

  /**
   * Loads resources recursively.
   * @param files The base paths to look for container.xml
   * @throws GadgetException
   */
  private void loadResources(String[] files)  throws GadgetException {
    try {
      for (String entry : files) {
        logger.info("Reading container config: " + entry);
        String content = ResourceLoader.getContent(entry);
        loadFromString(content);
      }
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, e);
    }
  }

  /**
   * Merges two JSON objects together (recursively), with values from "merge"
   * replacing values in "base" to produce a new object.
   *
   * @param base The base object that values will be replaced into.
   * @param merge The object to merge values from.
   *
   * @throws JSONException if the two objects can't be merged for some reason.
   */
  private JSONObject mergeObjects(JSONObject base, JSONObject merge)
      throws JSONException {
    // Clone the initial object (JSONObject doesn't support "clone").

    JSONObject clone = new JSONObject(base, JSONObject.getNames(base));
    // Walk parameter list for the merged object and merge recursively.
    String[] fields = JSONObject.getNames(merge);
    for (String field : fields) {
      Object existing = clone.opt(field);
      Object update = merge.get(field);
      if (existing == null || update == null) {
        // It's new custom config, not referenced in the prototype, or
        // it's removing a pre-configured value.
        clone.put(field, update);
      } else {
        // Merge if object type is JSONObject.
        if (update instanceof JSONObject &&
            existing instanceof JSONObject) {
          clone.put(field, mergeObjects((JSONObject)existing,
                                        (JSONObject)update));
        } else {
          // Otherwise we just overwrite it.
          clone.put(field, update);
        }
      }
    }
    return clone;
  }

  /**
   * Recursively merge values from parent objects in the prototype chain.
   *
   * @return The object merged with all parents.
   *
   * @throws GadgetException If there is an invalid parent parameter
   *    in the prototype chain.
   */
  private JSONObject mergeParents(String container)
      throws GadgetException, JSONException {
    JSONObject base = config.get(container);
    if (DEFAULT_CONTAINER.equals(container)) {
      return base;
    }

    String parent = base.optString(PARENT_KEY, DEFAULT_CONTAINER);
    if (!config.containsKey(parent)) {
      throw new GadgetException(GadgetException.Code.INVALID_CONFIG,
          "Unable to locate parent '" + parent + "' required by "
          + base.getString(CONTAINER_KEY));
    }
    return mergeObjects(mergeParents(parent), base);
  }

  /**
   * Processes a container file.
   *
   * @param json
   * @throws GadgetException
   */
  protected void loadFromString(String json) throws GadgetException {
    try {
      JSONObject contents = new JSONObject(json);
      JSONArray containers = contents.getJSONArray(CONTAINER_KEY);

      for (int i = 0, j = containers.length(); i < j; ++i) {
        // Copy the default object and produce a new one.
        String container = containers.getString(i);
        config.put(container, contents);
      }
    } catch (JSONException e) {
      throw new GadgetException(GadgetException.Code.INVALID_CONFIG, e);
    }
  }

  /**
   * Loads containers from the specified resource. Follows the same rules
   * as {@code JsFeatureLoader.loadFeatures} for locating resources.
   *
   * @param path
   * @throws GadgetException
   */
  private void loadContainers(String path) throws GadgetException {
    try {
      for (String location : StringUtils.split(path, FILE_SEPARATOR)) {
        if (location.startsWith("res://")) {
          location = location.substring(6);
          logger.info("Loading resources from: " + location);
          if (path.endsWith(".txt")) {
            loadResources(ResourceLoader.getContent(location).split("[\r\n]+"));
          } else {
            loadResources(new String[]{location});
          }
        } else {
          logger.info("Loading files from: " + location);
          File file = new File(location);
          loadFiles(new File[]{file});
        }
      }

      // Now that all containers are loaded, we go back through them and merge
      // recursively. This is done at startup to simplify lookups.
      Map<String, JSONObject> merged
          = new HashMap<String, JSONObject>(config.size());
      for (String container : config.keySet()) {
        merged.put(container, mergeParents(container));
      }
      config.putAll(merged);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, e);
    } catch (JSONException e) {
      throw new GadgetException(GadgetException.Code.INVALID_CONFIG, e);
    }
  }

  /**
   * Creates a new, empty configuration.
   * @param containers
   * @throws GadgetException
   */
  @Inject
  public ContainerConfig(@Named("containers.default") String containers)
      throws GadgetException {
    config = new HashMap<String, JSONObject>();
    if (containers != null) {
      loadContainers(containers);
    }
  }
}
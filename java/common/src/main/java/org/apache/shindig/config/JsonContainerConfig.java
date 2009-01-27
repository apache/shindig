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

package org.apache.shindig.config;

import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.expressions.ElException;
import org.apache.shindig.expressions.Expression;
import org.apache.shindig.expressions.ExpressionContext;
import org.apache.shindig.expressions.Expressions;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a container configuration using JSON notation.
 *
 * See config/container.js for an example configuration.
 *
 * We use a cascading model, so you only have to specify attributes in
 * your config that you actually want to change.
 *
 * String values may use expressions. The variable context defaults to the 'current' container,
 * but parent values may be accessed through the special "parent" property.
 */
@Singleton
public class JsonContainerConfig extends AbstractContainerConfig {
  private static final Logger LOG = Logger.getLogger(JsonContainerConfig.class.getName());
  public static final char FILE_SEPARATOR = ',';
  public static final String PARENT_KEY = "parent";
  // TODO: Rename this to simply "container", gadgets.container is unnecessary.
  public static final String CONTAINER_KEY = "gadgets.container";

  private final Map<String, Map<String, Object>> config;

  /**
   * Creates a new, empty configuration.
   * @param containers
   * @throws ContainerConfigException
   */
  @Inject
  public JsonContainerConfig(@Named("shindig.containers.default") String containers)
      throws ContainerConfigException {
    config = createContainers(loadContainers(containers));
  }

  @Override
  public Collection<String> getContainers() {
    return Collections.unmodifiableSet(config.keySet());
  }

  @Override
  public Map<String, Object> getProperties(String container) {
    return config.get(container);
  }

  @Override
  public Object getProperty(String container, String property) {
    if (property.startsWith("${")) {
      // An expression!
      try {
        Expression<String> expression = Expressions.parse(property, String.class);
        return expression.evaluate(createExpressionContext(container));
      } catch (ElException e) {
        return null;
      }
    }

    Map<String, Object> containerData = config.get(container);
    if (containerData == null) {
      return null;
    }
    return containerData.get(property);
  }

  /**
   * Initialize each container's configuration.
   */
  private Map<String, Map<String, Object>> createContainers(JSONObject json) {
    Map<String, Map<String, Object>> map = Maps.newHashMap();
    for (String container : JSONObject.getNames(json)) {
      ExpressionContext context = createExpressionContext(container);
      map.put(container, jsonToMap(json.optJSONObject(container), context));
    }

    return map;
  }

  /**
   * Protected to allow overriding.
   */
  protected ExpressionContext createExpressionContext(String container) {
    return new ContainerConfigExpressionContext(container, this);
  }

  /**
   * Convert a JSON value to a configuration value.
   */
  private static Object jsonToConfig(Object json, ExpressionContext context) {
    if (json instanceof CharSequence) {
      return new DynamicConfigProperty(json.toString(), context);
    } else if (json instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) json;
      List<Object> values = new ArrayList<Object>(jsonArray.length());
      for (int i = 0, j = jsonArray.length(); i < j; ++i) {
        values.add(jsonToConfig(jsonArray.opt(i), context));
      }
      return Collections.unmodifiableList(values);
    } else if (json instanceof JSONObject) {
      return jsonToMap((JSONObject) json, context);
    }

    // A (boxed) primitive.
    return json;
  }

  private static Map<String, Object> jsonToMap(JSONObject json, ExpressionContext context) {
    Map<String, Object> values = new HashMap<String, Object>(json.length(), 1);
    for (String key : JSONObject.getNames(json)) {
      values.put(key, jsonToConfig(json.opt(key), context));
    }
    return Collections.unmodifiableMap(values);
  }

  /**
   * Loads containers from directories recursively.
   *
   * Only files with a .js or .json extension will be loaded.
   *
   * @param files The files to examine.
   * @throws ContainerConfigException
   */
  private void loadFiles(File[] files, JSONObject all) throws ContainerConfigException {
    try {
      for (File file : files) {
        LOG.info("Reading container config: " + file.getName());
        if (file.isDirectory()) {
          loadFiles(file.listFiles(), all);
        } else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".js") ||
                   file.getName().toLowerCase(Locale.ENGLISH).endsWith(".json")) {
          if (!file.exists()) {
            throw new ContainerConfigException(
                "The file '" + file.getAbsolutePath() + "' doesn't exist.");
          }
          loadFromString(ResourceLoader.getContent(file), all);
        } else {
          LOG.finest(file.getAbsolutePath() + " doesn't seem to be a JS or JSON file.");
        }
      }
    } catch (IOException e) {
      throw new ContainerConfigException(e);
    }
  }

  /**
   * Loads resources recursively.
   * @param files The base paths to look for container.xml
   * @throws ContainerConfigException
   */
  private void loadResources(String[] files, JSONObject all)  throws ContainerConfigException {
    try {
      for (String entry : files) {
        LOG.info("Reading container config: " + entry);
        String content = ResourceLoader.getContent(entry);
        loadFromString(content, all);
      }
    } catch (IOException e) {
      throw new ContainerConfigException(e);
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
   * @throws ContainerConfigException If there is an invalid parent parameter
   *    in the prototype chain.
   */
  private JSONObject mergeParents(String container, JSONObject all)
      throws ContainerConfigException, JSONException {
    JSONObject base = all.getJSONObject(container);
    if (DEFAULT_CONTAINER.equals(container)) {
      return base;
    }

    String parent = base.optString(PARENT_KEY, DEFAULT_CONTAINER);
    if (!all.has(parent)) {
      throw new ContainerConfigException(
          "Unable to locate parent '" + parent + "' required by "
          + base.getString(CONTAINER_KEY));
    }
    return mergeObjects(mergeParents(parent, all), base);
  }

  /**
   * Processes a container file.
   *
   * @param json
   * @throws ContainerConfigException
   */
  protected void loadFromString(String json, JSONObject all) throws ContainerConfigException {
    try {
      JSONObject contents = new JSONObject(json);
      JSONArray containers = contents.getJSONArray(CONTAINER_KEY);

      for (int i = 0, j = containers.length(); i < j; ++i) {
        // Copy the default object and produce a new one.
        String container = containers.getString(i);
        all.put(container, contents);
      }
    } catch (JSONException e) {
      throw new ContainerConfigException(e);
    }
  }

  /**
   * Loads containers from the specified resource. Follows the same rules
   * as {@code JsFeatureLoader.loadFeatures} for locating resources.
   *
   * @param path
   * @throws ContainerConfigException
   */
  private JSONObject loadContainers(String path) throws ContainerConfigException {
    JSONObject all = new JSONObject();
    try {
      for (String location : StringUtils.split(path, FILE_SEPARATOR)) {
        if (location.startsWith("res://")) {
          location = location.substring(6);
          LOG.info("Loading resources from: " + location);
          if (path.endsWith(".txt")) {
            loadResources(ResourceLoader.getContent(location).split("[\r\n]+"), all);
          } else {
            loadResources(new String[]{location}, all);
          }
        } else {
          LOG.info("Loading files from: " + location);
          File file = new File(location);
          loadFiles(new File[]{file}, all);
        }
      }

      // Now that all containers are loaded, we go back through them and merge
      // recursively. This is done at startup to simplify lookups.
      for (String container : JSONObject.getNames(all)) {
        all.put(container, mergeParents(container, all));
      }

      return all;
    } catch (IOException e) {
      throw new ContainerConfigException(e);
    } catch (JSONException e) {
      throw new ContainerConfigException(e);
    }
  }

  @Override
  public String toString() {
    return JsonSerializer.serialize(config);
  }
}

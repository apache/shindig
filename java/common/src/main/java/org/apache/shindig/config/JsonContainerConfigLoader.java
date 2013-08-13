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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.config.ContainerConfig.Transaction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to build container configurations from JSON notation.
 *
 * See config/container.js for an example configuration.
 *
 * We use a cascading model, so you only have to specify attributes in your
 * config that you actually want to change.
 *
 * String values may use expressions. The variable context defaults to the
 * 'current' container, but parent values may be accessed through the special
 * "parent" property.
 */
public class JsonContainerConfigLoader {

  private static final String classname = JsonContainerConfigLoader.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);
  private static final Splitter CRLF_SPLITTER = Splitter.onPattern("[\r\n]+");

  public static final char FILE_SEPARATOR = ',';
  public static final String SERVER_PORT = "SERVER_PORT";
  public static final String SERVER_HOST = "SERVER_HOST";
  public static final String CONTEXT_ROOT = "CONTEXT_ROOT";

  private JsonContainerConfigLoader() {
  }

  /**
   * Creates a transaction to append the contents of one or more files or
   * resources to an existing configuration.
   *
   * @param containers The comma-separated list of files or resources to load
   *        the container configurations from.
   * @param host The hostname where Shindig is running.
   * @param port The port number where Shindig is receiving requests.
   * @param contextRoot contextRoot where Shindig module is deployed
   * @param containerConfig The container configuration to add the contents of
   *        the file to.
   * @return A transaction to add the new containers to the configuration.
   * @throws ContainerConfigException If there was a problem reading the files.
   */
  public static Transaction getTransactionFromFile(
      String containers, String host, String port, String contextRoot,ContainerConfig containerConfig)
      throws ContainerConfigException {
    return addToTransactionFromFile(containers, host, port, contextRoot,containerConfig.newTransaction());
  }

  /**
   * Appends the contents of one or more files or resources to an transaction.
   *
   * @param containers The comma-separated list of files or resources to load
   *        the container configurations from.
   * @param host The hostname where Shindig is running.
   * @param port The port number where Shindig is receiving requests.
   * @param transaction The transaction to add the contents of the file to.
   * @return The transaction, to allow chaining.
   * @throws ContainerConfigException If there was a problem reading the files.
   */
  public static Transaction addToTransactionFromFile(
      String containers, String host, String port, String contextRoot, Transaction transaction)
      throws ContainerConfigException {
    List<Map<String, Object>> config = loadContainers(containers);
    addHostAndPortToDefaultContainer(config, host, port,contextRoot);
    addContainersToTransaction(transaction, config);
    return transaction;
  }

  /**
   * Parses a container in JSON notation.
   *
   * @param json The container configuration in JSON notation.
   * @return A parsed container configuration.
   */
  public static Map<String, Object> parseJsonContainer(JSONObject json) {
    return jsonToMap(json);
  }

  /**
   * Parses a container in JSON notation.
   *
   * @param json The container configuration in JSON notation.
   * @return A parsed container configuration.
   * @throws JSONException If there was a problem parsing the container.
   */
  public static Map<String, Object> parseJsonContainer(String json) throws JSONException {
    return parseJsonContainer(new JSONObject(json));
  }

  /**
   * Loads containers from the specified resource. Follows the same rules as
   * {@code JsFeatureLoader.loadFeatures} for locating resources.
   *
   * @param path
   * @throws ContainerConfigException
   */
  private static List<Map<String, Object>> loadContainers(String path)
      throws ContainerConfigException {
    List<Map<String, Object>> all = Lists.newArrayList();
    try {
      for (String location : Splitter.on(FILE_SEPARATOR).split(path)) {
        if (location.startsWith("res://")) {
          location = location.substring(6);
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "loadContainers", MessageKeys.LOAD_RESOURCES_FROM, new Object[] {location});
          }
          if (path.endsWith(".txt")) {
            loadResources(CRLF_SPLITTER.split(ResourceLoader.getContent(location)), all);
          } else {
            loadResources(ImmutableList.of(location), all);
          }
        } else {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "loadContainers", MessageKeys.LOAD_FILES_FROM, new Object[] {location});
          }
          File file = new File(location);
          loadFiles(new File[] {file}, all);
        }
      }

      return all;
    } catch (IOException e) {
      throw new ContainerConfigException(e);
    }
  }

  /**
   * Loads containers from directories recursively.
   *
   * Only files with a .js or .json extension will be loaded.
   *
   * @param files The files to examine.
   * @throws ContainerConfigException when IO exceptions occur
   */
  private static void loadFiles(File[] files, List<Map<String, Object>> all)
      throws ContainerConfigException {
    for (File file : files) {
      try {
        if (file == null) continue;
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "loadFiles", MessageKeys.READING_CONFIG, new Object[] {file.getName()});
        }
        if (file.isDirectory()) {
          loadFiles(file.listFiles(), all);
        } else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".js")
            || file.getName().toLowerCase(Locale.ENGLISH).endsWith(".json")) {
          if (!file.exists()) {
            throw new ContainerConfigException(
                "The file '" + file.getAbsolutePath() + "' doesn't exist.");
          }
          all.add(loadFromString(ResourceLoader.getContent(file)));
        } else {
          if (LOG.isLoggable(Level.FINEST))
            LOG.finest(file.getAbsolutePath() + " doesn't seem to be a JS or JSON file.");
        }
      } catch (IOException e) {
        throw new ContainerConfigException(
            "The file '" + file.getAbsolutePath() + "' has errors", e);
      }
    }
  }

  /**
   * Loads resources recursively.
   *
   * @param files The base paths to look for container.xml
   * @throws ContainerConfigException when IO errors occur
   */
  private static void loadResources(Iterable<String> files, List<Map<String, Object>> all)
      throws ContainerConfigException {
    try {
      for (String entry : files) {
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "loadResources", MessageKeys.READING_CONFIG, new Object[] {entry});
        }
        String content = ResourceLoader.getContent(entry);
        if (content == null || content.length() == 0)
          throw new IOException("The file " + entry + "is empty");
        all.add(loadFromString(content));
      }
    } catch (IOException e) {
      throw new ContainerConfigException(e);
    }
  }

  /**
   * Processes a container file.
   *
   * @param json json to parse and load
   * @throws ContainerConfigException when invalid json is encountered
   */
  private static Map<String, Object> loadFromString(String json) throws ContainerConfigException {
    try {
      return jsonToMap(new JSONObject(json));
    } catch (JSONException e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.logp(Level.WARNING, classname, "loadFromString", MessageKeys.READING_CONFIG, new Object[] {json});
      }
      throw new ContainerConfigException("Trouble parsing " + json, e);
    }
  }

  /**
   * Convert a JSON value to a configuration value.
   */
  private static Object jsonToConfig(Object json) {
    if (JSONObject.NULL.equals(json)) {
      return null;
    } else if (json instanceof CharSequence) {
      return json.toString();
    } else if (json instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) json;
      ImmutableList.Builder<Object> values = ImmutableList.builder();
      for (int i = 0, j = jsonArray.length(); i < j; ++i) {
        values.add(jsonToConfig(jsonArray.opt(i)));
      }
      return values.build();
    } else if (json instanceof JSONObject) {
      return jsonToMap((JSONObject) json);
    }

    // A (boxed) primitive.
    return json;
  }

  private static Map<String, Object> jsonToMap(JSONObject json) {
    String[] keys = JSONObject.getNames(json);
    if (keys == null) {
      return ImmutableMap.of();
    }
    Map<String, Object> values = new HashMap<String, Object>(json.length(), 1);
    for (String key : keys) {
      Object val = jsonToConfig(json.opt(key));
      //If this is a string see if its a pointer to an external resource, and if so, load the resource
      if (val instanceof String) {
        String stringVal = (String) val;
        if (stringVal.startsWith(ResourceLoader.RESOURCE_PREFIX) ||
            stringVal.startsWith(ResourceLoader.FILE_PREFIX)) {
          try {
            val = IOUtils.toString(ResourceLoader.open(stringVal), "UTF-8");
          } catch (IOException e) {
            if (LOG.isLoggable(Level.WARNING)) {
              LOG.logp(Level.WARNING, classname, "jsonToMap", MessageKeys.READING_CONFIG, e);
            }
          }
        }
      }
      values.put(key, val);
    }
    return Collections.unmodifiableMap(values);
  }

  private static void addHostAndPortToDefaultContainer(
      List<Map<String, Object>> config, String host, String port,String contextRoot) {
    for (int i = 0, j = config.size(); i < j; ++i) {
      Map<String, Object> container = config.get(i);
      @SuppressWarnings("unchecked")
      List<String> names = (List<String>) container.get(ContainerConfig.CONTAINER_KEY);
      if (names != null && names.contains(ContainerConfig.DEFAULT_CONTAINER)) {
        Map<String, Object> newContainer = Maps.newHashMap();
        newContainer.putAll(container);
        newContainer.put(SERVER_PORT, port);
        newContainer.put(SERVER_HOST, host);
        newContainer.put(CONTEXT_ROOT, contextRoot);
        config.set(i, Collections.unmodifiableMap(newContainer));
      }
    }
  }

  private static void addContainersToTransaction(
      Transaction transaction, List<Map<String, Object>> config) {
    for (Map<String, Object> container : config) {
      transaction.addContainer(container);
    }
  }
}

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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.shindig.common.JsonSerializer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Basic container configuration class, without expression support.
 *
 * We use a cascading model, so you only have to specify attributes in your
 * config that you actually want to change.
 *
 * Configurations can be added/modified/removed using transactions. The
 * configuration is protected with a read/write lock.
 */
public class BasicContainerConfig implements ContainerConfig {

  protected final Set<ConfigObserver> observers =
      Sets.newSetFromMap(new WeakHashMap<ConfigObserver, Boolean>());
  protected Map<String, Map<String, Object>> config = Maps.newHashMap();

  public Collection<String> getContainers() {
    return Collections.unmodifiableSet(config.keySet());
  }

  public Map<String, Object> getProperties(String container) {
    return config.get(container);
  }

  public Object getProperty(String container, String name) {
    Map<String, Object> containerData = config.get(container);
    if (containerData == null) {
      return null;
    }
    return containerData.get(name);
  }

  public String getString(String container, String property) {
    Object value = getProperty(container, property);
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  public int getInt(String container, String property) {
    Object value = getProperty(container, property);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    } else if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException nfe) {
        return 0;
      }
    }
    return 0;
  }

  public boolean getBool(String container, String property) {
    Object value = getProperty(container, property);
    if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue();
    } else if (value instanceof String) {
      return "true".equalsIgnoreCase((String) value);
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getList(String container, String property) {
    Object value = getProperty(container, property);
    if (value instanceof List) {
      return (List<T>) value;
    }
    return Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getMap(String container, String property) {
    Object value = getProperty(container, property);
    if (value instanceof Map) {
      return (Map<String, T>) value;
    }
    return Collections.emptyMap();
  }

  public void addConfigObserver(ConfigObserver observer, boolean notifyNow) {
    observers.add(observer);
    if (notifyNow) {
      notifyObservers(getContainers(), ImmutableSet.<String>of());
    }
  }

  public Transaction newTransaction() {
    return new BasicTransaction();
  }

  /**
   * Notifies the configuration observers that some containers' configurations
   * have been changed.
   *
   * @param changed The names of the containers that have been added or changed.
   * @param removed The names of the containers that have been removed.
   */
  protected void notifyObservers(Collection<String> changed, Collection<String> removed) {
    for (ConfigObserver observer : observers) {
      observer.containersChanged(this, changed, removed);
    }
  }

  @Override
  public String toString() {
    return JsonSerializer.serialize(config);
  }

  protected class BasicTransaction implements Transaction {
    protected boolean clear = false;
    protected Map<String, Map<String, Object>> setContainers = Maps.newHashMap();
    protected Set<String> removeContainers = Sets.newHashSet();
    protected ContainerConfigException throwException = null;

    public Transaction clearContainers() {
      clear = true;
      return this;
    }

    public Transaction addContainer(Map<String, Object> container) {
      Object names = container.get(CONTAINER_KEY);
      if (names instanceof Collection<?>) {
        for (Object name : (Collection<?>) names) {
          setContainers.put(name.toString(), container);
        }
      } else if (names != null) {
        setContainers.put(names.toString(), container);
      } else {
        throwException = new ContainerConfigException(
            "A container configuration doesn't have the " + CONTAINER_KEY + " property");
      }
      return this;
    }

    public Transaction removeContainer(String name) {
      removeContainers.add(name);
      return this;
    }

    public void commit() throws ContainerConfigException {
      if (throwException != null) {
        throw throwException;
      }
      Set<String> removed = Sets.newHashSet();
      Set<String> changed = Sets.newHashSet();
      synchronized (BasicContainerConfig.this) {
        BasicContainerConfig tmpConfig = getTemporaryConfig(!clear);
        changeContainersInConfig(tmpConfig, setContainers, removeContainers);
        // This point will not be reached if an exception was thrown.
        diffConfiguration(tmpConfig, changed, removed);
        setNewConfig(tmpConfig);
      }
      notifyObservers(changed, removed);
    }

    /**
     * Creates a temporary ContainerConfig object that optionally contains a
     * copy of the current configuration.
     *
     * If you subclass {@link BasicContainerConfig} and you change its
     * internals, you must generally override this method to generate an object
     * of the same type as your subclass, and to fill its contents correctly.
     *
     * @param copyValues Whether the current configuration should be copied.
     * @return A new ContainerConfig object of the appropriate type.
     */
    protected BasicContainerConfig getTemporaryConfig(boolean copyValues) {
      BasicContainerConfig tmp = new BasicContainerConfig();
      if (copyValues) {
        tmp.config = deepCopyConfig(config);
      }
      return tmp;
    }

    /**
     * Applies the requested changes in a container configuration.
     *
     * @param newConfig The container configuration object to modify.
     * @param setContainers A map from container name to container to
     *        add/modify.
     * @param removeContainers A set of names of containers to remove.
     * @throws ContainerConfigException If there was a problem setting the new
     *         configuration.
     */
    protected void changeContainersInConfig(BasicContainerConfig newConfig,
        Map<String, Map<String, Object>> setContainers, Set<String> removeContainers)
        throws ContainerConfigException {
      newConfig.config.putAll(setContainers);
      for (String container : removeContainers) {
        newConfig.config.remove(container);
      }
      for (String container : newConfig.config.keySet()) {
        newConfig.config.put(container, mergeParents(container, newConfig.config));
      }
    }

    /**
     * Replaces the old configuration with the new configuration.
     *
     * @param newConfig The map that contains the new configuration.
     */
    protected void setNewConfig(BasicContainerConfig newConfig) {
      config = newConfig.config;
    }

    /**
     * Recursively merge values from parent containers in the prototype chain.
     *
     * For example, for the following two containers:
     * { 'gadgets.container': ['default'],
     *   'base': '/gadgets/foo',
     *   'user': 'peter',
     *   'map': { 'latitude': 42, 'longitude': -8 },
     *   'data': [ 'foo', 'bar' ] }
     * { 'gadgets.container': ['new'],
     *   'user': 'anne',
     *   'colour': 'green',
     *   'map': { 'longitude': 130 },
     *   'data': null }
     *
     * It would result in a merged "new" container that looks like this:
     * { 'gadgets.container': ['new'],
     *   'base': '/gadgets/foo',
     *   'user': 'anne',
     *   'colour': 'green',
     *   'map': { 'latitude': 42, 'longitude': 130 },
     *   'data': null }
     *
     * @return The container merged with all parents.
     * @throws ContainerConfigException If there is an invalid parent parameter
     *         in the prototype chain.
     */
    protected Map<String, Object> mergeParents(String name, Map<String, Map<String, Object>> config)
        throws ContainerConfigException {
      Map<String, Object> container = config.get(name);
      if (ContainerConfig.DEFAULT_CONTAINER.equals(name)) {
        return container;
      }

      String parent = container.get(PARENT_KEY) != null
          ? container.get(PARENT_KEY).toString() : ContainerConfig.DEFAULT_CONTAINER;
      if (!config.containsKey(parent)) {
        throw new ContainerConfigException(
            "Unable to locate parent '" + parent + "' required by " + container.get(CONTAINER_KEY));
      }
      return mergeObjects(mergeParents(parent, config), container);
    }

    /**
     * Merges two container configurations together (recursively), adding values
     * from "parentValues" into "container" if "container" doesn't already
     * define them.
     *
     * @param parentValues The values that will be added if absent.
     * @param container The container to merge the values into.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeObjects(
        Map<String, Object> parentValues, Map<String, Object> container) {
      // Clone the object with the parent values
      Map<String, Object> clone = Maps.newHashMap(parentValues);
      // Walk parameter list for the container and merge recursively.
      for (Map.Entry<String, Object> entry : container.entrySet()) {
        String field = entry.getKey();
        Object fromParents = clone.get(field);
        Object fromContainer = entry.getValue();
        // Merge if object type is Map
        if (fromContainer instanceof Map<?, ?> && fromParents instanceof Map<?, ?>) {
          clone.put(field, mergeObjects(
              (Map<String, Object>) fromParents, (Map<String, Object>) fromContainer));
        } else {
          // Otherwise we just overwrite it.
          clone.put(field, fromContainer);
        }
      }
      return clone;
    }

    /**
     * Calculates the difference between the current and new configurations.
     *
     * @param newConfig The object containing the new configuration.
     * @param changed A set that will be populated with the names of the
     *        added/modified containers.
     * @param removed A set that will be populated with the names of the removed
     *        containers.
     */
    private void diffConfiguration(
        BasicContainerConfig newConfig, Set<String> changed, Set<String> removed) {
      removed.addAll(Sets.difference(config.keySet(), newConfig.config.keySet()));
      for (String container : newConfig.config.keySet()) {
        if (!newConfig.config.get(container).equals(config.get(container))) {
          changed.add(container);
        }
      }
    }

    /**
     * Returns a deep copy of a configuration object.
     *
     * @param config The configuration object to copy.
     * @return A copy of the configuration object.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Map<String, Object>> deepCopyConfig(
        Map<String, Map<String, Object>> config) {
      return (Map<String, Map<String, Object>>) deepCopyObject(config);
    }

    private Object deepCopyObject(Object obj) {
      if (obj instanceof Map<?, ?>) {
        Map<?, ?> objMap = (Map<?, ?>) obj;
        Map<Object, Object> map = Maps.newHashMap();
        for (Entry<?, ?> entry : objMap.entrySet()) {
          map.put(entry.getKey(), deepCopyObject(entry.getValue()));
        }
        return map;
      } else if (obj instanceof List<?>) {
        List<?> objList = (List<?>) obj;
        List<Object> list = Lists.newArrayList();
        for (Object elem : objList) {
          list.add(deepCopyObject(elem));
        }
        return list;
      } else {
        return obj;
      }
    }
  }
}

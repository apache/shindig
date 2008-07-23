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
package org.apache.shindig.social.core.util;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ezmorph.MorpherRegistry;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.DefaultValueProcessor;
import net.sf.json.util.EnumMorpher;
import net.sf.json.util.JSONUtils;
import net.sf.json.util.NewBeanInstanceStrategy;
import net.sf.json.util.PropertyFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.Enum;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.Url;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * BeanConverter implementation us the net.sf.json-lib json library.
 */
public class BeanJsonLibConverter implements BeanConverter {

  /**
   * The Logger
   */
  protected static final Log LOG = LogFactory.getLog(BeanJsonLibConverter.class);
  /**
   * The Guice injector used to create beans
   */
  private Injector injector;
  /**
   * Json Config object used by each instance.
   */
  private JsonConfig jsonConfig;
  /**
   * in IDE debug flag
   */
  private boolean debugMode = false;

  /*
   * Register the Enum Morphers so that JSON -> Bean works correctly for enums.
   */
  static {
    MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
    morpherRegistry.registerMorpher(new EnumMorpher(Address.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Phone.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Email.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(MediaItem.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(MediaItem.Type.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Drinker.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Gender.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.NetworkPresence.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Smoker.class));

    morpherRegistry.registerMorpher(new JsonObjectToMapMorpher());
  }

  @Inject
  public BeanJsonLibConverter(Injector injector) {
    this.injector = injector;
    createJsonConfig();
  }

  @SuppressWarnings("unchecked")
  public <T> T convertToObject(String string, final Class<T> rootBeanClass) {

    if ("".equals(string)) {
      string = "{}";
    }
    if (string.startsWith("[")) {
      JSONArray jsonArray = JSONArray.fromObject(string, jsonConfig);
      if (debugMode) {
        JsonLibConverterUtils.dumpJsonArray(jsonArray, " ");
      }

      if (rootBeanClass.isArray()) {
        Class<?> componentType = rootBeanClass.getComponentType();
        Object rootObject = injector.getInstance(componentType);
        List<?> o = JSONArray.toList(jsonArray, rootObject, jsonConfig);
        Object[] result = (Object[]) Array.newInstance(componentType, o.size());
        for (int i = 0; i < o.size(); i++) {
          result[i] = o.get(i);
        }
        return (T) result;

      } else {
        T rootObject = injector.getInstance(rootBeanClass);
        Object o = JSONArray.toArray(jsonArray, rootObject, jsonConfig);
        return (T) o;
      }
    } else {
      JSONObject jsonObject = JSONObject.fromObject(string, jsonConfig);

      if (debugMode) {
        JsonLibConverterUtils.dumpJsonObject(jsonObject, " ");
      }

      T rootObject = injector.getInstance(rootBeanClass);
      Object o = JSONObject.toBean(jsonObject, rootObject, jsonConfig);
      return (T) o;

    }
  }

  /**
   * @return
   */
  private void createJsonConfig() {

    jsonConfig = new JsonConfig();

    /*
     * This hook deals with the creation of new beans in the JSON -> Java Bean
     * conversion
     */
    jsonConfig.setNewBeanInstanceStrategy(new NewBeanInstanceStrategy() {

      @SuppressWarnings("unchecked")
      @Override
      public Object newInstance(Class beanClass, JSONObject jsonObject)
          throws InstantiationException, IllegalAccessException, NoSuchMethodException,
          InvocationTargetException {
        if (beanClass != null) {
          Object o = BeanJsonLibConverter.this.injector.getInstance(beanClass);
          if (debugMode) {
            LOG.info("Created Object " + o + " for " + beanClass + " with [" + jsonObject + "]");
          }
          return o;
        }
        return DEFAULT.newInstance(beanClass, jsonObject);
      }

    });

    /*
     * We are expecting null for nulls
     */
    jsonConfig.registerDefaultValueProcessor(String.class, new DefaultValueProcessor() {
      @SuppressWarnings("unchecked")
      public Object getDefaultValue(Class target) {
        return null;
      }
    });

    jsonConfig.setJsonPropertyFilter(new PropertyFilter() {

      public boolean apply(Object source, String name, Object value) {
        return filterProperty(source, name, value);
      }

    });

    jsonConfig.setJavaPropertyFilter(new PropertyFilter() {

      public boolean apply(Object source, String name, Object value) {
        return filterProperty(source, name, value);
      }

    });

    // the classMap deals with the basic json string to bean conversion

    Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();

    /*
     * mappings are required where there is a List of objects in the interface
     * with no indication of what type the list should contain. At the moment,
     * we are using 1 map for all json trees, as there is no conflict, but if
     * there is a map could be selected on the basis of the root object. It
     * would be better to do this with generics, but this is good enough and
     * compact enough for the moment.
     *
     */
    //
    // activity
    classMap.put("mediaItems", MediaItem.class);
    // this may not be necessary
    classMap.put("templateParams", Map.class);
    // BodyType needs no mappings
    // Message needs no mappings
    // Name needs no mappings
    // Organization needs no mappings
    // Url needs no mappings
    // Email needs no mappings
    // Phone Needs no mappings
    // Address Needs no mappings
    // MediaItem needs no mappings

    // Person map
    classMap.put("addresses", Address.class);
    classMap.put("phoneNumbers", Phone.class);
    classMap.put("emails", Email.class);
    classMap.put("mediaItems", MediaItem.class);
    classMap.put("jobs", Organization.class);
    classMap.put("schools", Organization.class);
    classMap.put("urls", Url.class);
    jsonConfig.setClassMap(classMap);

  }

  /**
   * Filter the output of a property, if it should be emitted return false.
   * @param source The object containing the value
   * @param name the name of the key in the output structure
   * @param value the value of the object
   * @return true if the property should be filtered, false if not.
   */
  protected boolean filterProperty(Object source, String name, Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof JSONArray) {
      JSONArray array = (JSONArray) value;
      if (array.size() == 0) {
        return true;
      }
    }
    if (value instanceof JSONObject) {
      JSONObject object = (JSONObject) value;
      if (object.isNullObject() || object.isEmpty()) {
        return true;
      }
    }
    if (value instanceof Collection) {
      Collection<?> collection = (Collection<?>) value;
      if (collection.size() == 0) {
        return true;
      }
    }
    if (value instanceof Object[]) {
      Object[] oarray = (Object[]) value;
      if (oarray.length == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Convert the pojo to a json string representation.
   * @param pojo the pojo to convert
   * @return the json string representation of the pojo.
   */
  public String convertToString(Object pojo) {
    if ("".equals(pojo)) {
      return "{}";
    }

    try {
      JSONObject jsonObject = JSONObject.fromObject(pojo, jsonConfig);
      return jsonObject.toString();
    } catch (JSONException jse) {
      Class<?> pojoClass = pojo.getClass();
      if (JSONUtils.isArray(pojoClass)) {
        JSONArray jsonArray = JSONArray.fromObject(pojo);
        String result = jsonArray.toString();
        return result;
      }
      throw jse;
    }
  }

  /**
   * Add a mapping to the json -> pojo conversion map.
   * @param key the name of the json key to bind to
   * @param class1 the class that should be used to represent that key
   */
  @SuppressWarnings("unchecked")
  public void addMapping(String key, Class<?> class1) {
    jsonConfig.getClassMap().put(key, class1);
  }

}

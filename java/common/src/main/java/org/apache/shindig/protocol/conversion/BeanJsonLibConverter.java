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
package org.apache.shindig.protocol.conversion;

import org.apache.shindig.protocol.conversion.jsonlib.JsonLibConverterUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.JSONUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;

/**
 * BeanConverter implementation us the net.sf.json-lib json library.
 */
public class BeanJsonLibConverter implements BeanConverter {
  /**
   * The Guice injector used to create beans.
   */
  private Injector injector;
  /**
   * Json Config object used by each instance.
   */
  private JsonConfig jsonConfig;
  /**
   * in IDE debug flag.
   */
  private final boolean debugMode = false;


  /**
   * Create an BeanConverter with an injector.
   * @param injector the Guice injector to use for conversion
   * @param jsonConfig the Json Configuration
   */
  @Inject
  public BeanJsonLibConverter(Injector injector,
      @Named("ShindigJsonConfig") JsonConfig jsonConfig) {
    this.injector = injector;
    this.jsonConfig = jsonConfig;
  }

  public String getContentType() {
    return "application/json";
  }

  /**
   * Convert the json string into a pojo based on the supplied root class.
   * @param string the json string
   * @param rootBeanClass the root class of the bean
   * @param <T> The typep of the pojo to be returned
   * @return A pojo of the same type as the rootBeanClass
   */
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

  public JSONObject convertToJson(Object pojo) {
    try {
      return JSONObject.fromObject(pojo, jsonConfig);
    } catch (JSONException jse) {
      throw new RuntimeException(jse);
    }
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
        return jsonArray.toString();
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
  
  public void append(Appendable buf, Object pojo) throws IOException {
    buf.append(convertToString(pojo));
  }

}

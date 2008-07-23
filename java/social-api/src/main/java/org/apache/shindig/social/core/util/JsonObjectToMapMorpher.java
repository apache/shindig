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

import java.util.HashMap;
import java.util.Map;

import net.sf.ezmorph.Morpher;
import net.sf.ezmorph.ObjectMorpher;
import net.sf.json.JSONObject;

/**
 *
 */
public class JsonObjectToMapMorpher implements Morpher, ObjectMorpher {

  public Class<?> morphsTo() {
    return Map.class;
  }

  @SuppressWarnings("unchecked")
  public boolean supports(Class clazz) {    
    return (JSONObject.class.equals(clazz));
  }

  public Object morph(Object bean) {
    Map<Object, Object> result = new HashMap<Object, Object>();
    JSONObject jsonObject = (JSONObject) bean;
    for ( Object key : jsonObject.keySet()) {
      result.put(key,jsonObject.get(key));
    }
    return result;
  }

}

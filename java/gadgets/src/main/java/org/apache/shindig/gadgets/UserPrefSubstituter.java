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
package org.apache.shindig.gadgets;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Substitutes user prefs into the spec.
 */
public class UserPrefSubstituter implements GadgetFeatureFactory {
  private final static UserPrefSubstituterFeature feature
      = new UserPrefSubstituterFeature();

  /**
  * {@inheritDoc}
  */
  public GadgetFeature create() {
    return feature;
  }
}

class UserPrefSubstituterFeature implements GadgetFeature {

  /**
   * {@inheritDoc}
   */
  public void prepare(GadgetView gadget, GadgetContext context,
                      Map<String, String> params) {
  }

  /**
   * {@inheritDoc}
   */
  public void process(Gadget gadget, GadgetContext context,
                      Map<String, String> params) {
    Substitutions substitutions = gadget.getSubstitutions();
    UserPrefs upValues = gadget.getUserPrefValues();

    JSONObject json = null;

    if (context.getRenderingContext() == RenderingContext.GADGET) {
      json = new JSONObject();
    }

    for (GadgetSpec.UserPref pref : gadget.getUserPrefs()) {
      String name = pref.getName();
      String value = upValues.getPref(name);
      if (value == null) {
        value = pref.getDefaultValue();
      }
      if (value == null) {
        value = "";
      }
      substitutions.addSubstitution(Substitutions.Type.USER_PREF, name, value);

      if (json != null) {
        try {
          json.put(name, value);
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    }

    if (json != null) {
      String setPrefFmt = "gadgets.prefs_.setPref(%d, %s);";
      int moduleId = gadget.getId().getModuleId();
      String setPrefStr = String.format(setPrefFmt, moduleId, json.toString());
      gadget.addJsLibrary(JsLibrary.create(JsLibrary.Type.INLINE, setPrefStr));
    }
  }
}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

import java.util.Map;

/**
 * Substitutes user prefs into the spec.
 */
public class UserPrefSubstituter implements GadgetFeature {

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
    Map<String, String> upValues = gadget.getUserPrefValues();
    for (GadgetSpec.UserPref pref : gadget.getUserPrefs()) {
      String name = pref.getName();
      String value = upValues.get(name);
      if (value == null) {
        value = pref.getDefaultValue();
      }
      if (value == null) {
        value = "";
      }
      substitutions.addSubstitution(Substitutions.Type.USER_PREF, name, value);
    }
  }
}

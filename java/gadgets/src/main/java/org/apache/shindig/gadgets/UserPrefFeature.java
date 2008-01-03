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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Adds user pref library support. Note that this only deals with read access
 * to prefs. Write access is handled separately.
 */
public class UserPrefFeature extends JsLibraryFeature {

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) throws GadgetException {
    Map<String, String> prefs = gadget.getUserPrefValues();
    Map<String, String> msgs = gadget.getCurrentMessageBundle().getMessages();
    JSONObject prefJson = new JSONObject();
    JSONObject msgJson = new JSONObject();
    try {
      for (Map.Entry<String, String> entry : prefs.entrySet()) {
        prefJson.put(entry.getKey(), entry.getValue());
      }
      for (Map.Entry<String, String> entry : msgs.entrySet()) {
        msgJson.put(entry.getKey(), entry.getValue());
      }
    } catch (JSONException e) {
      throw new GadgetException(GadgetException.Code.INVALID_USER_DATA, e);
    }
    StringBuilder output = new StringBuilder();
    output.append("gadgets.PrefStore_.setPref(")
        .append(gadget.getId().getModuleId())
        .append(",")
        .append(prefJson.toString())
        .append(");");
    output.append("gadgets.PrefStore_.setMsg(")
        .append(gadget.getId().getModuleId())
        .append(",")
        .append(msgJson.toString())
        .append(");");
    output.append("gadgets.PrefStore_.setLanguage(")
        .append(gadget.getId().getModuleId())
        .append(",\"")
        .append(context.getLocale().getLanguage())
        .append("\");");
    output.append("gadgets.PrefStore_.setCountry(")
        .append(gadget.getId().getModuleId())
        .append(",\"")
        .append(context.getLocale().getCountry())
        .append("\");");
    output.append("gadgets.PrefStore_.setDefaultModuleId(")
        .append(gadget.getId().getModuleId())
        .append(");");
    gadget.addJsLibrary(JsLibrary.inline("userprefs", output.toString()));
  }
}

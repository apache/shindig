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
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.JsLibraryFeature;

import java.util.Map;

/**
 * Adds support for analytics.
 */
public class AnalyticsFeature extends JsLibraryFeature {
  private static final String FEATURE_NAME = "analytics";
  private static final String URCHIN_JS
      = "http://www.gmodules.com/ig/f/8JdLW-FnPCU/lib/liburchin.js";
  private static final String ANALYTICS_JS
      = "http://www.gmodules.com/ig/f/ltPUpXpo9mk/lib/libanalytics.js";
  /**
   * {@inheritDoc}
   */
  @Override
  public void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) throws GadgetException {
    gadget.addJsLibrary(JsLibrary.uri(FEATURE_NAME, URCHIN_JS));
    gadget.addJsLibrary(JsLibrary.uri(FEATURE_NAME, ANALYTICS_JS));
  }
}

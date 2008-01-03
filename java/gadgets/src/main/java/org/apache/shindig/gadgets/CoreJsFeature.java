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
 * Adds all core javascript dependencies that are needed by all gadgets.
 */
public class CoreJsFeature extends JsLibraryFeature {

  static {
    JsLibrary.register("core", "../../javascript/gadgets/core.js",
                       JsLibrary.Type.FILE, null, null);
    JsLibrary.register("util", "../../javascript/gadgets/util.js",
                       JsLibrary.Type.FILE, null, null);
    JsLibrary.register("prefs", "../../javascript/gadgets/prefs.js",
                       JsLibrary.Type.FILE, null, null);
    JsLibrary.register("io", "../../javascript/gadgets/io.js",
                       JsLibrary.Type.FILE, null, null);
    JsLibrary.register("legacy", "../../javascript/gadgets/legacy.js",
                       JsLibrary.Type.FILE, null, null);
  }

  @Override
  public void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) throws GadgetException {
    gadget.addJsLibrary(JsLibrary.file("core", "core"));
    gadget.addJsLibrary(JsLibrary.file("core", "util"));
    gadget.addJsLibrary(JsLibrary.file("core", "prefs"));
    gadget.addJsLibrary(JsLibrary.file("core", "io"));
    gadget.addJsLibrary(JsLibrary.file("core", "legacy"));
  }
}

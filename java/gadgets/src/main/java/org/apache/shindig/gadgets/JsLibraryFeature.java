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
 * All Javascript library dependencies belong here. To use, extend this class:
 * {@code}
 * class MyJsLib extends JsLibraryFeature {
 *   public void process(Gadget gadget, GadgetContext context,
         Map<String, String> params) {
       gadget.addJsLibrary(JsLibrary.file("hello.js"));
       gadget.addJsLibrary(JsLibrary.inline("hello('world!')"));
     }
 * }
 *
 */
public abstract class JsLibraryFeature implements GadgetFeature {

  public void prepare(GadgetView gadget, GadgetContext context,
      Map<String, String> params) {
  }

  public abstract void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) throws GadgetException;
}

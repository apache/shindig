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
 * Populates MODULE hangman variables in the substitution coordinator.
 */
class ModuleSubstituter implements GadgetFeatureFactory {
  private final static ModuleSubstituterFeature feature
      = new ModuleSubstituterFeature();
  /**
   * {@inheritDoc}
   */
  public GadgetFeature create() {
    return feature;
  }
}

class ModuleSubstituterFeature implements GadgetFeature {

  /**
   * {@inheritDoc}
   */
  public void prepare(GadgetView gadget, GadgetContext context,
      Map<String, String> params) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   */
  public void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) {
    gadget.getSubstitutions().addSubstitution(Substitutions.Type.MODULE, "ID",
        Integer.toString(gadget.getId().getModuleId()));

    if (context.getRenderingContext() == RenderingContext.GADGET) {
      String format = "gadgets.PrefStore_.setDefaultModuleId(%d);";
      String fmtStr = String.format(format, gadget.getId().getModuleId());
      gadget.addJsLibrary(JsLibrary.create(JsLibrary.Type.INLINE, fmtStr));
    }
  }
}

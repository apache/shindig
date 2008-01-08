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
 * Base interface providing Gadget Server's primary extensibility mechanism.
 *
 * During processing of a {@code Gadget}, a tree of {@code GadgetFeature}
 * objects is constructed based on the &lt;Require&gt; and &lt;Optional&gt;
 * tags declared in its {@code GadgetSpec}, and the dependencies registered
 * for these in {@code GadgetFeatureRegistry}.
 *
 * Each {@code GadgetFeature}'s prepare method is called first - potentially
 * in parallel with many others whose dependencies have also been satisfied.
 * Once this has completed, its process method is called. Prepare is useful
 * for async operations such as retrieval of a remote resource; all
 * {@code Gadget} modifications occur in process.
 *
 * To extend the Gadget Server's feature set, simply implement this interface
 * and register your class with {@code GadgetFeatureRegistry}, indicating
 * which other {@code GadgetFeature} features are needed before yours can
 * operate successfully.
 *
 * Each feature <i>must</i> be instantiable by a no-argument constructor,
 * and will <i>always</i> be instantiated this way. As such, it is recommended
 * not to define a constructor for a feature at all.
 */
public interface GadgetFeature {
  public void prepare(GadgetView gadget, GadgetContext context,
                      Map<String, String> params) throws GadgetException;
  public void process(Gadget gadget, GadgetContext context,
                      Map<String, String> params) throws GadgetException;
}

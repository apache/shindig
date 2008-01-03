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

import java.net.URI;

/**
 * A read-only view of the gadget.
 */
public interface GadgetView extends GadgetSpec {
  /**
   * A unique identifier for this gadget.
   */
  public interface ID {
    /**
     * @return The url of the gadget.
     */
    public URI getURI();

    /**
     * @return The unique identifier for this instance of the gadget.
     */
    public int getModuleId();

    /**
     * @return A string representing this gadget that can be used for caching.
     */
    public String getKey();
  }

  /**
   * @return This gadget's identifier.
   */
  public ID getId();

  /**
   * @return The substitution coordinator.
   */
  public Substitutions getSubstitutions();

  /**
   * @return The current message bundle for this rendering job.
   */
  public MessageBundle getCurrentMessageBundle();

}

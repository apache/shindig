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

/**
 * Contains flags that effect how a given {@code GadgetServer.processGadget}
 * call operates. Care should be taken with this class to ensure it doesn't
 * become a catch-all for unnecessary pieces of functionality.
 * 
 * This class's methods are designed to be overridden depending on the context
 * of the request. Its default implementations provide default values for
 * each supported flag.
 */
public class ProcessingOptions {
  /**
   * @return Whether or not to bypass caching behavior for the current request.
   */
  public boolean getIgnoreCache() {
    return false;
  }

  /**
   * Overrides javascript library processing by forcing the use of a fixed set
   * of libraries. This is mainly intended for use by parent sites that are
   * using the core library to fetch meta data for many gadgets and aggregating
   * the required libraries.
   * @return Libs path to use
   */
  public String getForcedJsLibs() {
    return null;
  }
}

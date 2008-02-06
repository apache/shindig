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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UserPrefs {
  /**
   * Convenience representation of an empty pref set.
   */
  public static final UserPrefs EMPTY = new UserPrefs();
  private Map<String, String> prefs;

  /**
   * @return An immutable reference to all prefs.
   */
  public Map<String, String> getPrefs() {
    return prefs;
  }

  /**
   * @param name The pref to fetch.
   * @return The pref specified by the given name.
   */
  public String getPref(String name) {
    return prefs.get(name);
  }

  /**
   * @param prefs
   */
  public UserPrefs(Map<String, String> prefs) {
    Map<String, String> tempMap = new HashMap<String, String>(prefs);
    this.prefs = Collections.unmodifiableMap(tempMap);
  }

  /**
   * Creates an empty user prefs object.
   */
  private UserPrefs() {
    // just use the empty map.
    this.prefs = Collections.emptyMap();
  }
}

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

/**
 * Name/value mapping of messages retrieved from a message bundle.
 */
public class MessageBundle {
  private Map<String, String> messages = new HashMap<String, String>();
  public static final MessageBundle EMPTY = new MessageBundle();

  /**
   * @return A read-only view of the message bundle.
   */
  public Map<String, String> getMessages() {
    return messages;
  }

  public MessageBundle(Map<String, String> messages) {
    Map<String, String> tempMap = new HashMap<String, String>(messages);
    this.messages = Collections.unmodifiableMap(tempMap);
  }

  @SuppressWarnings("unchecked")
  private MessageBundle() {
    this.messages = Collections.EMPTY_MAP;
  }
}

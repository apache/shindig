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
 * Exceptions for Gadget Spec parsing.
 */
public class SpecParserException extends GadgetException {
  private String message;

  /**
   * @param message
   */
  public SpecParserException(String message) {
    super(GadgetException.Code.MALFORMED_XML_DOCUMENT);
    this.message = message;
  }

  /**
   * @return The message for this exception.
   */
  @Override
  public String getMessage() {
    return message;
  }
}

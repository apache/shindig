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

public final class JsLibrary {
  private String sourceUrl;
  private String content;

  /**
   * @return The string representation of the JsLibrary object.
   */
  @Override
  public String toString() {
    // TODO: escape sourceUrl / content. Not a real security concern, but
    // an easy mistake to make.
    if (sourceUrl != null) {
      return "<script src=\"" + sourceUrl + "\"></script>";
    } else if (content != null) {
      return "<script>" + content + "</script>";
    } else {
      return "";
    }
  }

  /**
   * Creates a new JsLibrary from the specified source file.
   * @param file
   * @return The newly created JsLibrary object.
   */
  public static JsLibrary file(String file) {
    JsLibrary library = new JsLibrary();
    library.sourceUrl = file;
    return library;
  }
  /**
   * Creates a new JsLibrary from the specified code.
   * @param content
   * @return The newly created JsLibrary object.
   */
  public static JsLibrary inline(String content) {
    JsLibrary library = new JsLibrary();
    library.content = content;
    return library;
  }
}

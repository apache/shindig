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
package org.apache.shindig.gadgets.http;

import javax.servlet.http.HttpServletRequest;

import java.util.Locale;

public class BasicHttpContext {
  private String country;
  private String language;
  
  private static final String DEFAULT_VALUE = "all";
  
  public BasicHttpContext(HttpServletRequest request) {
    language = request.getParameter("lang");
    country = request.getParameter("country");
    if (language == null) {
      language = DEFAULT_VALUE;
    }
    if (country == null) {
      country = DEFAULT_VALUE;
    }
  }
  
  public String getCountry() {
    return country;
  }

  public String getLanguage() {
    return language;
  }
  
  public Locale getLocale() {
    return new Locale(language, country);
  }
}

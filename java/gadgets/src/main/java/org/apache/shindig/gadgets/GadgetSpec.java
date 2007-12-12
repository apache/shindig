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

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a Gadget specification.
 */
public interface GadgetSpec {
  public String getTitle();
  public URL getTitleURL();
  public String getDirectoryTitle();
  public String getDescription();
  public String getAuthor();
  public String getAuthorEmail();
  public String getScreenshot();
  public String getThumbnail();

  public static interface MessageBundle {
    public Locale getLocale();
    public URL getURL();
    public boolean isRightToLeft();
  }

  public List<MessageBundle> getMessageBundles();

  public static interface FeatureSpec {
    public String getName();
    public Map<String, String> getParams();
    public boolean isOptional();
  }

  public Map<String, FeatureSpec> getRequires();
  public List<String> getPreloads();

  public static interface Icon {
    public URL getURL();
    public String getMode();
    public String getType();
  }

  public List<Icon> getIcons();

  public static interface UserPref {

    public String getName();
    public String getDisplayName();
    public String getDefaultValue();
    public boolean isRequired();

    public static enum DataType {
      STRING, HIDDEN, BOOL, ENUM, LIST, NUMBER
    }

    public DataType getDataType();
  }

  public List<UserPref> getUserPrefs();

  public static enum ContentType {
      HTML, URL
  }

  public ContentType getContentType();
  public URL getContentHref();
  public String getContentData();

  /**
   * @return A copy of the spec. This is NOT the same as clone().
   */
  public GadgetSpec copy();
}
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages registration and retrieval of JavaScript libraries and injected
 * code that support many Gadget features. {@code JsLibraryFeature} classes are
 * the primary consumers of this functionality. Each such feature uses
 * the {@code register()} method to register and validate the gadget and
 * container JS code that supports the feature. This code may be deployed
 * as a separate file or as a Resource retrieved from the classpath.
 * The {@code JsLibraryFeature} will typically then inject JS code into a
 * {@code Gadget} using the {@code inline} and {@code file} methods.
 *
 * In addition, registration assists in serving of JS resources to type=URL
 * gadgets. Features requiring {@code inline} code are not supported by
 * such gadgets at present.
 */
public final class JsLibrary {
  // Server/client library registration
  // TODO: support base directory specification (as flag?)
  private static final Map<String, Entry> libraries;
  public static final String ALIAS_SEPARATOR = ":";

  static {
    libraries = new HashMap<String, Entry>();
  }

  /**
   * Contains gadget- and container-side JS loaded from registered resources.
   */
  private static final class Entry {
    private String gadgetJs;
    private String containerJs;
  }

  /**
   * Indicates how to load a given resource.
   */
  public static enum Type {
    FILE, RESOURCE
  }

  /**
   * Register a JS library. {@code alias} must be unique among all resources
   * registered in the server instance, and cannot contain character ":"
   * ({@code ALIAS_SEPARATOR}).
   * @param alias Identifier for the JS library
   * @param gadgetJsName Name of gadget-side JS resource, or null if not needed
   * @param gadgetJsType Type of gadget-side JS resource, or null if not needed
   * @param containerJsName Name of container JS resource, or null if not needed
   * @param containerJsType Type of container JS resource, or null if not needed
   */
  public static final void register(String alias,
                                    String gadgetJsName,
                                    Type gadgetJsType,
                                    String containerJsName,
                                    Type containerJsType) {
    if (alias.indexOf(ALIAS_SEPARATOR) >= 0) {
      throw new RuntimeException(
          String.format("Invalid JsLibrary alias %s - contains char %s",
                        alias,
                        ALIAS_SEPARATOR));
    }

    Entry entry = new Entry();
    entry.gadgetJs = loadData(gadgetJsName, gadgetJsType);
    entry.containerJs = loadData(containerJsName, containerJsType);
    libraries.put(alias, entry);
  }

  /**
   * Retrieves gadget-side JavaScript for the given {@code alias}, which
   * may optionally be a composite alias keying several pieces of code.
   * @param alias Identifier, possibly composite, of JS to load
   * @return JS code keyed by alias
   */
  public static final String getGadgetJs(String alias) {
    return getJs(alias, true);
  }

  /**
   * Retrieves container-side JavaScript for the given {@code alias}, which
   * may optionally be a composite alias keying several pieces of code.
   * @param alias Identifier, possibly composite, of JS to load
   * @return JS code keyed by alias
   */
  public static final String getContainerJs(String alias) {
    return getJs(alias, false);
  }

  private static final String getJs(String alias, boolean isGadget) {
    StringBuilder builder = new StringBuilder();
    String[] components = alias.split(ALIAS_SEPARATOR);
    for (String component : components) {
      Entry entry = libraries.get(component);
      if (entry == null) {
        return null;
      }
      if (isGadget) {
        builder.append(entry.gadgetJs);
      } else {
        builder.append(entry.containerJs);
      }
    }
    return builder.toString();
  }

  private static final String loadData(String name, Type type) {
    if (type == Type.FILE) {
      return loadFile(name);
    } else if (type == Type.RESOURCE) {
      return loadResource(name);
    }
    return null;
  }

  private static final String loadFile(String fileName) {
    if (fileName == null) {
      // Valid case: no JS needed for container or gadget for feature.
      // Blank String provided for this case.
      return "";
    }

    File file = new File(fileName);
    if (!file.exists()) {
      throw new RuntimeException(
          String.format("JsLibrary file missing: %s", fileName));
    }
    if (!file.isFile()) {
      throw new RuntimeException(
          String.format("JsLibrary is not a file: %s", fileName));
    }
    if (!file.canRead()) {
      throw new RuntimeException(
          String.format("JsLibrary cannot be read: %s", fileName));
    }

    FileInputStream fis = null;
    try {
      fis = new FileInputStream(fileName);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error reading file %s", fileName), e);
    }

    return loadFromInputStream(fis, fileName, "file");
  }

  private static final String loadResource(String name) {
     InputStream stream =
         JsLibrary.class.getClassLoader().getResourceAsStream(name);
     if (stream == null) {
       throw new RuntimeException(
           String.format("Could not find resource %s", name));
     }
     return loadFromInputStream(stream, name, "resource");
  }

  private static final String loadFromInputStream(InputStream is,
                                                  String name,
                                                  String type) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int read = 0;
    try {
      while ((read = is.read(buf)) > 0) {
        baos.write(buf, 0, read);
      }
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error reading %s %s", type, name), e);
    }

    String ret = null;
    try {
      ret = new String(baos.toByteArray(), "UTF8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unexpected error: UTF8 encoding unsupported");
    }
    return ret;
  }

  // Instance
  private String featureName;
  private URI sourceUri;
  private String alias;
  private String content;

  /**
   * @return The string representation of the JsLibrary object.
   */
  @Override
  public String toString() {
    // TODO: escape sourceUrl / content. Not a real security concern, but
    // an easy mistake to make.
    if (sourceUri != null) {
      return "<script src=\"" + sourceUri.toString() + "\"></script>";
    } else if (content != null) {
      return "<script><!--\n" + content + "\n--></script>";
    } else {
      return "";
    }
  }

  /**
   * Creates a new JsLibrary from the specified URI.
   * @param featureName Feature associated with this library
   * @param uriStr URI of the feature JS
   * @return The newly created {@code JsLibrary} object.
   * @throws GadgetException On programmer error, specifying invalid URL
   */
  public static JsLibrary uri(String featureName, String uriStr)
      throws GadgetException {
    URI uri = null;
    try {
      uri = new URI(uriStr);
    } catch (URISyntaxException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
    JsLibrary library = new JsLibrary(featureName);
    library.sourceUri = uri;
    // TODO: Consider scheme for retrieving and caching lib for later
    // retrieval by (randomly-generated) alias
    return library;
  }

  /**
   * Creates a new JsLibrary from the specified source file.
   * @param featureName Feature associated with this library
   * @param alias Alias for the file resource; must be pre-registered
   * @return The newly created {@code JsLibrary} object.
   * @throws GadgetException On programmer error in creating a feature
   */
  public static JsLibrary file(String featureName, String alias)
      throws GadgetException {
    Entry entry = libraries.get(alias);
    if (entry == null) {
      throw new GadgetException(
          GadgetException.Code.INTERNAL_SERVER_ERROR,
          String.format("Misconfigured feature, unknown alias %s", alias));
    }
    JsLibrary library = new JsLibrary(featureName);
    library.alias = alias;
    library.content = entry.gadgetJs;
    return library;
  }

  /**
   * Creates a new {@code JsLibrary} from the specified code.
   * @param featureName Feature associated with this library
   * @param content Ad hoc JavaScript to be inserted into a gadget
   * @return The newly created {@code JsLibrary} object.
   */
  public static JsLibrary inline(String featureName, String content) {
    JsLibrary library = new JsLibrary(featureName);
    library.content = content;
    // TODO: Consider scheme for writing this data to persistence store
    // for later retrieval, supporting more features via type=URL
    return library;
  }

  /**
   * @return An alias referencing the JS, if available; null otherwise.
   */
  public String getAlias() {
    return alias;
  }

  /**
   * @return Feature name applicable to this library.
   */
  public String getFeature() {
    return featureName;
  }

  /**
   * Constructs a {@code JsLibrary}
   * @param featureName Name of the feature associated with this library.
   */
  private JsLibrary(String featureName) {
    this.featureName = featureName;
  }
}

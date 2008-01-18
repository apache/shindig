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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Represents a javascript library, either as an external resource (url)
 * or as an inline script.
 * TODO: pull in url type libraries and treat them the same as file, resource,
 * or inline scripts.
 */
public final class JsLibrary {
  private Type type;
  public Type getType() {
    return type;
  }
  private String content;
  public String getContent() {
    return content;
  }

  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  @Override
  public String toString() {
    if (type == Type.URL) {
      return "<script src=\"" + content + "\"></script>";
    } else {
      return "<script><!--\n" + content + "\n--></script>";
    }
  }

  /**
   * Indicates how to load a given resource.
   */
  public static enum Type {
    FILE, RESOURCE, URL, INLINE;

    /**
     * Returns the type named by the given string.
     */
    public static Type parse(String name) {
      if (name.equals("file")) {
        return FILE;
      } else if (name.equals("url")) {
        return URL;
      } else if (name.equals("resource")) {
        return RESOURCE;
      } else {
        return INLINE;
      }
    }
  }

  /**
   * Creates a new js library.
   *
   * @param type If FILE or RESOURCE, the content will be loaded from disk.
   *     if URL or INLINE, the content will be handled the same as html <script>
   * @return The newly created library.
   */
  public static JsLibrary create(Type type, String content) {
    if (type == Type.FILE || type == Type.RESOURCE) {
      logger.info("Loading js from: " + content);
      content = loadData(content, type);
    }
    return new JsLibrary(type, content);
  }

  /**
   * Loads an external resource.
   * @param name
   * @param type
   * @return The contents of the file or resource named by @code name.
   */
  private static String loadData(String name, Type type) {
    if (type == Type.FILE) {
      return loadFile(name);
    } else if (type == Type.RESOURCE) {
      return loadResource(name);
    }
    return null;
  }

  /**
   * Loads a file
   * @param fileName
   * @return The contents of the file.
   */
  private static String loadFile(String fileName) {
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

  /**
   * Loads a resource.
   * @param name
   * @return The contents of the named resource.
   */
  private static String loadResource(String name) {
     InputStream stream =
         JsLibrary.class.getClassLoader().getResourceAsStream(name);
     if (stream == null) {
       throw new RuntimeException(
           String.format("Could not find resource %s", name));
     }
     return loadFromInputStream(stream, name, "resource");
  }

  /**
   * Loads content from the given input stream.
   * @param is
   * @param name
   * @param type
   * @return The contents of the stream.
   */
  private static String loadFromInputStream(InputStream is, String name,
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

  /**
   * @param type
   * @param content
   */
  private JsLibrary(Type type, String content) {
    this.type = type;
    this.content = content;
  }
}

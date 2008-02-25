/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import org.apache.shindig.util.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents a javascript library, either as an external resource (url)
 * or as an inline script.
 * TODO: pull in url type libraries and treat them the same as file, resource,
 * or inline scripts.
 */
public final class JsLibrary {
  private final Type type;
  public Type getType() {
    return type;
  }
  private final String content;
  public String getContent() {
    return content;
  }

  private final String debugContent;
  public String getDebugContent() {
    return debugContent;
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
  public enum Type {
    FILE, RESOURCE, URL, INLINE;

    /**
     * Returns the type named by the given string.
     */
    public static Type parse(String name) {
      if ("file".equals(name)) {
        return FILE;
      } else if ("url".equals(name)) {
        return URL;
      } else if ("resource".equals(name)) {
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
   * @param content If FILE or RESOURCE, we will also look for a file
   *     named file.opt.ext for every file.ext, and if present we will
   *     use that as the standard content and file.ext as the debug content.
   * @return The newly created library.
   */
  public static JsLibrary create(Type type, String content) {
    String optimizedContent = null;
    String debugContent;
    if (type == Type.FILE || type == Type.RESOURCE) {
      if (content.endsWith(".js")) {
        optimizedContent = loadData(
            content.substring(0, content.length() - 3) + ".opt.js", type);
      }
      debugContent = loadData(content, type);
      if (optimizedContent == null || optimizedContent.length() == 0) {
        optimizedContent = debugContent;
      }
    } else {
      debugContent = content;
      optimizedContent = content;
    }
    return new JsLibrary(type, optimizedContent, debugContent);
  }

  /**
   * Loads an external resource.
   * @param name
   * @param type
   * @return The contents of the file or resource named by @code name.
   */
  private static String loadData(String name, Type type) {
    logger.info("Loading js from: " + name + " type: " + type.toString());
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

    try {
      return ResourceLoader.getContent(file);
    } catch (IOException e) {
      logger.warning("Error reading file: " + fileName);
      return null;
    }
  }

  /**
   * Loads a resource.
   * @param name
   * @return The contents of the named resource.
   */
  private static String loadResource(String name) {
     try {
       return ResourceLoader.getContent(name);
     } catch (IOException e) {
       logger.warning("Could not find resource: " + name);
       return null;
     }
  }



  /**
   * @param type
   * @param content
   */
  private JsLibrary(Type type, String content, String debugContent) {
    this.type = type;
    this.content = content;
    this.debugContent = debugContent;
  }
}

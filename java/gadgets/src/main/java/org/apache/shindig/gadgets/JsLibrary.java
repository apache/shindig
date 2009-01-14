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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a javascript library, either as an external resource (url)
 * or as an inline script.
 */
public final class JsLibrary {
  private final Type type;
  public Type getType() {
    return type;
  }

  /**
   * The content of the library. May be optimized through minification or
   * other compression techniques. Use debugContent to get the unmodified
   * version.
   */
  private final String content;
  public String getContent() {
    return content;
  }

  /**
   * Unmodified content. May be identical to content if no optimized version of
   * the script exists.
   */
  private final String debugContent;
  public String getDebugContent() {
    return debugContent;
  }

  /**
   * The feature that this library belongs to; may be null;
   */
  private final String feature;
  public String getFeature() {
    return feature;
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
   * @param feature The name of the feature that this library was created for
   *     may be null.
   * @param fetcher Used to retrieve Type.URL; if null, Type.URL will not be
   *     kept as a url reference, otherwise the file will be fetched and treated
   *     as a FILE type.
   * @return The newly created library.
   * @throws GadgetException
   */
  public static JsLibrary create(Type type, String content, String feature,
      HttpFetcher fetcher) throws GadgetException {
    String optimizedContent = null;
    String debugContent;
    switch (type) {
      case FILE:
      case RESOURCE:
        if (content.endsWith(".js")) {
          optimizedContent = loadData(
              content.substring(0, content.length() - 3) + ".opt.js", type);
        }
        debugContent = loadData(content, type);
        if (optimizedContent == null || optimizedContent.length() == 0) {
          optimizedContent = debugContent;
        }
        break;
      case URL:
        if (fetcher == null) {
          debugContent = optimizedContent = content;
        } else {
          type = Type.FILE;
          debugContent = optimizedContent = loadDataFromUrl(content, fetcher);
        }
        break;
      default:
        debugContent = content;
        optimizedContent = content;
        break;
    }
    return new JsLibrary(feature, type, optimizedContent, debugContent);
  }

  /**
   * Loads an external resource.
   * @param name
   * @param type
   * @return The contents of the file or resource named by @code name.
   */
  private static String loadData(String name, Type type) {
    logger.fine("Loading js from: " + name + " type: " + type.toString());
    if (type == Type.FILE) {
      return loadFile(name);
    } else if (type == Type.RESOURCE) {
      return loadResource(name);
    }
    return null;
  }

  /**
   * Retrieves js content from the given url.
   *
   * @param url
   * @param fetcher
   * @return The contents of the JS file, or null if it can't be fetched.
   * @throws GadgetException
   */
  private static String loadDataFromUrl(String url, HttpFetcher fetcher) throws GadgetException {
    try {
      logger.info("Attempting to load js from: " + url);
      Uri uri = Uri.parse(url);
      HttpRequest request = new HttpRequest(uri);
      HttpResponse response = fetcher.fetch(request);
      if (response.getHttpStatusCode() == HttpResponse.SC_OK) {
        return response.getResponseAsString();
      } else {
        logger.warning("Unable to retrieve remote library from " + url);
        return null;
      }
    } catch (IllegalArgumentException e) {
      logger.log(Level.WARNING, "Malformed URL: " + url, e);
      return null;
    }
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
      logger.warning("File not found: " + fileName);
      return null;
    }
    if (!file.isFile()) {
      logger.warning("JsLibrary is not a file: " + fileName);
      return null;
    }
    if (!file.canRead()) {
      logger.warning("JsLibrary cannot be read: " + fileName);
      return null;
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

  @Override
  public int hashCode() {
    return content.hashCode() + type.hashCode();
  }

  @Override
  public boolean equals(Object rhs) {
    if (rhs == this) {
      return true;
    }
    if (rhs instanceof JsLibrary) {
      JsLibrary lib = (JsLibrary)rhs;
      return content.equals(lib.content) && type.equals(lib.type);
    }
    return false;
  }

  /**
   * @param feature
   * @param type
   * @param content
   * @param debugContent
   */
  private JsLibrary(String feature, Type type, String content,
      String debugContent) {
    this.feature = feature;
    this.type = type;
    this.content = content;
    this.debugContent = debugContent;
  }
}

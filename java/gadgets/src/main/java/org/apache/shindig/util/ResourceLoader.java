/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.shindig.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Handles loading contents from resource and file system files.
 */
public class ResourceLoader {

  private static final Logger logger
    = Logger.getLogger("org.apache.shindig.util");

  /**
   * Reads the contents of a resource as a string.
   *
   * @param resource
   * @return Contents of the resource.
   * @throws IOException
   */
  public static String getContent(String resource)
      throws IOException, FileNotFoundException {
    ClassLoader cl = ResourceLoader.class.getClassLoader();
    InputStream is = cl.getResourceAsStream(resource);
    if (is == null) {
      throw new FileNotFoundException("Can not locate resource: " + resource);
    }

    return InputStreamConsumer.readToString(is);
  }

  /**
   * Loads resources recursively and returns the contents of all matching
   * files.
   *
   * @param paths The base paths to look for the desired files.
   * @param file The name of the files to actually return content for.
   * @return A map of file paths to their contents.
   *
   * @throws IOException
   */
  public static Map<String, String> getContent(String[] paths, String file)
      throws IOException {
    ClassLoader cl = ResourceLoader.class.getClassLoader();
    Map<String, String> out = new HashMap<String, String>();

    for (String path : paths) {
      logger.info("Looking for " + file + " in " + path);
      Enumeration<URL> mappedResources = cl.getResources(path);
      while (mappedResources.hasMoreElements()) {
        URL resourceUrl =  mappedResources.nextElement();
        if ("file".equals(resourceUrl.getProtocol())) {
          try {
            File f = new File(resourceUrl.toURI());
            out.put(path, getContent(f));
          } catch (URISyntaxException e) {
            logger.warning("Unable to load file " + resourceUrl.toString());
          }
        } else {
          URLConnection urlConnection = resourceUrl.openConnection();
          List<String> fullPaths = new LinkedList<String>();
          if (urlConnection instanceof JarURLConnection) {
            JarURLConnection jarUrlConn = (JarURLConnection)urlConnection;
            JarFile jar = jarUrlConn.getJarFile();

            Enumeration<JarEntry> jarEntries = jar.entries();
            while (jarEntries.hasMoreElements()) {
              JarEntry jarEntry =  jarEntries.nextElement();
              if (jarEntry.getName().startsWith(path) &&
                  jarEntry.getName().endsWith(file)) {
                fullPaths.add(jarEntry.getName());
              }
            }
          }
          for (String res : fullPaths) {
            out.put(res, getContent(res));
          }
        }
      }
    }
    return Collections.unmodifiableMap(out);
  }

  /**
   * @param file
   * @return The contents of the file (assumed to be UTF-8).
   * @throws IOException
   */
  public static String getContent(File file) throws IOException {
    return InputStreamConsumer.readToString(new FileInputStream(file));
  }
}

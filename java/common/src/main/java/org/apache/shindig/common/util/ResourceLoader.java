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
package org.apache.shindig.common.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles loading contents from resource and file system files.
 */
public final class ResourceLoader {
  public static final String RESOURCE_PREFIX = "res://";
  public static final String FILE_PREFIX = "file://";

  private ResourceLoader() {}
  /**
   * Opens a given path as either a resource or a file, depending on the path
   * name.
   *
   * If path starts with res://, we interpret it as a resource.
   * If path starts with file://, or path has no prefix, we interpret it as a file.
   * @param path
   * @return The opened input stream
   */
  public static InputStream open(String path) throws IOException {
    if (path.startsWith(RESOURCE_PREFIX)) {
      return openResource(path.substring(RESOURCE_PREFIX.length()));
    } else if (path.startsWith(FILE_PREFIX)) {
      path = path.substring(FILE_PREFIX.length());
    }
    File file = new File(path);
    return new FileInputStream(file);
  }

  /**
   * Opens a resource
   * @param resource
   * @return An input stream for the given named resource
   * @throws FileNotFoundException
   */
  public static InputStream openResource(String resource) throws FileNotFoundException  {
    ClassLoader cl = ResourceLoader.class.getClassLoader();
    try {
      return openResource(cl, resource);
    } catch (FileNotFoundException e) {
      // If we cannot find the resource using the current classes class loader
      // try the current threads
      cl = Thread.currentThread().getContextClassLoader();
      return openResource(cl, resource);
    }
  }

  /**
   * Opens a resource
   * @param cl The classloader to use to find the resource
   * @param resource The resource to open
   * @return An input stream for the given named resource
   * @throws FileNotFoundException
   */

  private static InputStream openResource(ClassLoader cl, String resource)
      throws FileNotFoundException {
    InputStream is = cl.getResourceAsStream(resource.trim());
    if (is == null) {
      throw new FileNotFoundException("Can not locate resource: " + resource);
    }
    return is;
  }

  /**
   * Reads the contents of a resource as a string.
   *
   * @param resource
   * @return Contents of the resource.
   * @throws IOException
   */
  public static String getContent(String resource) throws IOException {
    InputStream is = openResource(resource);
    try {
      return IOUtils.toString(is, "UTF-8");
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * @param file
   * @return The contents of the file (assumed to be UTF-8).
   * @throws IOException
   */
  public static String getContent(File file) throws IOException {
    InputStream is = new FileInputStream(file);
    try {
      return IOUtils.toString(is, "UTF-8");
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}

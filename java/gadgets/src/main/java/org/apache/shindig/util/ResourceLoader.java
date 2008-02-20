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
   * @param file
   * @return The contents of the file (assumed to be UTF-8).
   * @throws IOException
   */
  public static String getContent(File file) throws IOException {
    return InputStreamConsumer.readToString(new FileInputStream(file));
  }
}

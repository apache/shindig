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
package org.apache.shindig.gadgets.features;

import org.apache.shindig.common.util.ResourceLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * DefaultFile delegate feature file interface to java.io.File object
 */
public class DefaultFeatureFile implements FeatureFile {

  protected final File wrappedFile;

  public DefaultFeatureFile(String path) {
    this.wrappedFile = new File(path);
  }

  protected DefaultFeatureFile(File wrappedFile) {
    this.wrappedFile = wrappedFile;
  }

  protected DefaultFeatureFile createFile(File wrappedFile) {
    return new DefaultFeatureFile(wrappedFile);
  }

  public InputStream getInputStream() throws IOException {
    return new FileInputStream(wrappedFile);
  }

  public boolean canRead() {
    return wrappedFile.canRead();
  }

  public boolean exists() {
    return wrappedFile.exists();
  }

  public String getName() {
    return wrappedFile.getName();
  }

  public String getPath() {
    return wrappedFile.getPath();
  }

  public String getAbsolutePath() {
    return wrappedFile.getAbsolutePath();
  }

  public boolean isDirectory() {
    return wrappedFile.isDirectory();
  }

  public FeatureFile[] listFiles() {
    File[] wrappedFiles = wrappedFile.listFiles();
    if (wrappedFiles == null) {
      return null;
    }
    FeatureFile[] files = new FeatureFile[wrappedFiles.length];
    for (int i = 0; i < wrappedFiles.length; i++) {
      files[i] = createFile(wrappedFiles[i]);
    }
    return files;
  }

  public URI toURI() {
    return wrappedFile.toURI();
  }

  public String getContent() throws IOException {
    return ResourceLoader.getContent(wrappedFile);
  }

  public long lastModified() {
    return wrappedFile.lastModified();
  }
}

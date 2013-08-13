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
package org.apache.shindig.protocol.multipart;

import org.apache.commons.fileupload.FileItem;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of FormDataItem using Apache commons FileItem.
 */
class CommonsFormDataItem implements FormDataItem {
  private final FileItem fileItem;

  CommonsFormDataItem(FileItem fileItem) {
    this.fileItem = fileItem;
  }

  public byte[] get() {
    return fileItem.get();
  }

  public String getAsString() {
    return fileItem.getString();
  }

  public String getContentType() {
    return fileItem.getContentType();
  }

  public String getFieldName() {
    return fileItem.getFieldName();
  }

  public InputStream getInputStream() throws IOException {
    return fileItem.getInputStream();
  }

  public String getName() {
    return fileItem.getName();
  }

  public long getSize() {
    return fileItem.getSize();
  }

  public boolean isFormField() {
    return fileItem.isFormField();
  }
}

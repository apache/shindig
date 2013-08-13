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

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface to represent an field item in multipart/form-data.
 */
public interface FormDataItem {

  /**
   * Returns the Content type of the field item.
   *
   * @return content type
   */
  String getContentType();

  /**
   * The size of the content stored in this field item.
   *
   * @return size of the content
   */
  long getSize();

  /**
   * Returns an InputStream from which the content of the field item can be
   * read.
   *
   * @return InputStream to the content of the field item.
   * @throws IOException
   */
  InputStream getInputStream() throws IOException;

  /**
   * Returns the content of the field item.
   *
   * @return content of the field item
   */
  byte[] get();

  /**
   * Returns the content of the field item as text.
   *
   * @return content of the field item as text
   */
  String getAsString();

  /**
   * Name of the uploaded file, if the item represents file upload.
   * This will be only valid when {@link #isFormField()} returns false.
   *
   * @return name of the uploaded file
   */
  String getName();

  /**
   * Field name of this field item. Can be used to identify a field by name but
   * as per RFC this need not be unique.
   *
   * @return name of the field
   */
  String getFieldName();

  /**
   * Used to identify if the field item represents a file upload or a regular
   * form field.
   *
   * @return true if it is a regular form field
   */
  boolean isFormField();
}

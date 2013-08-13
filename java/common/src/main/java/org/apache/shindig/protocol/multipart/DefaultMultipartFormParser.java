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
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.IOException;
import java.net.UnknownServiceException;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;

/**
 * Implementation of MultipartFormParser using Apache Commons file upload.
 */
public class DefaultMultipartFormParser implements MultipartFormParser {
  private static final String MULTIPART = "multipart/";

  public Collection<FormDataItem> parse(HttpServletRequest servletRequest)
      throws IOException  {
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);

    try {
      @SuppressWarnings("unchecked")
      List<FileItem> fileItems = upload.parseRequest(servletRequest);
      return convertToFormData(fileItems);
    } catch (FileUploadException e) {
      UnknownServiceException use = new UnknownServiceException("File upload error.");
      use.initCause(e);
      throw use;
    }
  }

  private Collection<FormDataItem> convertToFormData(List<FileItem> fileItems) {
    List<FormDataItem> formDataItems =
        Lists.newArrayListWithCapacity(fileItems.size());
    for (FileItem item : fileItems) {
      formDataItems.add(new CommonsFormDataItem(item));
    }

    return formDataItems;
  }

  public boolean isMultipartContent(HttpServletRequest request) {
    if (!"POST".equals(request.getMethod())) {
      return false;
    }
    String contentType = request.getContentType();
    if (contentType == null) {
      return false;
    }
    return contentType.toLowerCase().startsWith(MULTIPART);
  }
}

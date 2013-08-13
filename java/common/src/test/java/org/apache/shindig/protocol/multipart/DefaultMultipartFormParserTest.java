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

import org.apache.shindig.common.testing.FakeHttpServletRequest;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultMultipartFormParserTest extends Assert {

  private static final String REQUEST_FIELDNAME = "request";
  private static final String REQUEST_DATA = "{name: 'HelloWorld'}";

  private static final String ALBUM_IMAGE_FIELDNAME = "album-image";
  private static final String ALBUM_IMAGE_FILENAME = "album-image.jpg";
  private static final String ALBUM_IMAGE_DATA = "album image data";
  private static final String ALBUM_IMAGE_TYPE = "image/jpeg";

  private static final String PROFILE_IMAGE_FIELDNAME = "profile-image";
  private static final String PROFILE_IMAGE_FILENAME = "profile-image.jpg";
  private static final String PROFILE_IMAGE_DATA = "profile image data";
  private static final String PROFILE_IMAGE_TYPE = "image/png";

  private MultipartFormParser multipartFormParser;
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    multipartFormParser = new DefaultMultipartFormParser();
  }

  /**
   * Test that requests must be both POST and have a multipart
   * content type.
   */
  @Test
  public void testIsMultipartContent() {
    FakeHttpServletRequest request = new FakeHttpServletRequest();

    request.setMethod("GET");
    assertFalse(multipartFormParser.isMultipartContent(request));

    request.setMethod("POST");
    assertFalse(multipartFormParser.isMultipartContent(request));

    request.setContentType("multipart/form-data");
    assertTrue(multipartFormParser.isMultipartContent(request));

    request.setMethod("GET");
    assertFalse(multipartFormParser.isMultipartContent(request));
}

  /**
   * Helper class to create the multipart/form-data body of the POST request.
   */
  private static class MultipartFormBuilder {
    private final String boundary;
    private final StringBuilder packet = new StringBuilder();
    private static final String BOUNDARY = "--abcdefgh";

    public MultipartFormBuilder() {
      this(BOUNDARY);
    }

    public MultipartFormBuilder(String boundary) {
      this.boundary = boundary;
    }

    public String getContentType() {
      return "multipart/form-data; boundary=" + boundary;
    }

    public byte[] build() {
      write("--");
      write(boundary);
      write("--");
      return packet.toString().getBytes();
    }

    public void addFileItem(String fieldName, String fileName, String content,
        String contentType) {
      writeBoundary();

      write("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" +
          fileName + '\"');
      write("\r\n");
      write("Content-Type: " + contentType);
      write("\r\n\r\n");
      write(content);
      write("\r\n");
    }

    public void addFormField(String fieldName, String content) {
      addFormField(fieldName, content, null);
    }

    public void addFormField(String fieldName, String content, String contentType) {
      writeBoundary();

      write("Content-Disposition: form-data; name=\"" + fieldName + '\"');
      if (contentType != null) {
        write("\r\n");
        write("Content-Type: " + contentType);
      }
      write("\r\n\r\n");
      write(content);
      write("\r\n");
    }

    private void writeBoundary() {
      write("--");
      write(boundary);
      write("\r\n");
    }

    private void write(String content) {
      packet.append(content);
    }
  }

  private void setupRequest(byte[] postData, String contentType) throws IOException {
    FakeHttpServletRequest fakeReq = new FakeHttpServletRequest("/social/rest", "", "");
    fakeReq.setPostData(postData);
    fakeReq.setContentType(contentType);
    request = fakeReq;
  }

  @Test
  public void testSingleFileItem() throws Exception {
    MultipartFormBuilder builder = new MultipartFormBuilder();
    builder.addFileItem(ALBUM_IMAGE_FIELDNAME, ALBUM_IMAGE_FILENAME, ALBUM_IMAGE_DATA,
        ALBUM_IMAGE_TYPE);
    setupRequest(builder.build(), builder.getContentType());

    List<FormDataItem> formItems =
      Lists.newArrayList(multipartFormParser.parse(request));

    assertEquals(1, formItems.size());
    FormDataItem formItem = formItems.get(0);
    assertFalse(formItem.isFormField());
    assertEquals(ALBUM_IMAGE_FIELDNAME, formItem.getFieldName());
    assertEquals(ALBUM_IMAGE_FILENAME, formItem.getName());
    assertEquals(ALBUM_IMAGE_TYPE, formItem.getContentType());
    assertEquals(ALBUM_IMAGE_DATA, new String(formItem.get()));
  }

  @Test
  public void testSingleRequest() throws Exception {
    MultipartFormBuilder builder = new MultipartFormBuilder();
    builder.addFormField(REQUEST_FIELDNAME, REQUEST_DATA);
    setupRequest(builder.build(), builder.getContentType());

    List<FormDataItem> formItems =
      Lists.newArrayList(multipartFormParser.parse(request));

    assertEquals(1, formItems.size());
    FormDataItem formItem = formItems.get(0);
    assertTrue(formItem.isFormField());
    assertEquals(REQUEST_FIELDNAME, formItem.getFieldName());
    assertEquals(REQUEST_DATA, new String(formItem.get()));
  }

  @Test
  public void testSingleFileItemAndRequest() throws Exception {
    MultipartFormBuilder builder = new MultipartFormBuilder();
    builder.addFileItem(ALBUM_IMAGE_FIELDNAME, ALBUM_IMAGE_FILENAME, ALBUM_IMAGE_DATA,
        ALBUM_IMAGE_TYPE);
    builder.addFormField(REQUEST_FIELDNAME, REQUEST_DATA);
    setupRequest(builder.build(), builder.getContentType());

    List<FormDataItem> formItems =
      Lists.newArrayList(multipartFormParser.parse(request));

    assertEquals(2, formItems.size());
    FormDataItem formItem = formItems.get(0);
    assertFalse(formItem.isFormField());
    assertEquals(ALBUM_IMAGE_FIELDNAME, formItem.getFieldName());
    assertEquals(ALBUM_IMAGE_FILENAME, formItem.getName());
    assertEquals(ALBUM_IMAGE_TYPE, formItem.getContentType());
    assertEquals(ALBUM_IMAGE_DATA, new String(formItem.get()));

    formItem = formItems.get(1);
    assertTrue(formItem.isFormField());
    assertEquals(REQUEST_FIELDNAME, formItem.getFieldName());
    assertEquals(REQUEST_DATA, new String(formItem.get()));
  }

  @Test
  public void testMultipleFileItemAndRequest() throws Exception {
    MultipartFormBuilder builder = new MultipartFormBuilder();
    builder.addFileItem(ALBUM_IMAGE_FIELDNAME, ALBUM_IMAGE_FILENAME, ALBUM_IMAGE_DATA,
        ALBUM_IMAGE_TYPE);
    builder.addFormField(REQUEST_FIELDNAME, REQUEST_DATA);
    builder.addFileItem(PROFILE_IMAGE_FIELDNAME, PROFILE_IMAGE_FILENAME, PROFILE_IMAGE_DATA,
        PROFILE_IMAGE_TYPE);
    setupRequest(builder.build(), builder.getContentType());

    List<FormDataItem> formItems =
      Lists.newArrayList(multipartFormParser.parse(request));

    assertEquals(3, formItems.size());
    FormDataItem formItem = formItems.get(0);
    assertFalse(formItem.isFormField());
    assertEquals(ALBUM_IMAGE_FIELDNAME, formItem.getFieldName());
    assertEquals(ALBUM_IMAGE_FILENAME, formItem.getName());
    assertEquals(ALBUM_IMAGE_TYPE, formItem.getContentType());
    assertEquals(ALBUM_IMAGE_DATA, new String(formItem.get()));

    formItem = formItems.get(1);
    assertTrue(formItem.isFormField());
    assertEquals(REQUEST_FIELDNAME, formItem.getFieldName());
    assertEquals(REQUEST_DATA, new String(formItem.get()));

    formItem = formItems.get(2);
    assertFalse(formItem.isFormField());
    assertEquals(PROFILE_IMAGE_FIELDNAME, formItem.getFieldName());
    assertEquals(PROFILE_IMAGE_FILENAME, formItem.getName());
    assertEquals(PROFILE_IMAGE_TYPE, formItem.getContentType());
    assertEquals(PROFILE_IMAGE_DATA, new String(formItem.get()));
  }
}

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
package org.apache.shindig.common.servlet;

import org.apache.shindig.common.util.DateUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Captures output from an HttpServletResponse.
 */
public class HttpServletResponseRecorder extends HttpServletResponseWrapper {
  protected final ByteArrayOutputStream baos = new ByteArrayOutputStream();
  private PrintWriter writer;
  private final Map<String, String> headers = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
  private int httpStatusCode = HttpServletResponse.SC_OK;
  private String encoding = Charset.defaultCharset().name();

  public HttpServletResponseRecorder(HttpServletResponse response) {
    super(response);
  }

  public String getResponseAsString() {
    try {
      getWriter().close();
      return new String(baos.toByteArray(), "UTF-8");
    } catch (IOException e) {
      return null;
    }
  }

  public byte[] getResponseAsBytes() {
    return baos.toByteArray();
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  public String getHeader(String name) {
    return headers.get(name);
  }

  @Override
  public PrintWriter getWriter() throws UnsupportedEncodingException {
    if (writer == null) {
      writer = new PrintWriter(new OutputStreamWriter(baos, encoding));
    }
    return writer;
  }

  @Override
  public ServletOutputStream getOutputStream() {
    return new ServletOutputStream() {
      @Override
      public void write(int b) {
        baos.write(b);
      }
    };
  }

  @Override
  public void addHeader(String name, String value) {
    headers.put(name, value);
  }

  @Override
  public void setHeader(String name, String value) {
    addHeader(name, value);
  }

  @Override
  public void addDateHeader(String name, long date) {
    headers.put(name, DateUtil.formatRfc1123Date(date));
  }

  @Override
  public void setDateHeader(String name, long date) {
    addDateHeader(name, date);
  }

  @Override
  public void setStatus(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

  @Override
  public void sendError(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

  @Override
  public void sendRedirect(String location) {
    setStatus(302);
    setHeader("Location", location);
  }

  @Override
  public void setStatus(int httpStatusCode, String msg)  {
    try {
      getWriter().write(msg);
      this.httpStatusCode = httpStatusCode;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendError(int httpStatusCode, String msg) {
    try {
      getWriter().write(msg);
      this.httpStatusCode = httpStatusCode;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setContentType(String type) {
    headers.put("Content-Type", type);
  }

  @Override
  public String getContentType() {
    return headers.get("Content-Type");
  }

  @Override
  public void setCharacterEncoding(String encoding) {
    this.encoding = encoding;
  }

  @Override
  public String getCharacterEncoding() {
    return encoding;
  }
}

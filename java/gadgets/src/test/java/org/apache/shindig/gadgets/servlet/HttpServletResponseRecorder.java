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
package org.apache.shindig.gadgets.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Captures output from an HttpServletResponse.
 */
public class HttpServletResponseRecorder extends HttpServletResponseWrapper {
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
  private final PrintWriter writer = new PrintWriter(baos);
  private int httpStatusCode = 200;

  public HttpServletResponseRecorder(HttpServletResponse response) {
    super(response);
  }

  public String getResponseAsString() {
    try {
      writer.close();
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

  @Override
  public PrintWriter getWriter() {
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
  public void setStatus(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

  @Override
  public void sendError(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

  @Override
  public void setStatus(int httpStatusCode, String msg) {
    writer.write(msg);
    this.httpStatusCode = httpStatusCode;
  }

  @Override
  public void sendError(int httpStatusCode, String msg) {
    writer.write(msg);
    this.httpStatusCode = httpStatusCode;
  }
}

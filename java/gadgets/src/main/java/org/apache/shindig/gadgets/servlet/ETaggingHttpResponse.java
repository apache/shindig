/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.servlet;

import org.apache.http.util.ByteArrayBuffer;
import org.apache.shindig.common.util.HashUtil;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * A class for generating and managing ETags for improved caching.
 *
 * Objects of this class have two modes: batching mode and streaming mode.
 * 
 * In batching mode, the response body is stored in a buffer and, at the end,
 * its ETag is calculated and everything is written to the output at once.
 * 
 * In streaming mode, however, the response body is output as it's received
 * from the servlet, and no ETag is calculated.
 */
public class ETaggingHttpResponse extends HttpServletResponseWrapper {

  public static final String RESPONSE_HEADER = "ETag";
  public static final String REQUEST_HEADER = "If-None-Match";

  private final HttpServletRequest request;
  private final BufferServletOutputStream stream;
  private ServletOutputStream originalStream;
  private PrintWriter writer;
  private boolean batching;

  public ETaggingHttpResponse(HttpServletRequest request, HttpServletResponse response) {
    super(response);
    this.request = request;
    this.stream = new BufferServletOutputStream();
    this.writer = null;
    this.batching = true;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (originalStream == null) {
      originalStream = getResponse().getOutputStream();
    }
    if (isCommitted()) {
      batching = false;
    }
    return stream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (writer == null) {
      writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
    }
    return writer;
  }

  /**
   * {@inheritDoc}
   * 
   * The response object is also switched to streaming mode.
   */
  @Override
  public void flushBuffer() throws IOException {
    writeToOutput();
    getResponse().flushBuffer();
    batching = false;
  }

  /**
   * {@inheritDoc}
   *
   * The response object is switched to batching mode if the response has not
   * been committed yet.
   */
  @Override
  public void reset() {
    super.reset();
    writer = null;
    stream.reset();
    batching = !isCommitted();
  }

  /**
   * {@inheritDoc}
   *
   * The response object is switched to batching mode if the response has not
   * been committed yet.
   */
  @Override
  public void resetBuffer() {
    super.resetBuffer();
    writer = null;
    stream.reset();
    batching = !isCommitted();
  }

  /**
   * Switches this response object to streaming mode.
   *
   * The current buffer is written to the output, as are any subsequent writes.
   *
   * @throws IOException If flushing the buffer produced an exception.
   */
  public void startStreaming() throws IOException {
    batching = false;
    writeToOutput();
  }

  /**
   * Outputs the response body.
   * 
   * In batching mode, it outputs the full contents of the buffer with its
   * corresponding ETag, or a NOT_MODIFIED response if the ETag matches the
   * request's "If-None-Match" header.
   *
   * In streaming mode, output is only generated if the buffer is not empty;
   * in that case, the buffer is flushed to the output.
   *
   * @throws IOException If there was a problem writing to the output.
   */
  void writeToOutput() throws IOException {
    if (writer != null) {
      writer.flush();
    }
    byte[] bytes = stream.getBuffer().toByteArray();
    if (batching) {
      String etag = stream.getContentHash();
      String reqEtag = request.getHeader(REQUEST_HEADER);
      ((HttpServletResponse) getResponse()).setHeader(RESPONSE_HEADER, etag);
      if (etag.equals(reqEtag)) {
        emitETagMatchedResult();
      } else {
        getResponse().setContentLength(bytes.length);
        getResponse().getOutputStream().write(bytes);
      }
    } else if (bytes.length != 0) {
      originalStream.write(bytes);
      stream.getBuffer().clear();
    }
  }

  protected void emitETagMatchedResult() throws IOException {
    ((HttpServletResponse) getResponse()).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    getResponse().setContentLength(0);
  }

  /**
   * A ServletOutputStream that stores the data in a byte array buffer.
   */
  private class BufferServletOutputStream extends ServletOutputStream {
    private static final int BUFFER_INITIAL_CAPACITY = 16384;

    private MessageDigest digest = HashUtil.getMessageDigest();
    private ByteArrayBuffer buffer = new ByteArrayBuffer(BUFFER_INITIAL_CAPACITY);

    @Override
    public void write(int b) throws IOException {
      if (batching) {
        updateDigest(b);
        buffer.append(b);
      } else {
        originalStream.write(b);
      }
    }

    public ByteArrayBuffer getBuffer() {
      return buffer;
    }

    public void reset() {
      buffer.clear();
      digest.reset();
    }

    public String getContentHash() {
      String hash = HashUtil.bytesToHex(digest.digest());
      digest = null;
      return hash;
    }

    private void updateDigest(int b) {
      if (digest == null) {
        digest = HashUtil.getMessageDigest();
      }
      digest.update((byte) b);
    }
  }
}

package org.apache.shindig.social.core.oauth;

import org.apache.http.util.ByteArrayBuffer;

import javax.servlet.ServletOutputStream;

/**
 * Used to capture the raw request response provided by servlet
 * 
 */
public class MockServletOutputStream extends ServletOutputStream {

  private ByteArrayBuffer buffer = new ByteArrayBuffer(1024);

  @Override
  public void write(int b) {
    buffer.append(b);
  }

  public byte[] getBuffer() {
    return buffer.toByteArray();
  }
}
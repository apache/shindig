package org.apache.shindig.social.abdera;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

import org.apache.abdera.protocol.server.context.SimpleResponseContext;
import org.apache.abdera.util.EntityTag;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;


/**
 * A ResponseContext implementation that use a generic writer.
 */
public class RawResponseContext 
  extends SimpleResponseContext {
  
  private InputStream in;

  public RawResponseContext(
    InputStream in, 
    EntityTag etag, 
    int status) {
      this.in = in;
      this.status = status;
      setEntityTag(etag);
  }
  
  public RawResponseContext(
    InputStream in,  
    int status) {
      this.in = in;
      this.status = status;
  }
  
  public RawResponseContext(
    InputStream in, 
    Date lastmodified, 
    int status) {
      this.in = in;
      this.status = status;
      setLastModified(lastmodified);
  }

  public RawResponseContext(String content, int status) throws UnsupportedEncodingException {
    this.in = new ByteArrayInputStream(content.getBytes("UTF-8"));
    this.status = status;
  }

  public boolean hasEntity() {
    return in != null;
  }

  public void writeTo(
    OutputStream out) 
      throws IOException {
    if (hasEntity()) {
      if (in != null) {
        IOUtils.copy(in, out);
      }
    }
  } 

  protected void writeEntity(
    Writer out) 
      throws IOException {
    if (in != null) {
      IOUtils.copy(in, out);
    }
  }
}

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
package org.apache.shindig.social.abdera.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.abdera.model.Base;
import org.apache.abdera.util.AbstractNamedWriter;
import org.apache.abdera.util.AbstractWriterOptions;
import org.apache.abdera.writer.NamedWriter;
import org.apache.abdera.writer.WriterOptions;

/*
 * TODO: This file is copied and modified from Abdera code as we needed
 * functionality different from the Abdera Json writer code base.
 * This file definitely needs cleanup and heavy refactoring
 */
public class JSONWriter
  extends AbstractNamedWriter
  implements NamedWriter {

  public static final String NAME = "json";

  public static final String[] FORMATS = {
    "application/json",
  };

  public JSONWriter() {
    super(NAME,FORMATS);
  }

  @Override
  protected WriterOptions initDefaultWriterOptions() {
    return new AbstractWriterOptions() {};
  }

  @Override
  public String getName() {
    return NAME;
  }

  public Object write(Base base, WriterOptions options) throws IOException {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      writeTo(base,out,options);
      return new String(out.toByteArray(),options.getCharset());
    } catch (IOException i) {
      throw i;
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
  }

  public void writeTo(Base base, OutputStream out, WriterOptions options) throws IOException {
    writeTo(base,new OutputStreamWriter(out,options.getCharset()),options);
  }

  public void writeTo(Base base, java.io.Writer out, WriterOptions options) throws IOException {
    try {
      JSONUtil.toJson(base, out);
      if (options.getAutoClose()) out.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException(e.getMessage());
    }
  }

}

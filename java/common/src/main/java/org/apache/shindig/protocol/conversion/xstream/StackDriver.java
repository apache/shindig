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
package org.apache.shindig.protocol.conversion.xstream;

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

/**
 * A StackDriver wraps other forms of Drivers and updates a WriterStack with the
 * path into the writer hierarchy.
 */
public class StackDriver implements HierarchicalStreamDriver {

  /**
   * The parent Stream Driver that does the work.
   */
  private HierarchicalStreamDriver parent;
  /**
   * A Writer Stack implementation that records where the writer is.
   */
  private WriterStack writerStack;
  private Map<String, NamespaceSet> namespaces;

  /**
   * Create a {@link StackDriver}, wrapping a {@link HierarchicalStreamDriver}
   * and updating a {@link WriterStack}.
   *
   * @param parent
   *          the driver to be wrapped
   * @param writerStack
   *          the thread safe writer stack that records where the writer is.
   * @param map
   */
  public StackDriver(HierarchicalStreamDriver parent, WriterStack writerStack, Map<String, NamespaceSet> map) {
    this.parent = parent;
    this.writerStack = writerStack;
    this.namespaces = map;
  }

  /**
   * Create a {@link HierarchicalStreamReader}, using the wrapped
   * {@link HierarchicalStreamDriver}.
   *
   * @param reader
   *          the Reader that will be used to read from the underlying stream
   * @return the reader
   * @see com.thoughtworks.xstream.io.HierarchicalStreamDriver#createReader(java.io.Reader)
   */
  public HierarchicalStreamReader createReader(Reader reader) {
    return parent.createReader(reader);
  }

  /**
   * Create a {@link HierarchicalStreamReader}, using the wrapped
   * {@link HierarchicalStreamDriver}.
   *
   * @param inputStream
   *          the input stream that will be used to read from the underlying
   *          stream
   * @return the reader
   * @see com.thoughtworks.xstream.io.HierarchicalStreamDriver#createReader(java.io.InputStream)
   */
  public HierarchicalStreamReader createReader(InputStream inputStream) {
    return parent.createReader(inputStream);
  }

  /**
   * Create a {@link HierarchicalStreamWriter} that tracks the path to the
   * current element based on a {@link java.io.Writer}.
   *
   * @param writer
   *          the underlying writer that will perform the writes.
   * @return the writer
   * @see com.thoughtworks.xstream.io.HierarchicalStreamDriver#createWriter(java.io.Writer)
   */
  public HierarchicalStreamWriter createWriter(Writer writer) {
    HierarchicalStreamWriter parentWriter = parent.createWriter(writer);
    return new StackWriterWrapper(parentWriter, writerStack, namespaces);
  }

  /**
   * Create a {@link HierarchicalStreamWriter} that tracks the path to the
   * current element based on a {@link OutputStream}.
   *
   * @param outputStream
   *          the underlying output stream that will perform the writes.
   * @return the writer
   * @see com.thoughtworks.xstream.io.HierarchicalStreamDriver#createWriter(java.io.Writer)
   */
  public HierarchicalStreamWriter createWriter(OutputStream outputStream) {
    HierarchicalStreamWriter parentWriter = parent.createWriter(outputStream);
    return new StackWriterWrapper(parentWriter, writerStack, namespaces);
  }

  public HierarchicalStreamReader createReader(URL url) {
    return parent.createReader(url);
  }

  public HierarchicalStreamReader createReader(File file) {
    return parent.createReader(file);
  }
}

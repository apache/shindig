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
package org.apache.shindig.social.core.util.xstream;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.WriterWrapper;

/**
 * A Writer that provides a Stack based tracking of the location of the
 * underlying writer.
 */
public class StackWriterWrapper extends WriterWrapper {

  /**
   * The stack that keeps track of current node.
   */
  private WriterStack writerStack;

  /**
   * Create a {@link StackWriterWrapper} that wraps a
   * {@link HierarchicalStreamWriter} and tracks where that writer is in the
   * hierarchy.
   *
   * @param wrapped
   *          the underlying writer
   * @param writerStack
   *          the stack that will record where the writer is.
   */
  public StackWriterWrapper(HierarchicalStreamWriter wrapped,
      WriterStack writerStack) {
    super(wrapped);
    this.writerStack = writerStack;
  }

  /**
   * Set attribute values on the current node, but filter out class attributes
   * from the writer, this is not strictly a feature of this class, but is
   * required (for shindig to meet the XSD requirements.
   *
   * @param name
   *          the name of attribute
   * @param value
   *          the value of the attribute.
   * @see com.thoughtworks.xstream.io.WriterWrapper#addAttribute(java.lang.String,
   *      java.lang.String)
   */
  public void addAttribute(String name, String value) {
    if (!"class".equals(name)) {
      super.addAttribute(name, value);
    }
  }

  /**
   * Begin a new element or node of the supplied name.
   *
   * @param name
   *          the name of the node.
   *
   * @see com.thoughtworks.xstream.io.WriterWrapper#startNode(java.lang.String )
   */
  @Override
  public void startNode(String name) {
    writerStack.push(name);
    super.startNode(name);
  }

  /**
   * Start a node with a specific class. This may invoke
   * {@link StackWriterWrapper#startNode(String)} so we might have double
   * recording of the position in the stack. This would be a bug.
   *
   * @see com.thoughtworks.xstream.io.WriterWrapper#startNode(java.lang.String ,
   *      java.lang.Class)
   */
  @SuppressWarnings("unchecked") //API is not generic
  @Override
  public void startNode(String name, Class clazz) {
    writerStack.push(name);
    super.startNode(name, clazz);
  }

  /**
   * End the current node, making the parent node the active node.
   *
   * @see com.thoughtworks.xstream.io.WriterWrapper#endNode()
   */
  @Override
  public void endNode() {
    writerStack.pop();
    super.endNode();
  }

}

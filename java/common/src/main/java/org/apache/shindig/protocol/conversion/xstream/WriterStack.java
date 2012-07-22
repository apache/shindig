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

/**
 * A writer stack is a simple stack that tracks the current location of the
 * writer.
 */
public interface WriterStack {

  /**
   * Peek into the current location of the writer.
   *
   * @return the name of the current node.
   */
  String peek();

  /**
   * @return the current namespace.
   */
  NamespaceSet peekNamespace();

  /**
   * Reset the stack to its default state.
   */
  void reset();

  /**
   * add a node name into the stack indicating that the writer has moved into a
   * new child element.
   *
   * @param name
   *          the name of the new child element.
   * @param namespace
   *          the namespace set associated with the current element.
   */
  void push(String name, NamespaceSet namespace);

  /**
   * Remove and return the current node name, making the parent node the active
   * node name.
   *
   * @return the node name just removed from the stack.
   */
  String pop();

  /**
   * @return the size of the statck
   */
  int size();

}

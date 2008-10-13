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

import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of a WriterStack that can be shared amongst multiple
 * threads and will record the state of each thread. This cannot however be
 * shared amongst multiple writers on multiple threads as this would lead to an
 * inconsistent state. In the shindig implementation this is not an issue as the
 * serialization process is atomic below the API.
 */
public class ThreadSafeWriterStack implements WriterStack {
  /**
   * A thread local holder for the stack.
   */
  private ThreadLocal<List<String>> stackHolder = new ThreadLocal<List<String>>();

  /**
   * Create a {@link WriterStack} that is thread safe. The stack will store its
   * contents on the thread so this class can be shared amongst multiple
   * threads, but obviously there must be only one instance of the class per
   * writer per thread.
   */
  public ThreadSafeWriterStack() {
    stackHolder.set(new ArrayList<String>());
  }

  /**
   * Add an element name to the stack on the current thread.
   *
   * @param name
   *          the node name just added.
   */
  public void push(String name) {
    stackHolder.get().add(name);
  }

  /**
   * Remove a node name from the stack on the current thread.
   *
   * @return the node name just ended.
   */
  public String pop() {
    List<String> stack = stackHolder.get();
    if (stack.size() == 0) {
      return null;
    } else {
      return stack.remove(stack.size() - 1);
    }
  }

  /**
   * Look at the node name on the top of the stack on the current thread.
   *
   * @return the current node name.
   */
  public String peek() {
    List<String> stack = stackHolder.get();
    if (stack.size() == 0) {
      return null;
    } else {
      return stack.get(stack.size() - 1);
    }
  }

  /**
   * Reset the stack back to the default state.
   *
   * @see org.apache.shindig.social.core.util.xstream.WriterStack#reset()
   */
  public void reset() {
    stackHolder.get().clear();
  }

}

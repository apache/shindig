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
package org.apache.shindig.gadgets.rewrite;

import com.google.inject.Inject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Registry into which is injected a single rewriter, which
 * bootstraps the rewriters list. This enables modularization
 * of {@code ContentRewriter} instances without changing
 * Guice injection bindings. The class also provides a method
 * for manipulating a simple list of rewriters.
 */
public class BasicContentRewriterRegistry implements ContentRewriterRegistry {
  private final List<ContentRewriter> rewriters;
  
  @Inject
  public BasicContentRewriterRegistry(ContentRewriter firstRewriter) {
    rewriters = new LinkedList<ContentRewriter>();
    appendRewriter(firstRewriter);
  }
  
  /** {@inheritDoc} */
  public List<ContentRewriter> getRewriters() {
    return Collections.unmodifiableList(rewriters);
  }
  
  public void appendRewriter(ContentRewriter rewriter) {
    if (rewriter != null) {
      rewriters.add(rewriter);
    }
  }

}

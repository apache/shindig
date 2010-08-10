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

/**
 * Indicates properties of a rewrite operation that can't be determined by
 * inspecting manipulated content directly. Gives hints to calling code as
 * to how to cache rewritten results, in particular.
 */
public class RewriterResults {
  private final long cacheTtl;
  
  /**
   * @return Amount of time, in milliseconds, the rewritten results may be cached. <= 0
   * indicates not cacheable.
   */
  public long getCacheTtl() {
    return cacheTtl;
  }
  
  /**
   * Helper method indicating whether results are cacheable at all.
   * @return Whether rewriter operation is cacheable.
   */
  public boolean isCacheable() {
    return cacheTtl > 0;
  }
  
  // Helper methods for creation of RewriterResults objects
  
  /**
   * @return Object indicating that results are not cacheable.
   */
  public static RewriterResults notCacheable() {
    return new RewriterResults(0);
  }
  
  /**
   * Indicates the results are cacheable for {@code ttl} milliseconds.
   * @param ttl Time in milliseconds that results are cacheable.
   * @return RewriterResults object indicating this.
   */
  public static RewriterResults cacheable(long ttl) {
    return new RewriterResults(ttl);
  }
  
  /**
   * @return Object indicating results can be cached forever.
   */
  public static RewriterResults cacheableIndefinitely() {
    return new RewriterResults(Long.MAX_VALUE);
  }
  
  private RewriterResults(long cacheTtl) {
    this.cacheTtl = cacheTtl;
  }
}

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

import junit.framework.TestCase;

public class BasicContentRewriterRegistryTest extends TestCase {
  public void testNoArgsCreatedBasicRegistry() {
    BasicContentRewriterRegistry r = new BasicContentRewriterRegistry(null);
    assertNotNull(r.getRewriters());
    assertEquals(0, r.getRewriters().size());
  }
  
  public void testSingleValuedBasicRegistry() {
    BasicContentRewriterRegistry r = new BasicContentRewriterRegistry(
        new NoOpContentRewriter());
    assertNotNull(r.getRewriters());
    assertEquals(1, r.getRewriters().size());
    assertTrue(r.getRewriters().get(0) instanceof NoOpContentRewriter);
  }
  
  public void testBasicContentRegistryWithAdds() {
    ContentRewriter cr0 = new NoOpContentRewriter();
    BasicContentRewriterRegistry r = new BasicContentRewriterRegistry(cr0);
    ContentRewriter cr1 = new NoOpContentRewriter();
    ContentRewriter cr2 = new NoOpContentRewriter();
    r.appendRewriter(cr1);
    r.appendRewriter(cr2);
    assertNotNull(r.getRewriters());
    assertEquals(3, r.getRewriters().size());
    assertSame(cr0, r.getRewriters().get(0));
    assertSame(cr1, r.getRewriters().get(1));
    assertSame(cr2, r.getRewriters().get(2));
  }
}

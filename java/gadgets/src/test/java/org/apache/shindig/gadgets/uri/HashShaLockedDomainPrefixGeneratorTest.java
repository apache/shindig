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
package org.apache.shindig.gadgets.uri;

import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.uri.Uri;

import org.junit.Test;

/**
 * Very simple test case for a very simple class generating
 * locked-domain prefixes for Uris. The main value of this test is to
 * increase code coverage numbers, but there's also some value in
 * checkpointing the class's output, since if it ever changed integrators
 * would need to be well aware of this fact, since changing l-d requires
 * a delicate dance in production.
 */
public class HashShaLockedDomainPrefixGeneratorTest {
  private HashShaLockedDomainPrefixGenerator generator = new HashShaLockedDomainPrefixGenerator();

  @Test
  public void generate() {
    Uri uri = Uri.parse("http://www.apache.org/gadget.xml");
    assertEquals("e5bld32ce9pe5ln81rjhe0d0e1vao1ba", generator.getLockedDomainPrefix(uri));
  }

  @Test(expected = NullPointerException.class)
  public void isNull() {
    generator.getLockedDomainPrefix((Uri)null);
  }
}

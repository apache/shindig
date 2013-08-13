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
package org.apache.shindig.gadgets.oauth2.persistence.sample;

import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Encrypter;
import org.junit.Assert;
import org.junit.Test;

public class NoOpEncrypterTest {
  @Test
  public void testNoOpEncrypter_1() throws Exception {

    final NoOpEncrypter result = new NoOpEncrypter();

    Assert.assertNotNull(result);
    Assert.assertTrue(OAuth2Encrypter.class.isInstance(result));
  }

  @Test
  public void testDecrypt_1() throws Exception {
    final NoOpEncrypter fixture = new NoOpEncrypter();
    final String encryptedSecret = "secretin";

    final byte[] bytes = fixture.decrypt(encryptedSecret.getBytes("UTF-8"));

    final String result = new String(bytes, "UTF-8");

    Assert.assertEquals("secretin", result);
  }

  @Test
  public void testDecrypt_2() throws Exception {
    final NoOpEncrypter fixture = new NoOpEncrypter();

    final byte[] result = fixture.decrypt(null);

    Assert.assertEquals(null, result);
  }

  @Test
  public void testDecrypt_3() throws Exception {
    final NoOpEncrypter fixture = new NoOpEncrypter();
    final String encryptedSecret = "";

    final byte[] bytes = fixture.decrypt(encryptedSecret.getBytes("UTF-8"));

    final String result = new String(bytes, "UTF-8");

    Assert.assertEquals("", result);
  }

  @Test
  public void testEncrypt_1() throws Exception {
    final NoOpEncrypter fixture = new NoOpEncrypter();
    final String plainSecret = "secretin";

    final byte[] bytes = fixture.encrypt(plainSecret.getBytes("UTF-8"));

    final String result = new String(bytes, "UTF-8");

    Assert.assertEquals("secretin", result);
  }

  @Test
  public void testEncrypt_2() throws Exception {
    final NoOpEncrypter fixture = new NoOpEncrypter();

    final byte[] result = fixture.encrypt(null);

    Assert.assertEquals(null, result);
  }

  @Test
  public void testEncrypt_3() throws Exception {
    final NoOpEncrypter fixture = new NoOpEncrypter();
    final String plainSecret = "";

    final byte[] bytes = fixture.encrypt(plainSecret.getBytes("UTF-8"));

    final String result = new String(bytes, "UTF-8");

    Assert.assertEquals("", result);
  }
}

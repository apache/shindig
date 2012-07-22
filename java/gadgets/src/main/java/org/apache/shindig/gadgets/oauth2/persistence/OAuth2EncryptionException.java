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
package org.apache.shindig.gadgets.oauth2.persistence;

import org.apache.shindig.gadgets.GadgetException;

/**
 * Subclass of {@link OAuth2PersistenceException} for secret
 * encryption/decryption issues.
 *
 */
public class OAuth2EncryptionException extends GadgetException {
  private static final long serialVersionUID = -3884237661767049433L;

  public OAuth2EncryptionException(final Exception cause) {
    super(Code.OAUTH_STORAGE_ERROR, cause);
  }
}

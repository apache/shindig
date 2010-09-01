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
package org.apache.shindig.auth;

import com.google.inject.ImplementedBy;

import java.util.Map;

/**
 *  Handles verification of gadget security tokens.
 *
 * @since 2.0.0
 */
@ImplementedBy(DefaultSecurityTokenCodec.class)
public interface SecurityTokenCodec {

  /**
   * The security token value must be passed on a map value referenced by this key. Additional
   * parameters can be passed as seen fit.
   */
  String SECURITY_TOKEN_NAME = "token";
  
  /**
   * Active URL for the request.  Must include protocol, host, and port.  May include path
   * and may include query.
   */
  String ACTIVE_URL_NAME = "activeUrl";

  /**
   * Decrypts and verifies a gadget security token to return a gadget token.
   *
   * @param tokenParameters Map containing a entry 'token' in wire format (probably encrypted.)
   * @return the decrypted and verified token.
   * @throws SecurityTokenException If tokenString is not a valid token
   */
  SecurityToken createToken(Map<String, String> tokenParameters)
      throws SecurityTokenException;

  String encodeToken(SecurityToken token) throws SecurityTokenException;
}

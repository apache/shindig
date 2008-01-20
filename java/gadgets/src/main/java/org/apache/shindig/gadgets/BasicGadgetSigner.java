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
package org.apache.shindig.gadgets;

/**
 * A GadgetSigner implementation that just provides dummy data to satisfy
 * tests and API calls. Do not use this for any security applications.
 */
public class BasicGadgetSigner implements GadgetSigner {
  private final long timeToLive;

  /**
   * {@inheritDoc}
   */
  public GadgetToken createToken(Gadget gadget) {
    String uri = gadget.getId().getURI().toString();
    long expiry = System.currentTimeMillis() + this.timeToLive;
    return new BasicGadgetToken(uri + '$' + expiry);
  }

  /**
   * {@inheritDoc}
   * This implementation only validates non-empty tokens. Empty tokens
   * are considered to always be valid.
   */
  public GadgetToken createToken(String stringToken) throws GadgetException {
    if (stringToken != null && stringToken.length() != 0) {
      String[] parts = stringToken.split("\\$");
      if (parts.length != 2) {
        throw new GadgetException(GadgetException.Code.INVALID_GADGET_TOKEN,
            "Invalid token format.");
      }
      long expiry = Long.parseLong(parts[1]);
      if (expiry < System.currentTimeMillis()) {
        throw new GadgetException(GadgetException.Code.INVALID_GADGET_TOKEN,
            "Expired token.");
      }
    }
    return new BasicGadgetToken(stringToken);
  }

  /**
   * Create signer
   * @param timeToLive
   */
  public BasicGadgetSigner(long timeToLive) {
    this.timeToLive = timeToLive;
  }

  /**
   * Creates a signer with 24 hour token expiry
   */
  public BasicGadgetSigner() {
    this(24L * 60 * 60 * 1000);
  }
}

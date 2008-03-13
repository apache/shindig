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

import org.apache.shindig.util.BlobCrypterException;

/**
 * A GadgetSigner implementation that just provides dummy data to satisfy
 * tests and API calls. Do not use this for any security applications.
 */
public class BasicGadgetSigner implements GadgetSigner {

  /**
   * {@inheritDoc}
   * 
   * Returns a token with some faked out values.
   */
  public GadgetToken createToken(String stringToken) throws GadgetException {
    try {
      return new BasicGadgetToken("fakeowner", "fakeviewer", "fakeapp",
          "fakedomain");
    } catch (BlobCrypterException e) {
      throw new GadgetException(GadgetException.Code.INVALID_GADGET_TOKEN, e);
    }
  }

  /**
   * Creates a signer with 24 hour token expiry
   */
  public BasicGadgetSigner() {
  }
}

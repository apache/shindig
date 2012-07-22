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
package org.apache.shindig.gadgets.oauth2.handler;

import java.io.Serializable;

import org.apache.shindig.gadgets.oauth2.OAuth2Error;

/**
 * Stores an error in the handler layer.
 *
 *
 */
public class OAuth2HandlerError implements Serializable {
  private static final long serialVersionUID = 6533884367169476207L;

  private final OAuth2Error error;
  private final Exception cause;
  private final String contextMessage;

  public OAuth2HandlerError(final OAuth2Error error, final String contextMessage,
      final Exception cause) {
    this.error = error;
    this.contextMessage = contextMessage;
    this.cause = cause;
  }

  /**
   *
   * @return the {@link OAuth2Error} associated with this error
   */
  public OAuth2Error getError() {
    return this.error;
  }

  /**
   *
   * @return underlying exception that caused is error or <code>null</code>
   */
  public Exception getCause() {
    return this.cause;
  }

  /**
   *
   * @return non-translated message about the context of this error for
   *         debugging purposes
   */
  public String getContextMessage() {
    return this.contextMessage;
  }

  @Override
  public String toString() {
    return OAuth2HandlerError.class.getName() + " : " + this.error + " : "
        + this.getContextMessage() + " : " + this.cause;
  }
}

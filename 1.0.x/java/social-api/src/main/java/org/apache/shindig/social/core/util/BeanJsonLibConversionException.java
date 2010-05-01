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
package org.apache.shindig.social.core.util;

/**
 * Where a conversion exception happens in the Json Lib conversion, this
 * exception is thrown.
 */
public class BeanJsonLibConversionException extends RuntimeException {

  /**
   * serial ID.
   */
  private static final long serialVersionUID = -8609384443448202372L;

  /**
   * create the exception.
   */
  public BeanJsonLibConversionException() {
  }

  /**
   * Constructor.
   * @param message a message
   */
  public BeanJsonLibConversionException(String message) {
    super(message);
  }

  /**
   * create with a cause.
   * @param cause the cause
   */
  public BeanJsonLibConversionException(Throwable cause) {
    super(cause);
  }

  /**
   * create with a cause and a message.
   * @param message a message
   * @param cause the cause
   */
  public BeanJsonLibConversionException(String message, Throwable cause) {
    super(message, cause);
  }

}

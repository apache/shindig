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
package org.apache.shindig.social;

/**
 * Represents the response items that get handed back as json within the
 * DataResponse.
 */
public class ResponseItem {
  /**
   * The ResponseError associated with the item.
   */
  private ResponseError error;
  /**
   * The error message.
   */
  private String errorMessage;

  /**
   * Blank constructor protected for subclasses.
   */
  protected ResponseItem() {
    
  }

  /**
   * Create a ResponseItem specifying the ResponseError and error Message.
   * @param error a ResponseError
   * @param errorMessage the Error Message
   */
  public ResponseItem(ResponseError error, String errorMessage) {
    this.error = error;
    this.errorMessage = errorMessage;
  }

  /**
   * Get the ResponseError associated with this ResponseItem.
   * @return the ResponseError associated with this ResponseItem
   */
  public ResponseError getError() {
    return error;
  }

  /**
   * Set the ResponseError associated with this item.
   * @param error the new ResponseError
   */
  public void setError(ResponseError error) {
    this.error = error;
  }

  /**
   * Get the Error Message associated with this Response Item.
   * @return the Error Message
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Set the Error Message associated with this ResponseItem.
   * @param errorMessage the new error Message
   */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}

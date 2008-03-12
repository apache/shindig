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

import java.util.List;
import java.util.ArrayList;


/**
 * Represents the response object which gets passed as json to the gadget
 */
public class DataResponse extends AbstractSocialData {
  @Mandatory private List<ResponseItem> responses;
  private ResponseError error;

  public DataResponse(ResponseError error) {
    this.error = error;
    this.responses = new ArrayList<ResponseItem>();
  }

  public DataResponse(List<ResponseItem> responses) {
    this.responses = responses;
  }

  public List<ResponseItem> getResponses() {
    return responses;
  }

  public void setResponses(List<ResponseItem> responses) {
    this.responses = responses;
  }

  public ResponseError getError() {
    return error;
  }

  public void setError(ResponseError error) {
    this.error = error;
  }
}
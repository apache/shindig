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
package org.apache.shindig.social.dataservice;

import org.apache.shindig.social.ResponseItem;

import java.util.concurrent.Future;

public abstract class DataRequestHandler {

  public Future<? extends ResponseItem> handleItem(RequestItem request) {
    String httpMethod = request.getMethod();
    Future<? extends ResponseItem> responseItem;

    if ("GET".equals(httpMethod)) {
      responseItem = handleGet(request);
    } else if ("POST".equals(httpMethod)) {
      responseItem = handlePost(request);
    } else if ("PUT".equals(httpMethod)) {
      responseItem = handlePut(request);
    } else if ("DELETE".equals(httpMethod)) {
      responseItem = handleDelete(request);
    } else {
      throw new IllegalArgumentException("Unserviced Http method type");
    }
    return responseItem;
  }

  protected abstract Future<? extends ResponseItem> handleDelete(RequestItem request);

  protected abstract Future<? extends ResponseItem> handlePut(RequestItem request);

  protected abstract Future<? extends ResponseItem> handlePost(RequestItem request);

  protected abstract Future<? extends ResponseItem> handleGet(RequestItem request);
}

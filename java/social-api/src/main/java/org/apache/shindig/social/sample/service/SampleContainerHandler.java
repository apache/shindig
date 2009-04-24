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

package org.apache.shindig.social.sample.service;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

@Service(name = "samplecontainer", path = "/{type}/{doevil}")
public class SampleContainerHandler {

  private final JsonDbOpensocialService service;

  @Inject
  public SampleContainerHandler(JsonDbOpensocialService dbService) {
    this.service = dbService;
  }

  /**
   * We don't distinguish between put and post for these urls.
   */
  @Operation(httpMethods = "PUT")
  public Future<?> update(RequestItem request) throws ProtocolException {
    return create(request);
  }

  /**
   * Handles /samplecontainer/setstate and /samplecontainer/setevilness/{doevil}. TODO(doll): These
   * urls aren't very resty. Consider changing the samplecontainer.html calls post.
   */
  @Operation(httpMethods = "POST")
  public Future<?> create(RequestItem request) throws ProtocolException {
    String type = request.getParameter("type");
    if (type.equals("setstate")) {
      try {
        String stateFile = request.getParameter("fileurl");
        service.setDb(new JSONObject(fetchStateDocument(stateFile)));
      } catch (JSONException e) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
            "The json state file was not valid json", e);
      }
    } else if (type.equals("setevilness")) {
      throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED,
          "evil data has not been implemented yet");
    }

    return ImmediateFuture.newInstance(null);
  }

  /**
   * Handles /samplecontainer/dumpstate
   */
  @Operation(httpMethods = "GET")
  public Future<?> get(RequestItem request) {
    return ImmediateFuture.newInstance(service.getDb());
  }

  private String fetchStateDocument(String stateFileLocation) {
    String errorMessage = "The json state file " + stateFileLocation
        + " could not be fetched and parsed.";

    HttpMethod jsonState = new GetMethod(stateFileLocation);
    HttpClient client = new HttpClient();
    try {
      client.executeMethod(jsonState);

      if (jsonState.getStatusCode() != 200) {
        throw new RuntimeException(errorMessage);
      }
      return jsonState.getResponseBodyAsString();
    } catch (IOException e) {
      throw new RuntimeException(errorMessage, e);
    } finally {
      jsonState.releaseConnection();
    }
  }
}

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

package org.apache.shindig.social.sample.container;

import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.service.DataRequestHandler;
import org.apache.shindig.social.opensocial.service.RequestItem;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.inject.Inject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Future;

public class SampleContainerHandler extends DataRequestHandler {
  private final JsonDbOpensocialService service;
  private static final String POST_PATH = "/samplecontainer/{type}/{doevil}";

  @Inject
  public SampleContainerHandler(JsonDbOpensocialService dbService) {
    this.service = dbService;
  }

  /**
   * We don't support any delete methods right now.
   */
  protected Future<? extends ResponseItem> handleDelete(RequestItem request) {
    throw new UnsupportedOperationException();
  }

  /**
   * We don't distinguish between put and post for these urls.
   */
  protected Future<? extends ResponseItem> handlePut(RequestItem request) {
    return handlePost(request);
  }

  /**
   * Handles /samplecontainer/setstate and /samplecontainer/setevilness/{doevil}.
   * TODO(doll): These urls aren't very resty. Consider changing the samplecontainer.html calls
   * post.
   */
  protected Future<? extends ResponseItem> handlePost(RequestItem request) {
    ResponseItem<Object> response = new ResponseItem<Object>("");

    request.parseUrlWithTemplate(POST_PATH);
    String type = request.getParameters().get("type");
    if (type.equals("setstate")) {
      try {
        String stateFile = request.getParameters().get("fileurl");
        service.setDb(new JSONObject(fetchStateDocument(stateFile)));
      } catch (JSONException e) {
        response = new ResponseItem<Object>(ResponseError.BAD_REQUEST,
            "The json state file was not valid json", null);
      }
    } else if (type.equals("setevilness")) {
      response = new ResponseItem<Object>(ResponseError.NOT_IMPLEMENTED,
          "evil data has not been implemented yet", null);
    }

    return ImmediateFuture.newInstance(response);
  }

  /**
   * Handles /samplecontainer/dumpstate
   */
  protected Future<? extends ResponseItem> handleGet(RequestItem request) {
    return ImmediateFuture.newInstance(new ResponseItem<Object>(service.getDb()));
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

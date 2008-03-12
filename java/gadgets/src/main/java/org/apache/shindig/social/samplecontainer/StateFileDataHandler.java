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
package org.apache.shindig.social.samplecontainer;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.GadgetDataHandler;
import org.apache.shindig.social.RequestItem;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;

/**
 * Servlet for serving the data required for opensocial.
 * This will expand to be more sophisticated as time goes on.
 */
public class StateFileDataHandler implements GadgetDataHandler {

  public enum RequestType {
    DUMP_STATE, SET_STATE
  }

  public boolean shouldHandle(String requestType) {
     try {
      // There should be a cleaner way to do this...
      RequestType.valueOf(requestType);                                         
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public ResponseItem handleRequest(RequestItem request) {
    RequestType type = RequestType.valueOf(request.getType());
    ResponseItem response = new ResponseItem<Object>(
        ResponseError.NOT_IMPLEMENTED);

    XmlStateFileFetcher fetcher = XmlStateFileFetcher.get();

    switch (type) {
      case DUMP_STATE:
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("people", fetcher.getAllPeople());
        state.put("idMap", fetcher.getIdMap());
        state.put("data", fetcher.getAppData());
        response = new ResponseItem<Map<String, Object>>(state);
        break;
      case SET_STATE:
        try {
          String stateFile = request.getParams().getString("fileUrl");
          fetcher.resetStateFile(new URI(stateFile));
          response = new ResponseItem<Object>(new JSONObject());
        } catch (URISyntaxException e) {
          response = new ResponseItem<Object>(ResponseError.BAD_REQUEST);
        } catch (JSONException e) {
          response = new ResponseItem<Object>(ResponseError.BAD_REQUEST);
        }
        break;

    }

    return response;
  }
}
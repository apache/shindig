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
package org.apache.shindig.social.opensocial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.shindig.social.samplecontainer.BasicPeopleService;
import org.apache.shindig.social.samplecontainer.BasicDataService;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.DataService;
import org.apache.shindig.social.opensocial.model.IdSpec;
import org.apache.shindig.social.opensocial.model.OpenSocialDataType;
import org.apache.shindig.social.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for serving the data required for opensocial.
 * This will expand to be more sophisticated as time goes on.
 */
public class OpenSocialDataHandler implements GadgetDataHandler {
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.social");

  // TODO: get through injection
  private static PeopleService peopleHandler = new BasicPeopleService();
  private static DataService dataHandler = new BasicDataService();

  public boolean shouldHandle(String requestType) {
    try {
      // There should be a cleaner way to do this...
      OpenSocialDataType.valueOf(requestType);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public ResponseItem handleRequest(RequestItem request) {
    OpenSocialDataType type = OpenSocialDataType.valueOf(request.getType());
    ResponseItem response = new ResponseItem<Object>(
        ResponseError.NOT_IMPLEMENTED);

    try {
      String jsonSpec = request.getParams().getString("idSpec");
      List<String> peopleIds = peopleHandler.getIds(IdSpec.fromJson(jsonSpec));

      switch (type) {
        case FETCH_PEOPLE :
          response = peopleHandler.getPeople(peopleIds);
          break;

        case FETCH_PERSON_APP_DATA :
          response = dataHandler.getPersonData(peopleIds);
          break;

        case UPDATE_PERSON_APP_DATA:
          // We only support updating one person right now
          String id = peopleIds.get(0);

          String key = request.getParams().getString("key");
          String value = request.getParams().getString("value");

          response = dataHandler.updatePersonData(id, key, value);
          break;
      }

    } catch (JSONException e) {
      response = new ResponseItem<Object>(ResponseError.BAD_REQUEST);
    } catch (IllegalArgumentException e) {
      response = new ResponseItem<Object>(ResponseError.BAD_REQUEST);
    }

    return response;
  }
}
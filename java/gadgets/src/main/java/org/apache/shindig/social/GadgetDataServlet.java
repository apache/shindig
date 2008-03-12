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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.shindig.social.samplecontainer.BasicPeopleService;
import org.apache.shindig.social.samplecontainer.BasicDataService;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.DataService;
import org.apache.shindig.social.opensocial.model.IdSpec;
import org.apache.shindig.social.opensocial.model.OpenSocialDataType;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for serving social data. This is a very basic hardcoded inital file.
 * This will expand to be more sophisticated as time goes on.
 */
public class GadgetDataServlet extends HttpServlet {
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.social");

  // TODO: get through injection
  private PeopleService peopleHandler = new BasicPeopleService();
  private DataService dataHandler = new BasicDataService();

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // TODO: Get the security token
    // TODO: etc, etc, etc

    String requestParam = req.getParameter("request");
    DataResponse response;
    try {
      response = new DataResponse(createResponse(requestParam));
    } catch (JSONException e) {
      response = new DataResponse(ResponseError.BAD_REQUEST);
    }

    PrintWriter writer = resp.getWriter();
    writer.write(response.toJson().toString());
  }

  private List<ResponseItem> createResponse(String requestParam)
      throws JSONException {
    // TODO: Improve json input handling. The json request should get auto
    // translated into objects
    JSONArray requestItems = new JSONArray(requestParam);
    List<ResponseItem> responseItems = new ArrayList<ResponseItem>();
    int length = requestItems.length();

    for (int i = 0; i < length; i++) {
      ResponseItem response = new ResponseItem<Object>(
          ResponseError.NOT_IMPLEMENTED);

      JSONObject requestItem = requestItems.getJSONObject(i);

      try {
        OpenSocialDataType type = OpenSocialDataType.valueOf(
            requestItem.getString("type"));

        String jsonSpec = requestItem.getString("idSpec");
        List<String> peopleIds = peopleHandler.getIds(IdSpec.fromJson(jsonSpec));

        // TODO: Abstract this logic into handlers which register
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

            String key = requestItem.getString("key");
            String value = requestItem.getString("value");

            response = dataHandler.updatePersonData(id, key, value);
            break;
        }

      } catch (JSONException e) {
        response = new ResponseItem<Object>(ResponseError.BAD_REQUEST);
      } catch (IllegalArgumentException e) {
        response = new ResponseItem<Object>(ResponseError.BAD_REQUEST);
      }

      responseItems.add(response);
    }

    return responseItems;
  }

}

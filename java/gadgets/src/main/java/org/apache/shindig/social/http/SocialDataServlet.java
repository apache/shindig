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
package org.apache.shindig.social.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.SocialDataType;
import org.apache.shindig.social.PeopleHandler;
import org.apache.shindig.social.Person;
import org.apache.shindig.social.IdSpec;
import org.apache.shindig.social.DataHandler;
import static org.apache.shindig.social.IdSpec.*;
import org.apache.shindig.social.file.BasicPeopleHandler;
import org.apache.shindig.social.file.BasicDataHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for serving social data. This is a very basic hardcoded inital file.
 * This will expand to be more sophisticated as time goes on.
 */
public class SocialDataServlet extends HttpServlet {
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.social");
  private static final String BAD__REQUEST__RESPONSE =
      "{\"responses\" : {}, \"error\" : \""
      + ResponseError.BAD_REQUEST.toJson() + "\"}";

  // TODO: get through injection
  private PeopleHandler peopleHandler = new BasicPeopleHandler();
  private DataHandler dataHandler = new BasicDataHandler();

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // TODO: Get the security token
    // TODO: Don't use string concatenation for json output
    // TODO: etc, etc, etc

    String requestParam = req.getParameter("request");
    String response;
    try {
      response = createResponse(requestParam);
    } catch (JSONException e) {
      response = BAD__REQUEST__RESPONSE;
    }

    PrintWriter writer = resp.getWriter();
    writer.write(response);
  }

  private String createResponse(String requestParam) throws JSONException {
    JSONArray requestItems = new JSONArray(requestParam);
    String jsonResponse = "{\"responses\" : [";
    int length = requestItems.length();

    for (int i = 0; i < length; i++) {
      String jsonData = "{}";

      JSONObject requestItem = requestItems.getJSONObject(i);

      try {
        SocialDataType type = SocialDataType.valueOf(
            requestItem.getString("type"));

        String jsonSpec = requestItem.getString("idSpec");
        List<String> peopleIds = peopleHandler.getIds(fromJson(jsonSpec));

        switch (type) {
          case FETCH_PEOPLE :
            jsonData = handleFetchPeople(peopleIds);
            break;

          case FETCH_PERSON_APP_DATA :
            jsonData = handleFetchData(peopleIds);
            break;

          case UPDATE_PERSON_APP_DATA:
            jsonData = handleUpdateData(peopleIds, requestItem);
            break;
        }

        jsonResponse += "{\"response\" : " + jsonData + "}";
      } catch (JSONException e) {
        jsonResponse += "{\"response\" : {}, \"error\" : \""
            + ResponseError.BAD_REQUEST.toJson() + "\"}";
      } catch (IllegalArgumentException e) {
        jsonResponse += "{\"response\" : {}, \"error\" : \""
            + ResponseError.BAD_REQUEST.toJson() + "\"}";
      }

      if (i < length - 1) {
        jsonResponse += ",";
      }
    }
    jsonResponse += "]}";
    return jsonResponse;
  }

  private String handleFetchData(List<String> peopleIds) {
    Map<String, Map<String,String>> people
        = dataHandler.getPersonData(peopleIds);

    String jsonData = "{";

    for (String userId : people.keySet()) {
      Map<String, String> userData = people.get(userId);
      jsonData += "\"" + userId + "\" : {";

      if (userData != null) {
        for (String dataKey : userData.keySet()) {
          String dataValue = userData.get(dataKey);
          jsonData += "\"" + dataKey + "\" : \"" + dataValue + "\" ";
        }
      }

      jsonData += "}, ";
    }

    jsonData += "}";

    return jsonData;
  }

  private String handleUpdateData(List<String> peopleIds,
      JSONObject requestItem) throws JSONException {

    // We only support updating one person right now
    String id = peopleIds.get(0);

    String key = requestItem.getString("key");
    String value = requestItem.getString("value");

    // TODO: Clean this error handling up
    boolean success = dataHandler.updatePersonData(id, key, value);
    if (!success) {
      throw new IllegalArgumentException();
    } else {
      return "{}";
    }
  }

  private String handleFetchPeople(List<String> peopleIds) {
    List<Person> people = peopleHandler.getPeople(peopleIds);
    if (people.isEmpty()) {
      return "{}";
    }

    String jsonData = "[";
    for (Person person : people) {
      jsonData += person.toJson() + ",";
    }
    jsonData += "]";

    return jsonData;
  }

}

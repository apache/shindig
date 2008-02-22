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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

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

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // TODO: Get the security token
    // TODO: Allow saving data
    // TODO: Don't use string concatentation for json output
    // TODO: etc, etc, etc

    String requestParam = req.getParameter("request");
    try {
      JSONArray requestItems = new JSONArray(requestParam);
      String jsonResponse = "{'responses' : [";
      int length = requestItems.length();

      for (int i = 0; i < length; i++) {
        String jsonData = "{}";

        JSONObject requestItem = requestItems.getJSONObject(i);
        String type = requestItem.getString("type");

        if (type.equals("FETCH_PEOPLE")) {
          String idSpec = requestItem.getString("idSpec");
          if (idSpec.equals("VIEWER") || idSpec.equals("OWNER")) {
            jsonData = "[{'id' : 'john.doe', 'name' : {'unstructured' : 'John Doe'}}]";
          } else if (idSpec.equals("VIEWER_FRIENDS")) {
            jsonData = "[{'id' : 'jane.doe', 'name' : {'unstructured' : 'Jane Doe'}}, " +
                "{'id' : 'george.doe', 'name' : {'unstructured' : 'George Doe'}}]";
          }

        } else if (type.equals("FETCH_PERSON_APP_DATA")) {
          String idSpec = requestItem.getString("idSpec");
          if (idSpec.equals("VIEWER")) {
            jsonData = "{'john.doe' : {'count' : 3}}";
          } else if (idSpec.equals("VIEWER_FRIENDS")) {
            jsonData = "{'jane.doe' : {'count' : 7}, " +
                "'george.doe' : {'count' : 2}}";
          }

        }

        jsonResponse += "{'response' : " + jsonData + "}";
        if (i < length - 1) {
          jsonResponse += ",";
        }
      }
      jsonResponse += "]}";
      PrintWriter writer = resp.getWriter();
      writer.write(jsonResponse);

    } catch (JSONException e) {
      // TODO: handle the exception here
    }

  }
}

/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/
package org.apache.shindig.social.abdera.json;

import org.apache.shindig.social.abdera.RawResponseContext;
import org.apache.shindig.social.abdera.RequestUrlTemplate;
import org.apache.shindig.social.abdera.SocialRouteManager;
import org.apache.shindig.social.abdera.util.ValidRequestFilter;
import org.apache.shindig.social.abdera.util.ValidRequestFilter.Format;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.Filter;
import org.apache.abdera.protocol.server.FilterChain;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextWrapper;
import org.apache.abdera.writer.Writer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Filter implementation that will convert an Atom document returned by
 * the server into a JSON document unless the request specifically asks for
 * atom by adding a format=atom query string parameter
 *
 * TODO: Converting from atom to json is silly. We should just convert from
 * pojo to atom and pojo to json. Need to fix abdera!
 */
public class JSONFilter implements Filter {

  public ResponseContext filter(RequestContext request, FilterChain chain) {
    ResponseContext resp = chain.next(request);
    // Bypass the filter for RawResponseContext responses.
    if (resp.getClass().equals(RawResponseContext.class)){
      return resp;
    }
    Format format = ValidRequestFilter.getFormatTypeFromRequest(request);
    if (format == Format.ATOM) {
      return resp;
    }
    // If there is no content, it could be either due to some error such as
    // a 404 or, there is no content to be translated into json. Return.
    // TODO: verify this claim
    if (resp.getContentType() == null) {
      return resp;
    }

    return new JsonResponseContext(resp, request.getAbdera(), request);
  }

  private static class JsonResponseContext extends ResponseContextWrapper {
    private final Abdera abdera;
    private final RequestContext request;

    public JsonResponseContext(ResponseContext response, Abdera abdera,
        RequestContext request) {
      super(response);
      this.request = request;
      setContentType("application/json");
      this.abdera = abdera;
    }

    @Override
    public void writeTo(OutputStream out) throws java.io.IOException {
      ByteArrayOutputStream temp = new ByteArrayOutputStream();
      super.writeTo(temp);
      convertToJson(temp, out);
    }

    @Override
    public void writeTo(OutputStream out, Writer writer)
        throws java.io.IOException {
      ByteArrayOutputStream temp = new ByteArrayOutputStream();
      super.writeTo(temp, writer);
      convertToJson(temp, out);
    }

    private void convertToJson(ByteArrayOutputStream superOut, OutputStream out)
        throws IOException {
      ByteArrayInputStream in = new ByteArrayInputStream(
          superOut.toByteArray());
      Document<Element> doc = abdera.getParser().parse(in);

      OutputStreamWriter streamWriter = new OutputStreamWriter(out);
      streamWriter.write(getJsonFromDocument(doc));
      streamWriter.flush();
    }

    private String getJsonFromDocument(Document<Element> doc) {
      // The JSON format for OpenSocial rest doesn't do any hoisting.
      // Thus, we just want to pull the main content object out.
      // TODO: There's gotta be a better way to do this with abdera...

      Element root = doc.getRoot();

      if (root instanceof Entry) {
        Entry entry = (Entry) root;
        return entry.getContentElement().getValue();

      } else if (root instanceof Feed) {
        Feed feed = (Feed) root;
        List<Entry> entries = feed.getEntries();

        JSONObject json = new JSONObject();

        try {
          RequestUrlTemplate url = SocialRouteManager
              .getUrlTemplate(request);

          // If the type of object is Data, then we want to create a JSONObject
          // instead of a JSONArray
          // TODO: This is another messy thing to clean up...
          switch (url) {
            case APPDATA_OF_APP_OF_USER :
            case APPDATA_OF_FRIENDS_OF_USER :
              createDataMapping(json, feed.getEntries());
              break;
            default :
              createJsonArray(json, entries);
          }
        } catch (JSONException e) {
          throw new RuntimeException(
              "The atom Document could not be converted to JSON", e);
        }

        return json.toString();
      }

      throw new UnsupportedOperationException("Converting a non-Entry "
          + "non-Feed abdera Document to JSON is not supported");
    }

    private void createDataMapping(JSONObject json, List<Entry> entries)
        throws JSONException {
      JSONObject jsonObject = new JSONObject();
      for (Entry entry : entries) {
        String contentValue = entry.getContentElement().getValue();
        JSONObject jsonItem = new JSONObject(contentValue);

        jsonObject.put(jsonItem.getString("personId"),
            jsonItem.getJSONObject("appdata"));
      }
      json.put("entry", jsonObject);
    }

    private void createJsonArray(JSONObject json, List<Entry> entries)
        throws JSONException {
      // TODO: Add the top level items for real
      json.put("startIndex", 0);
      json.put("totalResults", entries.size());

      JSONArray jsonArray = new JSONArray();
      for (Entry entry : entries) {
        String contentValue = entry.getContentElement().getValue();
        JSONObject jsonItem = new JSONObject(contentValue);
        jsonArray.put(jsonItem);
      }
      json.put("entry", jsonArray);
    }
  }
}

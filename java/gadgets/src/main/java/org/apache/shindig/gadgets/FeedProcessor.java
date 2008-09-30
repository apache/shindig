/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.util.List;

/**
 * Processes RSS & Atom Feeds and converts them into JSON output.
 */
public class FeedProcessor {

  /**
   * Converts feed XML to JSON.
   *
   * @param feedUrl The url that the feed was retrieved from.
   * @param feedXml The raw XML of the feed to be converted.
   * @param getSummaries True if summaries should be returned.
   * @param numEntries Number of entries to return.
   * @return The JSON representation of the feed.
   */
  @SuppressWarnings("unchecked")
  public JSONObject process(String feedUrl, String feedXml,
      boolean getSummaries, int numEntries) throws GadgetException {
    try {
      SyndFeed feed = new SyndFeedInput().build(new StringReader(feedXml));
      JSONObject json = new JSONObject();
      json.put("Title", feed.getTitle());
      json.put("URL", feedUrl);
      json.put("Description", feed.getDescription());
      json.put("Link", feed.getLink());

      List<SyndPerson> authors = feed.getAuthors();
      String jsonAuthor = null;
      if (authors != null && !authors.isEmpty()) {
        SyndPerson author = authors.get(0);
        if (author.getName() != null) {
          jsonAuthor = author.getName();
        } else if (author.getEmail() != null) {
          jsonAuthor = author.getEmail();
        }
      }
      JSONArray entries = new JSONArray();
      json.put("Entry", entries);

      int entryCnt = 0;
      for (Object obj : feed.getEntries()) {
        SyndEntry e = (SyndEntry) obj;
        if (entryCnt >= numEntries) {
          break;
        }
        entryCnt++;

        JSONObject entry = new JSONObject();
        entry.put("Title", e.getTitle());
        entry.put("Link", e.getLink());
        if (getSummaries) {
          if (e.getContents() != null && e.getContents().size() > 0) {
            entry.put("Summary",
                ((SyndContent)e.getContents().get(0)).getValue());
          } else {
            entry.put("Summary",
                e.getDescription() != null ? e.getDescription().getValue() : "");
          }
        }

        if (e.getUpdatedDate() != null) {
          entry.put("Date", e.getUpdatedDate().getTime());
        } else if (e.getPublishedDate() != null) {
          entry.put("Date", e.getPublishedDate().getTime());
        } else {
          entry.put("Date", 0);
        }

        // if no author at feed level, use the first entry author
        if (jsonAuthor == null) {
          jsonAuthor = e.getAuthor();
        }

        entries.put(entry);
      }

      json.put("Author", (jsonAuthor != null) ? jsonAuthor : "");
      return json;
    } catch (JSONException e) {
      // This shouldn't ever happen.
      throw new RuntimeException(e);
    } catch (FeedException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e);
    }
  }
}

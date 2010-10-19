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

import java.io.StringReader;
import java.util.List;

import com.sun.syndication.feed.module.mediarss.types.UrlReference;

import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.MediaModule;
import com.sun.syndication.feed.module.mediarss.types.MediaContent;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.ImplementedBy;


/**
 * Processes RSS & Atom Feeds and converts them into JSON output.
 */
@ImplementedBy(FeedProcessorImpl.class)
public interface FeedProcessor {

  /**
   * Converts feed XML to JSON.
   * 
   * @param feedUrl
   *            The url that the feed was retrieved from.
   * @param feedXml
   *            The raw XML of the feed to be converted.
   * @param getSummaries
   *            True if summaries should be returned.
   * @param numEntries
   *            Number of entries to return.
   * @return The JSON representation of the feed.
   */
  JSONObject process(String feedUrl, String feedXml, boolean getSummaries, int numEntries)
          throws GadgetException;
}

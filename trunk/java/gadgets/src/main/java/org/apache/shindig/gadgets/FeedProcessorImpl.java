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

import com.google.common.base.Strings;
import com.sun.syndication.feed.module.mediarss.types.UrlReference;
import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.MediaModule;
import com.sun.syndication.feed.module.mediarss.types.MediaContent;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Processes RSS/Atom Feeds and converts them into JSON output.
 */
public class FeedProcessorImpl implements FeedProcessor {

  /**
   * Converts feed XML to JSON.
   *
   * @param feedUrl      The url that the feed was retrieved from.
   * @param feedXml      The raw XML of the feed to be converted.
   * @param getSummaries True if summaries should be returned.
   * @param numEntries   Number of entries to return.
   * @return The JSON representation of the feed.
   */
  //@SuppressWarnings("unchecked")
  public JSONObject process(String feedUrl, String feedXml, boolean getSummaries, int numEntries)
      throws GadgetException {
    try {
      SyndFeed feed = new SyndFeedInput().build(new StringReader(feedXml));
      JSONObject json = new JSONObject();
      json.put("Title", Strings.nullToEmpty(feed.getTitle()));
      json.put("URL", feedUrl);
      json.put("Description", Strings.nullToEmpty(feed.getDescription()));
      json.put("Link", Strings.nullToEmpty(feed.getLink()));

      //Retrieve the feed image if it is available as well as an image url if the image is available.
      if (feed.getImage() != null && !Strings.isNullOrEmpty(feed.getImage().getUrl())) {
        SyndImage feedImage = feed.getImage();
        JSONObject jsonImage = new JSONObject();
        jsonImage.put("Url", feedImage.getUrl());

        if (!Strings.isNullOrEmpty(feedImage.getTitle())) {
          jsonImage.put("Title", feedImage.getTitle());
        }
        if (!Strings.isNullOrEmpty(feedImage.getDescription())) {
          jsonImage.put("Description", feedImage.getDescription());
        }
        if (!Strings.isNullOrEmpty(feedImage.getLink())) {
          jsonImage.put("Link", feedImage.getLink());
        }
        json.put("Image", jsonImage);
      }


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
        String link = e.getLink();
        if (link == null) {
          List<SyndLink> links = e.getLinks();
          if (links != null && !links.isEmpty()) {
            link = links.get(0).getHref();
          }
        }
        entry.put("Link", link);
        if (getSummaries) {
          if (e.getContents() != null && !e.getContents().isEmpty()) {
            entry.put("Summary", ((SyndContent) e.getContents().get(0)).getValue());
          } else {
            entry.put("Summary", e.getDescription() != null ? e.getDescription().getValue() : "");
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

        JSONObject media = new JSONObject();
        MediaEntryModule mediaModule = (MediaEntryModule) e.getModule(MediaModule.URI);
        if (mediaModule != null) {
          if (mediaModule.getMediaContents().length > 0) {
            JSONArray contents = new JSONArray();

            for (MediaContent c : mediaModule.getMediaContents()) {
              JSONObject content = new JSONObject();

              if (c.getReference() instanceof UrlReference) {
                content.put("URL", ((UrlReference) c.getReference()).getUrl().toString());
              }

              if (c.getType() != null) {
                content.put("Type", c.getType());
              }

              if (c.getWidth() != null) {
                content.put("Width", c.getWidth());
              }

              if (c.getHeight() != null) {
                content.put("Height", c.getHeight());
              }

              contents.put(content);
            }

            media.put("Contents", contents);
          }

          if (mediaModule.getMetadata() != null) {
            if (mediaModule.getMetadata().getThumbnail().length > 0) {
              // "If multiple thumbnails are included, it is assumed that they are in order of importance"
              // Only use the first thumbnail for simplicity's
              // sake

              JSONObject thumbnail = new JSONObject();

              Thumbnail t = mediaModule.getMetadata().getThumbnail()[0];
              thumbnail.put("URL", t.getUrl().toString());

              if (t.getWidth() != null) {
                thumbnail.put("Width", t.getWidth());
              }

              if (t.getHeight() != null) {
                thumbnail.put("Height", t.getHeight());
              }

              media.put("Thumbnail", thumbnail);
            }
          }
        }

        entry.put("Media", media);

        entries.put(entry);
      }

      json.put("Author", (jsonAuthor != null) ? jsonAuthor : "");
      return json;
    } catch (JSONException e) {
      // This shouldn't ever happen.
      throw new RuntimeException(e);
    } catch (FeedException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e, HttpResponse.SC_BAD_GATEWAY);
    } catch (IllegalArgumentException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e, HttpResponse.SC_BAD_GATEWAY);
    }
  }
}

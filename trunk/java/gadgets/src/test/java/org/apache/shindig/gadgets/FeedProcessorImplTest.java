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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Tests for FeedProcessorImpl
 */
public class FeedProcessorImplTest {

  private final static String FEED_TITLE = "Example Feed";
  private final static String FEED_AUTHOR = "John Doe";
  private final static String FEED_AUTHOR_EMAIL = "john.doe@example.com";
  private final static String FEED_IMAGE_URL = "http://example.org/example.gif";
  private final static String FEED_IMAGE_TITLE = "Example Feed Image";
  private final static String FEED_IMAGE_DESCRIPTION = "Example Feed Image Description";
  private final static String FEED_IMAGE_LINK = "http://example.org/";
  private final static String FEED_ENTRY_TITLE = "Atom-Powered Robots Run Amok";
  private final static String FEED_ENTRY_LINK = "http://example.org/2003/12/13/entry03";
  private final static String FEED_ENTRY_SUMMARY = "Some text.";
  private final static String URL_RSS = "http://www.example.com/rss.xml";
  private final static long TIMESTAMP = 1212790800000L;
  private final static String DATE_RSS = "Fri, 06 Jun 2008 22:20:00 GMT";
  private final static String DATA_RSS =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<rss version=\"2.0\"><channel>" +
      "<title>" + FEED_TITLE + "</title>" +
      "<link>http://example.org/</link>" +
      "<description>Example RSS Feed</description>" +
      "<pubDate>Sun, 19 May 2002 15:21:36 GMT</pubDate>" +
      "<image>" +
      "<url>" + FEED_IMAGE_URL + "</url>" +
      "<title>" + FEED_IMAGE_TITLE + "</title>" +
      "<description>" + FEED_IMAGE_DESCRIPTION + "</description>" +
      "<link>" + FEED_IMAGE_LINK + "</link>" +
      "</image>" +
      "<item>" +
      "<title>" + FEED_ENTRY_TITLE + "</title>" +
      "<link>" + FEED_ENTRY_LINK + "</link>" +
      "<guid>" + FEED_ENTRY_LINK + "#item1" + "</guid>" +
      "<pubDate>" + DATE_RSS + "</pubDate>" +
      "<description>" + FEED_ENTRY_SUMMARY + "</description>" +
      "<author>" + FEED_AUTHOR_EMAIL + "</author>" +
      "</item>" +
      "<item>" +
      "<title>" + FEED_ENTRY_TITLE + "</title>" +
      "<link>" + FEED_ENTRY_LINK + "</link>" +
      "<guid>" + FEED_ENTRY_LINK + "#item1" + "</guid>" +
      "<description>" + FEED_ENTRY_SUMMARY + "</description>" +
      "</item>" +
      "</channel></rss>";
  private final static String MEDIA_CONTENT_URL1 = "http://example.com/img1.jpg";
  private final static String MEDIA_CONTENT_URL2 = "http://example.com/img2.jpg";
  private final static String MEDIA_CONTENT_URL3 = "http://example.com/img3.jpg";
  private final static String MEDIA_CONTENT_TYPE = "image/jpeg";
  private final static int MEDIA_CONTENT_WIDTH = 800;
  private final static int MEDIA_CONTENT_HEIGHT = 600;
  private final static String MEDIA_THUMB_URL = "http://exmaple.com/thumb.jpg";
  private final static int MEDIA_THUMB_WIDTH = 75;
  private final static int MEDIA_THUMB_HEIGHT = 50;
  private final static String DATA_RSS_WITH_MEDIARSS =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<rss version=\"2.0\" xmlns:media=\"http://search.yahoo.com/mrss/\"><channel>" +
      "<title>" + FEED_TITLE + "</title>" +
      "<link>http://example.org/</link>" +
      "<description>Example RSS Feed</description>" +
      "<pubDate>Sun, 19 May 2002 15:21:36 GMT</pubDate>" +
      "<item>" +
      "<title>" + FEED_ENTRY_TITLE + "</title>" +
      "<link>" + FEED_ENTRY_LINK + "</link>" +
      "<guid>" + FEED_ENTRY_LINK + "#item1" + "</guid>" +
      "<pubDate>" + DATE_RSS + "</pubDate>" +
      "<description>" + FEED_ENTRY_SUMMARY + "</description>" +
      "<author>" + FEED_AUTHOR_EMAIL + "</author>" +
      "<media:content url=\"" + MEDIA_CONTENT_URL1 + "\" type=\"" + MEDIA_CONTENT_TYPE + "\" isDefault=\"false\" expression=\"sample\" width=\"" + MEDIA_CONTENT_WIDTH + "\" height=\"" + MEDIA_CONTENT_HEIGHT + "\" />" +
      "<media:content url=\"" + MEDIA_CONTENT_URL2 + "\" type=\"" + MEDIA_CONTENT_TYPE + "\" isDefault=\"false\" expression=\"sample\" width=\"" + MEDIA_CONTENT_WIDTH + "\" height=\"" + MEDIA_CONTENT_HEIGHT + "\" />" +
      "<media:content url=\"" + MEDIA_CONTENT_URL3 + "\" type=\"" + MEDIA_CONTENT_TYPE + "\" isDefault=\"false\" expression=\"sample\" width=\"" + MEDIA_CONTENT_WIDTH + "\" height=\"" + MEDIA_CONTENT_HEIGHT + "\" />" +
      "<media:thumbnail url=\"" + MEDIA_THUMB_URL + "\" width=\"" + MEDIA_THUMB_WIDTH + "\" height=\"" + MEDIA_THUMB_HEIGHT + "\" />" +
      "</item>" +
      "<item>" +
      "<title>" + FEED_ENTRY_TITLE + "</title>" +
      "<link>" + FEED_ENTRY_LINK + "</link>" +
      "<guid>" + FEED_ENTRY_LINK + "#item1" + "</guid>" +
      "<description>" + FEED_ENTRY_SUMMARY + "</description>" +
      "<media:thumbnail url=\"" + MEDIA_THUMB_URL + "\" />" +
      "</item>" +
      "</channel></rss>";

  private final static String URL_ATOM = "http://www.example.com/feed.atom";
  private final static String DATE_ATOM = "2008-06-06T22:20:00Z";
  private final static String DATA_ATOM =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<feed xmlns=\"http://www.w3.org/2005/Atom\">" +
      "<title>" + FEED_TITLE + "</title>" +
      "<link href=\"http://example.org/\"/>" +
      "<updated>2003-12-13T18:30:02Z</updated>" +
      "<id>urn:uuid:60a76c80-d399-11d9-b93C-0003939e0af6</id>" +
      "<author><name>" + FEED_AUTHOR + "</name></author>" +
      "<entry>" +
      "<title>" + FEED_ENTRY_TITLE + "</title>" +
      "<link href=\"" + FEED_ENTRY_LINK + "\"/>" +
      "<id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a</id>" +
      "<updated>" + DATE_ATOM + "</updated>" +
      "<summary>" + FEED_ENTRY_SUMMARY + "</summary>" +
      "</entry>" +
      "<entry>" +
      "<title>" + FEED_ENTRY_TITLE + "</title>" +
      "<link href=\"" + FEED_ENTRY_LINK + "\"/>" +
      "<id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da3443fa6a</id>" +
      "<summary>" + FEED_ENTRY_SUMMARY + "</summary>" +
      "</entry>" +
      "</feed>";
  private final static String DATA_ATOM_WITH_MEDIARSS =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:media=\"http://search.yahoo.com/mrss/\">" +
      "<title>" + FEED_TITLE + "</title>" +
      "<link href=\"http://example.org/\"/>" +
      "<updated>2003-12-13T18:30:02Z</updated>" +
      "<id>urn:uuid:60a76c80-d399-11d9-b93C-0003939e0af6</id>" +
      "<author><name>" + FEED_AUTHOR + "</name></author>" +
      "<entry>" +
      "<title>" + FEED_ENTRY_TITLE + "</title>" +
      "<link href=\"" + FEED_ENTRY_LINK + "\"/>" +
      "<id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a</id>" +
      "<updated>" + DATE_ATOM + "</updated>" +
      "<summary>" + FEED_ENTRY_SUMMARY + "</summary>" +
      "<media:content url=\"" + MEDIA_CONTENT_URL1 + "\" type=\"" + MEDIA_CONTENT_TYPE + "\" isDefault=\"false\" expression=\"sample\" width=\"" + MEDIA_CONTENT_WIDTH + "\" height=\"" + MEDIA_CONTENT_HEIGHT + "\" />" +
      "<media:content url=\"" + MEDIA_CONTENT_URL2 + "\" type=\"" + MEDIA_CONTENT_TYPE + "\" isDefault=\"false\" expression=\"sample\" width=\"" + MEDIA_CONTENT_WIDTH + "\" height=\"" + MEDIA_CONTENT_HEIGHT + "\" />" +
      "<media:content url=\"" + MEDIA_CONTENT_URL3 + "\" type=\"" + MEDIA_CONTENT_TYPE + "\" isDefault=\"false\" expression=\"sample\" width=\"" + MEDIA_CONTENT_WIDTH + "\" height=\"" + MEDIA_CONTENT_HEIGHT + "\" />" +
      "<media:thumbnail url=\"" + MEDIA_THUMB_URL + "\" width=\"" + MEDIA_THUMB_WIDTH + "\" height=\"" + MEDIA_THUMB_HEIGHT + "\" />" +
      "</entry>" +
      "<entry>" +
      "<title>" + FEED_ENTRY_TITLE + "</title>" +
      "<link href=\"" + FEED_ENTRY_LINK + "\"/>" +
      "<id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da3443fa6a</id>" +
      "<summary>" + FEED_ENTRY_SUMMARY + "</summary>" +
      "<media:thumbnail url=\"" + MEDIA_THUMB_URL + "\" />" +
      "</entry>" +
      "</feed>";
  private final static String BAD_XML = "broken xml !!!! & ><";
  private final static String INVALID_XML = "<data><foo/></data>";

  private final FeedProcessor processor;

  public FeedProcessorImplTest() {
    processor = new FeedProcessorImpl();
  }

  @Test
  public void parseRss() throws Exception {
    JSONObject feed = processor.process(URL_RSS, DATA_RSS, true, 1);

    assertEquals(URL_RSS, feed.getString("URL"));
    assertEquals(FEED_TITLE, feed.getString("Title"));
    assertEquals(FEED_AUTHOR_EMAIL, feed.getString("Author"));

    JSONArray entryArray = feed.getJSONArray("Entry");
    JSONObject entry = entryArray.getJSONObject(0);

    assertEquals(1, entryArray.length());
    assertEquals(FEED_ENTRY_TITLE, entry.getString("Title"));
    assertEquals(FEED_ENTRY_LINK, entry.getString("Link"));
    assertEquals(FEED_ENTRY_SUMMARY, entry.getString("Summary"));
  }

  @Test
  public void parseRssMultiple() throws Exception {
    JSONObject feed = processor.process(URL_RSS, DATA_RSS, true, 2);
    JSONArray entryArray = feed.getJSONArray("Entry");
    assertEquals(2, entryArray.length());
  }

  @Test
  public void parseRssDate() throws Exception {
    JSONObject feed = processor.process(URL_RSS, DATA_RSS, true, 2);
    JSONArray entryArray = feed.getJSONArray("Entry");
    assertEquals(TIMESTAMP, entryArray.getJSONObject(0).getLong("Date"));
    assertEquals(0, entryArray.getJSONObject(1).getLong("Date"));
  }

  @Test
  public void parseRssNoSummaries() throws Exception {
    JSONObject feed = processor.process(URL_RSS, DATA_RSS, false, 1);
    feed.getJSONArray("Entry");
    JSONObject entry = feed.getJSONArray("Entry").getJSONObject(0);
    assertNull("Summary should not be returned when getSummaries is false",
        entry.optString("Summary", null));
  }

  @Test
  public void parseMediaRss() throws Exception {
    JSONObject feed = processor.process(URL_RSS, DATA_RSS_WITH_MEDIARSS, true, 1);

    assertEquals(URL_RSS, feed.getString("URL"));
    assertEquals(FEED_TITLE, feed.getString("Title"));
    assertEquals(FEED_AUTHOR_EMAIL, feed.getString("Author"));

    JSONArray entryArray = feed.getJSONArray("Entry");
    JSONObject entry = entryArray.getJSONObject(0);

    assertEquals(1, entryArray.length());
    assertEquals(FEED_ENTRY_TITLE, entry.getString("Title"));
    assertEquals(FEED_ENTRY_LINK, entry.getString("Link"));
    assertEquals(FEED_ENTRY_SUMMARY, entry.getString("Summary"));

    // Three lots of content, each with a width/height and type
    JSONObject media = entry.getJSONObject("Media");
    JSONArray contents = media.getJSONArray("Contents");

    assertEquals(3, contents.length());

    JSONObject contents1 = contents.getJSONObject(0);
    assertEquals(MEDIA_CONTENT_URL1, contents1.getString("URL"));
    assertEquals(MEDIA_CONTENT_TYPE, contents1.getString("Type"));
    assertEquals(MEDIA_CONTENT_WIDTH, contents1.getInt("Width"));
    assertEquals(MEDIA_CONTENT_HEIGHT, contents1.getInt("Height"));

    JSONObject contents2 = contents.getJSONObject(1);
    assertEquals(MEDIA_CONTENT_URL2, contents2.getString("URL"));
    assertEquals(MEDIA_CONTENT_TYPE, contents2.getString("Type"));
    assertEquals(MEDIA_CONTENT_WIDTH, contents2.getInt("Width"));
    assertEquals(MEDIA_CONTENT_HEIGHT, contents2.getInt("Height"));

    JSONObject contents3 = contents.getJSONObject(2);
    assertEquals(MEDIA_CONTENT_URL3, contents3.getString("URL"));
    assertEquals(MEDIA_CONTENT_TYPE, contents3.getString("Type"));
    assertEquals(MEDIA_CONTENT_WIDTH, contents3.getInt("Width"));
    assertEquals(MEDIA_CONTENT_HEIGHT, contents3.getInt("Height"));

    JSONObject thumbnail = media.getJSONObject("Thumbnail");
    assertEquals(MEDIA_THUMB_URL, thumbnail.getString("URL"));
    assertEquals(MEDIA_THUMB_WIDTH, thumbnail.getInt("Width"));
    assertEquals(MEDIA_THUMB_HEIGHT, thumbnail.getInt("Height"));
  }

  @Test
  public void parseAtom() throws Exception {
    JSONObject feed = processor.process(URL_ATOM, DATA_ATOM, true, 1);

    assertEquals(URL_ATOM, feed.getString("URL"));
    assertEquals(FEED_TITLE, feed.getString("Title"));
    assertEquals(FEED_AUTHOR, feed.getString("Author"));

    JSONArray entryArray = feed.getJSONArray("Entry");
    JSONObject entry = entryArray.getJSONObject(0);

    assertEquals(1, entryArray.length());
    assertEquals(FEED_ENTRY_TITLE, entry.getString("Title"));
    assertEquals(FEED_ENTRY_LINK, entry.getString("Link"));
    assertEquals(FEED_ENTRY_SUMMARY, entry.getString("Summary"));
  }

  @Test
  public void parseAtomMultiple() throws Exception {
    JSONObject feed = processor.process(URL_ATOM, DATA_ATOM, true, 2);
    JSONArray entryArray = feed.getJSONArray("Entry");
    assertEquals(2, entryArray.length());
  }

  @Test
  public void parseAtomDate() throws Exception {
    JSONObject feed = processor.process(URL_ATOM, DATA_ATOM, true, 2);
    JSONArray entryArray = feed.getJSONArray("Entry");
    assertEquals(TIMESTAMP, entryArray.getJSONObject(0).getLong("Date"));
    assertEquals(0, entryArray.getJSONObject(1).getLong("Date"));
  }

  @Test
  public void parseAtomNoSummaries() throws Exception {
    JSONObject feed = processor.process(URL_ATOM, DATA_ATOM, false, 1);
    feed.getJSONArray("Entry");
    JSONObject entry = feed.getJSONArray("Entry").getJSONObject(0);
    assertNull("Summary should not be returned when getSummaries is false",
        entry.optString("Summary", null));
  }

  @Test
  public void parseMediaAtom() throws Exception {
    JSONObject feed = processor.process(URL_ATOM, DATA_ATOM_WITH_MEDIARSS, true, 1);

    assertEquals(URL_ATOM, feed.getString("URL"));
    assertEquals(FEED_TITLE, feed.getString("Title"));
    assertEquals(FEED_AUTHOR, feed.getString("Author"));

    JSONArray entryArray = feed.getJSONArray("Entry");
    JSONObject entry = entryArray.getJSONObject(0);

    assertEquals(1, entryArray.length());
    assertEquals(FEED_ENTRY_TITLE, entry.getString("Title"));
    assertEquals(FEED_ENTRY_LINK, entry.getString("Link"));
    assertEquals(FEED_ENTRY_SUMMARY, entry.getString("Summary"));

    // Three lots of content, each with a width/height and type
    JSONObject media = entry.getJSONObject("Media");
    JSONArray contents = media.getJSONArray("Contents");

    assertEquals(3, contents.length());

    JSONObject contents1 = contents.getJSONObject(0);
    assertEquals(MEDIA_CONTENT_URL1, contents1.getString("URL"));
    assertEquals(MEDIA_CONTENT_TYPE, contents1.getString("Type"));
    assertEquals(MEDIA_CONTENT_WIDTH, contents1.getInt("Width"));
    assertEquals(MEDIA_CONTENT_HEIGHT, contents1.getInt("Height"));

    JSONObject contents2 = contents.getJSONObject(1);
    assertEquals(MEDIA_CONTENT_URL2, contents2.getString("URL"));
    assertEquals(MEDIA_CONTENT_TYPE, contents2.getString("Type"));
    assertEquals(MEDIA_CONTENT_WIDTH, contents2.getInt("Width"));
    assertEquals(MEDIA_CONTENT_HEIGHT, contents2.getInt("Height"));

    JSONObject contents3 = contents.getJSONObject(2);
    assertEquals(MEDIA_CONTENT_URL3, contents3.getString("URL"));
    assertEquals(MEDIA_CONTENT_TYPE, contents3.getString("Type"));
    assertEquals(MEDIA_CONTENT_WIDTH, contents3.getInt("Width"));
    assertEquals(MEDIA_CONTENT_HEIGHT, contents3.getInt("Height"));

    JSONObject thumbnail = media.getJSONObject("Thumbnail");
    assertEquals(MEDIA_THUMB_URL, thumbnail.getString("URL"));
    assertEquals(MEDIA_THUMB_WIDTH, thumbnail.getInt("Width"));
    assertEquals(MEDIA_THUMB_HEIGHT, thumbnail.getInt("Height"));
  }

  @Test(expected = GadgetException.class)
  public void parseBadXml() throws GadgetException {
    processor.process(URL_RSS, BAD_XML, false, 1);
  }

  @Test(expected = GadgetException.class)
  public void parseInvalidXml() throws GadgetException {
    processor.process(URL_RSS, INVALID_XML, false, 1);
  }
}

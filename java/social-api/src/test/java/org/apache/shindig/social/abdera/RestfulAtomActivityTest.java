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
package org.apache.shindig.social.abdera;

import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.Activity;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RestfulAtomActivityTest extends AbstractLargeRestfulTests {
  private Activity activity = SocialApiTestsGuiceModule
      .MockXmlStateFileFetcher.johnActivity;

  @Test
  public void testGetActivityOfUser() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@self/1?format=atom");
    checkForGoodAtomResponse(resp);
    Document<Entry> doc = resp.getDocument();
    validateActivityAtomEntryElements(doc.getRoot());
    // TODO Test the content element and more top level elements.
  }

  @Test
  public void testGetActivitiesOfUser() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@self?format=atom");
    checkForGoodAtomResponse(resp);
    Document<Feed> doc = resp.getDocument();
    String feedId = BASEURL + "/activities/john.doe/%40self?aid=";
    String title = RequestUrlTemplate.ACTIVITIES_OF_USER.toString();
    validateActivityAtomFeedElements(doc.getRoot(), title, feedId);
    // TODO Test all elements.
  }

  @Test
  public void testGetActivitiesOfFriendsOfUser() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@friends?format=atom");
    checkForGoodAtomResponse(resp);
    Document<Feed> doc = resp.getDocument();
    prettyPrint(doc);
    String feedId = BASEURL + "/activities/john.doe/%40friends?aid=";
    String title = RequestUrlTemplate.ACTIVITIES_OF_FRIENDS_OF_USER.toString();
    validateActivityAtomFeedElements(doc.getRoot(), title, feedId);
    // TODO Social graph seems to make everyone friends at this point.
    // TODO Test all elements.
  }

  private void validateActivityAtomEntryElements(Entry entry) {
    assertEquals("urn:guid:" + activity.getId(), entry.getId().toString());
    assertEquals("urn:guid:" + activity.getUserId(), entry.getAuthor().getUri()
        .toString());
    assertEquals(activity.getTitle(), entry.getTitle());
    assertEquals(activity.getBody(), entry.getSummary());
  }

  private void validateActivityAtomFeedElements(Feed feed, String title,
      String feedId) {
    assertEquals(feedId, feed.getId().toString());
    assertEquals(feedId, feed.getLink("self").getHref().toString());
    assertEquals(activity.getUserId(), feed.getAuthor().getName().toString());
    assertEquals(title, feed.getTitle());
  }
}

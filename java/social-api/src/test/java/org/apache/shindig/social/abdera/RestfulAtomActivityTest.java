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

import static org.junit.Assert.assertEquals;

import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.Activity;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RestfulAtomActivityTest extends AbstractLargeRestfulTests {
  private Activity activity;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    activity = SocialApiTestsGuiceModule.MockActivitiesService.basicActivity;
    List<Activity> activities = new ArrayList<Activity>();
    activities.add(activity);

    SocialApiTestsGuiceModule.MockActivitiesService
        .setActivities(new ResponseItem<List<Activity>>(activities));
    SocialApiTestsGuiceModule.MockActivitiesService
        .setActivity(new ResponseItem<Activity>(activity));
  }

  @Override
  @After
  public void tearDown() throws Exception {
    SocialApiTestsGuiceModule.MockActivitiesService.setActivities(null);
    SocialApiTestsGuiceModule.MockActivitiesService.setActivity(null);

    super.tearDown();
  }

  @Test
  public void testGetActivityOfUser() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@self/1?format=atom");
    checkForGoodAtomResponse(resp);
    Document<Entry> doc = resp.getDocument();
    Entry entry = doc.getRoot();
    assertEquals("urn:guid:" + activity.getId(), entry.getId().toString());
    assertEquals("urn:guid:" + activity.getUserId(), entry.getAuthor().getUri()
        .toString());
    assertEquals(activity.getTitle(), entry.getTitle());
    assertEquals(activity.getBody(), entry.getSummary());
    // TODO Test the content element and more top level elements.
    resp.release();
  }

  @Test
  public void testGetActivitiesOfUser() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@self?format=atom");
    checkForGoodAtomResponse(resp);
    // TODO Test all elements.
    resp.release();
  }

  @Test
  public void testGetActivitiesOfFriendsOfUser() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@friends?format=atom");
    checkForGoodAtomResponse(resp);
    // TODO Social graph seems to make everyone friends at this point.
    // TODO Test all elements.
    resp.release();
  }
}

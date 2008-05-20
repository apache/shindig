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

import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.ApiCollection;
import org.apache.shindig.social.opensocial.model.Person;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.ClientResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class RestfulAtomPeopleTests extends AbstractLargeRestfulTests {
  private List<Person> people;
  private ClientResponse resp;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    people = new ArrayList<Person>();
    people.add(SocialApiTestsGuiceModule.MockPeopleService.janeDoe);
    people.add(SocialApiTestsGuiceModule.MockPeopleService.simpleDoe);

    SocialApiTestsGuiceModule.MockPeopleService.setPeople(
        new ResponseItem<ApiCollection<Person>>(
            new ApiCollection<Person>(people)));

    SocialApiTestsGuiceModule.MockPeopleService.setPerson(
        new ResponseItem<Person>(SocialApiTestsGuiceModule
            .MockPeopleService.johnDoe));
  }

  @After
  public void tearDown() throws Exception {
    SocialApiTestsGuiceModule.MockPeopleService.setPeople(null);
    SocialApiTestsGuiceModule.MockPeopleService.setPerson(null);
    resp.release();
  }

  @Test
  public void testGetPeopleAtom() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Feed> doc = resp.getDocument();
    prettyPrint(doc);
    Feed feed = doc.getRoot();
    assertEquals(2, feed.getEntries().size());
  }

  @Test
  public void testGetIndirectPersonAtom() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all/jane.doe?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Entry> doc = resp.getDocument();
    Entry entry = doc.getRoot();
    prettyPrint(entry);

    Person expectedJaneDoe = people.get(0);
    assertEquals(expectedJaneDoe.getName().getUnstructured(), entry.getTitle());
  }

  @Test
  public void testGetInvalidPersonAtom() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all/nobody?format=atom");
    checkForBadResponse(resp);
  }

}

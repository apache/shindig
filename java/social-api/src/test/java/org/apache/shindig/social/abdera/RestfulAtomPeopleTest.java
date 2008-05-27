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
import org.apache.shindig.social.opensocial.model.Person;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;


public class RestfulAtomPeopleTest extends AbstractLargeRestfulTests {
  @Test
  public void testGetProfilesOfConnectionsOfUserAtom() throws Exception {
    resp = client.get(BASEURL + "/people/john.doe/@all?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Feed> doc = resp.getDocument();
    prettyPrint(doc);

    List<Entry> entries = doc.getRoot().getEntries();
    assertEquals(2, entries.size());
    String id1 = getIdFromXmlContent(entries.get(0)
        .getContentElement().getValue());
    String id2 = getIdFromXmlContent(entries.get(1)
        .getContentElement().getValue());

    Person janeDoe = SocialApiTestsGuiceModule.MockXmlStateFileFetcher.janeDoe;
    Person simpleDoe = SocialApiTestsGuiceModule.MockXmlStateFileFetcher.simpleDoe;
    // TODO: Simplify after we have implement sorting
    if (id1.equals(janeDoe.getId())) {
      assertEquals(simpleDoe.getId(), id2);
    } else {
      assertEquals(janeDoe.getId(), id2);
      assertEquals(simpleDoe.getId(), id1);
    }
  }

  @Test
  public void testGetProfilesOfFriendsOfUserAtom() throws Exception {
    resp = client.get(BASEURL + "/people/john.doe/@friends?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Feed> doc = resp.getDocument();
    prettyPrint(doc);
    Feed feed = doc.getRoot();
    assertEquals(2, feed.getEntries().size());
  }

  @Test
  public void testGetProfileOfConnectionOfUserAtom() throws Exception {
    resp = client.get(BASEURL + "/people/jane.doe/@all/john.doe?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Entry> doc = resp.getDocument();
    Entry entry = doc.getRoot();
    prettyPrint(entry);

    Person expectedJohnDoe = SocialApiTestsGuiceModule
        .MockXmlStateFileFetcher.johnDoe;
    assertEquals(expectedJohnDoe.getId(),
        getIdFromXmlContent(entry.getContentElement().getValue()));
  }

  @Test
  public void testGetProfileOfNotConnectionOfUserAtom() throws Exception {
    // jane is friends with john but not simple
    resp = client.get(BASEURL + "/people/jane.doe/@all/simple.doe?format=atom");
    checkForBadResponse(resp);
  }

  @Test
  public void testGetInvalidPerson() throws Exception {
    resp = client.get(BASEURL + "/people/john.doe/@all/nobody?format=atom");
    checkForBadResponse(resp);
  }

}

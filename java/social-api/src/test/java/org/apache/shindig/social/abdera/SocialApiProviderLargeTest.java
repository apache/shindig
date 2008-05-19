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

import org.apache.shindig.social.JettyServer;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.ApiCollection;
import org.apache.shindig.social.opensocial.model.Person;

import junit.framework.Assert;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Base;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.util.Constants;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.abdera.writer.Writer;
import org.apache.abdera.writer.WriterFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class SocialApiProviderLargeTest extends Assert {
  private static Logger logger =
      Logger.getLogger(SocialApiProviderLargeTest.class.getName());

  private static JettyServer server;
  private static Abdera abdera = Abdera.getInstance();
  private static AbderaClient client = new AbderaClient();

  private static int JETTY_PORT = 9002;
  private static String BASE = "/social/rest";
  private static String BASEURL = "http://localhost:" + JETTY_PORT + BASE;

  private List<Person> people;
  private ClientResponse resp;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    try {
      server = new JettyServer();
      server.start(JETTY_PORT, BASE + "/*");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownOnce() throws Exception {
    server.stop();
  }

  @Before
  public void setUp() throws Exception {
    people = new ArrayList<Person>();
    people.add(SocialApiTestsGuiceModule.MockPeopleService.janeDoe);
    people.add(SocialApiTestsGuiceModule.MockPeopleService.simpleDoe);

    SocialApiTestsGuiceModule.MockPeopleService.setPeople(
        new ResponseItem<ApiCollection<Person>>(
            new ApiCollection<Person>(people)));
  }

  @After
  public void tearDown() throws Exception {
    SocialApiTestsGuiceModule.MockPeopleService.setPeople(null);
    resp.release();
  }

  @Test
  public void testGetConnectionsForJohnDoe() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Feed> doc = resp.getDocument();
    prettyPrint(doc);
    Feed feed = doc.getRoot();
    assertEquals(2, feed.getEntries().size());
  }

  @Test
  public void testGetJaneDoeProfileForJohnDoe() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all/jane.doe?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Entry> doc = resp.getDocument();
    Entry entry = doc.getRoot();
    prettyPrint(entry);

    Person expectedJaneDoe = people.get(0);
    assertEquals(expectedJaneDoe.getName().getUnstructured(), entry.getTitle());
  }

  @Test
  public void testGetInvalidProfileForJohnDoe() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all/nobody?format=atom");
    checkForBadAtomResponse(resp);
  }

  protected void checkForGoodAtomResponse(ClientResponse response){
    assertNotNull(response);
    assertEquals(ResponseType.SUCCESS, response.getType());
    assertTrue(MimeTypeHelper.isMatch(response.getContentType().toString(),
        Constants.ATOM_MEDIA_TYPE));
  }

  protected void checkForBadAtomResponse(ClientResponse response){
    assertNotNull(response);
    assertEquals(ResponseType.CLIENT_ERROR, response.getType());
  }

  protected void prettyPrint(Base doc) throws IOException {
    WriterFactory writerFactory = abdera.getWriterFactory();
    Writer writer = writerFactory.getWriter("prettyxml");
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    writer.writeTo(doc, os);
    logger.fine(os.toString("utf8"));
  }

}

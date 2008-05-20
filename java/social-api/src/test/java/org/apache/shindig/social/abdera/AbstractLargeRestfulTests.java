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

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Base;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.util.Constants;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.abdera.writer.Writer;
import org.apache.abdera.writer.WriterFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;


public abstract class AbstractLargeRestfulTests {
  private static Logger logger =
      Logger.getLogger(AbstractLargeRestfulTests.class.getName());

  private static JettyServer server;
  private static Abdera abdera = Abdera.getInstance();
  protected static AbderaClient client = new AbderaClient();

  private static int JETTY_PORT = 9002;
  private static String BASE = "/social/rest";
  protected static String BASEURL = "http://localhost:" + JETTY_PORT + BASE;

  protected ClientResponse resp;

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
  }

  @After
  public void tearDown() throws Exception {
    resp.release();
  }

  protected void checkForGoodResponse(ClientResponse response,
      String mimeType) {
    assertNotNull(response);
    assertEquals(Response.ResponseType.SUCCESS, response.getType());
    assertTrue(MimeTypeHelper.isMatch(response.getContentType().toString(),
        mimeType));
  }

  protected void checkForGoodJsonResponse(ClientResponse response){
    checkForGoodResponse(response, "application/json");
  }

  protected void checkForGoodAtomResponse(ClientResponse response){
    checkForGoodResponse(response, Constants.ATOM_MEDIA_TYPE);
  }

  protected void checkForBadResponse(ClientResponse response){
    assertNotNull(response);
    assertEquals(Response.ResponseType.CLIENT_ERROR, response.getType());
  }

  protected JSONObject getJson(ClientResponse resp) throws IOException,
      JSONException {
    BufferedReader reader = new BufferedReader(resp.getReader());

    StringBuffer json = new StringBuffer();
    String line = reader.readLine();
    while (line != null) {
      json.append(line);
      line = reader.readLine();
    }

    logger.fine(json.toString());
    return new JSONObject(json.toString());
  }

  protected void prettyPrint(Base doc) throws IOException {
    WriterFactory writerFactory = abdera.getWriterFactory();
    Writer writer = writerFactory.getWriter("prettyxml");
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    writer.writeTo(doc, os);
    logger.fine(os.toString("utf8"));
  }

}

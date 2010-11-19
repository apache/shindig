package org.apache.shindig.extras.as.dataservice.integration;

import org.apache.shindig.extras.as.core.model.ActivityEntryImpl;
import org.apache.shindig.extras.as.core.model.ActivityObjectImpl;
import org.apache.shindig.extras.as.opensocial.model.ActivityEntry;
import org.apache.shindig.extras.as.opensocial.model.ActivityObject;
import org.apache.shindig.protocol.ContentTypes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class RestfulJsonActivityEntryTest extends AbstractActivityStreamsRestfulTests {
  ActivityEntry johnsEntry;

  @Before
  public void restfulJsonActivityEntryTestBefore() throws Exception {
    ActivityObject actor = new ActivityObjectImpl();
    actor.setId("john.doe");
    actor.setDisplayName("John Doe");
    
    ActivityObject object = new ActivityObjectImpl();
    object.setId("myObjectId123");
    object.setDisplayName("My Object");
    
    johnsEntry = new ActivityEntryImpl();
    johnsEntry.setTitle("This is my ActivityEntry!");
    johnsEntry.setBody("ActivityStreams are so much fun!");
    johnsEntry.setActor(actor);
    johnsEntry.setObject(object);
  }

  /**
   * Expected response for an activity in json:
   * { 'entry' : {
   *     'title' : 'This is my ActivityEntry!',
   *     'body' : 'ActivityStreams are so much fun!',
   *     'actor' : {
   *       'id' : 'john.doe',
   *       'displayName' : 'John Doe'
   *     },
   *     'object' : {
   *       'id' : 'myObjectId123',
   *       'displayName' : 'My Object'
   *     }
   *   }
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityEntryJson() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self/@app/myObjectId123", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);
    assertActivityEntriesEqual(johnsEntry, result.getJSONObject("entry"));
  }

  /**
   * Expected response for a list of activities in json:
   *
   * {
   *  "totalResults" : 1,
   *  "startIndex" : 0
   *  "itemsPerPage" : 10 // Note: the js doesn't support paging. Should rest?
   *  "entry" : [
   *     {<entry>} // layed out like above
   *  ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityEntriesJson() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);

    assertEquals(1, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
    assertActivityEntriesEqual(johnsEntry, result.getJSONArray("entry").getJSONObject(0));
  }

  /**
   * Expected response for a list of activities in json:
   *
   * {
   *  "totalResults" : 3,
   *  "startIndex" : 0
   *  "itemsPerPage" : 10 // Note: the js doesn't support paging. Should rest?
   *  "entry" : [
   *     {<entry>} // layed out like above, except for jane.doe
   *  ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetFriendsActivityEntriesJson() throws Exception {
    String resp = getResponse("/activitystreams/jane.doe/@friends", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);

    assertEquals(1, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
  }

  private void assertActivityEntriesEqual(ActivityEntry entry, JSONObject result)
      throws JSONException {
    assertEquals(entry.getTitle(), result.getString("title"));
    assertEquals(entry.getBody(), result.getString("body"));
    assertEquals(entry.getActor().getId(), result.getJSONObject("actor").getString("id"));
    assertEquals(entry.getActor().getDisplayName(), result.getJSONObject("actor").getString("displayName"));
    assertEquals(entry.getObject().getId(), result.getJSONObject("object").getString("id"));
    assertEquals(entry.getObject().getDisplayName(), result.getJSONObject("object").getString("displayName"));
  }

  @Test
  public void testCreateActivityEntry() throws Exception {
    String postData = "{title : 'hi mom!', body : 'and dad.', object : {id: '1'}}";
    // Create the activity entry
    getResponse("/activitystreams/john.doe/@self", "POST", postData, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    // Verify it can be retrieved
    String resp = getResponse("/activitystreams/john.doe/@self", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);

    assertEquals(2, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));

    JSONArray entries = result.getJSONArray("entry");
    int newEntryIndex = 0;
    if (entries.getJSONObject(0).getJSONObject("object").getString("id")
        .equals("myObjectId123")) {
      newEntryIndex = 1;
    }

    JSONObject jsonEntry = entries.getJSONObject(newEntryIndex);
    assertEquals("hi mom!", jsonEntry.getString("title"));
    assertEquals("and dad.", jsonEntry.getString("body"));
    assertEquals("1", jsonEntry.getJSONObject("object").getString("id"));
  }

  // TODO: Add tests for the fields= parameter
}

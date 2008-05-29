package org.apache.shindig.social.samplecontainer;

import org.apache.shindig.social.abdera.AbstractLargeRestfulTests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import org.json.JSONObject;
import org.junit.Test;

public class SampleContainerRoutesTest extends AbstractLargeRestfulTests {

  /**
   * Expected response for dump state in json:
   *
   * {
   *  "people" : {"john.doe" : {<fields>}, ...},
   *  "friendIds" : {"john.doe" : ["jane.doe", "simple.doe"],
   *                 "jane.doe" : ["john.doe"]},
   *  "data" : {"john.doe" : {"count" : "0"}, ...},
   *  "activities" : {"john.doe" : [{<activity>}], ...}
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testDumpState() throws Exception {
    resp = client.get(BASEURL + "/samplecontainer/dumpstate");
    checkForGoodJsonResponse(resp);

    JSONObject json = getJson(resp);

    JSONObject people = json.getJSONObject("people");
    assertEquals(3, people.length());
    assertNotNull(people.getJSONObject("john.doe"));
    assertNotNull(people.getJSONObject("jane.doe"));
    assertNotNull(people.getJSONObject("simple.doe"));

    JSONObject friends = json.getJSONObject("friendIds");
    assertEquals(2, friends.length());
    assertEquals(2, friends.getJSONArray("john.doe").length());
    assertEquals(1, friends.getJSONArray("jane.doe").length());
    assertEquals("john.doe", friends.getJSONArray("jane.doe").getString(0));

    JSONObject data = json.getJSONObject("data");
    assertEquals(3, data.length());
    assertEquals("0", data.getJSONObject("john.doe").getString("count"));
    assertEquals("5", data.getJSONObject("jane.doe").getString("count"));
    assertEquals("7", data.getJSONObject("simple.doe").getString("count"));

    JSONObject activities = json.getJSONObject("activities");
    assertEquals(3, activities.length());
    assertNotNull(activities.getJSONArray("john.doe"));
    assertNotNull(activities.getJSONArray("jane.doe"));
    assertNotNull(activities.getJSONArray("simple.doe"));
  }

  /**
   * Expected response for app data in json:
   *
   * { "success" : true }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testSetState() throws Exception {
    // TODO: Test this with a post instead of a get.
    // I couldn't figure out how to get the post body passed through correctly
    resp = client.get(BASEURL + "/samplecontainer/setstate?fileurl=hello.com");

    checkForGoodJsonResponse(resp);
    assertTrue(getJson(resp).getBoolean("success"));
  }

  /**
   * Expected response for app data in json:
   *
   * { "success" : true }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testSetEvilness() throws Exception {
    resp = client.get(BASEURL + "/samplecontainer/setevilness/true");
    checkForGoodJsonResponse(resp);
    assertTrue(getJson(resp).getBoolean("success"));

    resp = client.get(BASEURL + "/samplecontainer/setevilness/false");
    checkForGoodJsonResponse(resp);
    assertTrue(getJson(resp).getBoolean("success"));

    resp = client.get(BASEURL + "/samplecontainer/setevilness/ahhhhhh!");
    checkForBadResponse(resp);
  }
}

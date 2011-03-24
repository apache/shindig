package org.apache.shindig.social.dataservice.integration;

import org.apache.shindig.protocol.ContentTypes;
import org.junit.Test;

/**
 * Tests the ATOM serialization of ActivityStreams.
 */
public class RestfulAtomActivityEntryTest extends AbstractLargeRestfulTests{
  
  private static final String FIXTURE_LOC = "src/test/java/org/apache/shindig/social/dataservice/integration/fixtures/";
  
  @Test
  public void testGetActivityEntryAtomById() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self/1/object1", "GET", "atom", ContentTypes.OUTPUT_ATOM_CONTENT_TYPE);
    String expected = TestUtils.loadTestFixture(FIXTURE_LOC + "ActivityEntryAtomId.xml");
    assertTrue(TestUtils.xmlsEqual(expected, resp));
  }
  
  @Test
  public void testGetActivityEntryAtomByIds() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self/1/object1,object2", "GET", "atom", ContentTypes.OUTPUT_ATOM_CONTENT_TYPE);
    String expected = TestUtils.loadTestFixture(FIXTURE_LOC + "ActivityEntryAtomIds.xml");
    assertTrue(TestUtils.xmlsEqual(expected, resp));
  }
  
  @Test
  public void testCreateActivityEntryAtom() throws Exception {
    // TODO: Creating activity from ATOM not fully supported
  }
}

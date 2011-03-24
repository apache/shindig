package org.apache.shindig.social.dataservice.integration;

import org.apache.shindig.protocol.ContentTypes;
import org.junit.Test;

/**
 * Tests the XML serialization of ActivityStreams.
 */
public class RestfulXmlActivityEntryTest extends AbstractLargeRestfulTests{
  
  private static final String FIXTURE_LOC = "src/test/java/org/apache/shindig/social/dataservice/integration/fixtures/";
  
  @Test
  public void testGetActivityEntryXmlById() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self/1/object1", "GET", "xml", ContentTypes.OUTPUT_XML_CONTENT_TYPE);
    String expected = TestUtils.loadTestFixture(FIXTURE_LOC + "ActivityEntryXmlId.xml");
    assertTrue(TestUtils.xmlsEqual(expected, resp));
  }
  
  @Test
  public void testGetActivityEntryXmlByIds() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self/1/object1,object2", "GET", "xml", ContentTypes.OUTPUT_XML_CONTENT_TYPE);
    String expected = TestUtils.loadTestFixture(FIXTURE_LOC + "ActivityEntryXmlIds.xml");
    assertTrue(TestUtils.xmlsEqual(expected, resp));
  }
  
  @Test
  public void testCreateActivityEntryXml() throws Exception {
    // TODO: Creating activity from XML not fully supported
  }
}

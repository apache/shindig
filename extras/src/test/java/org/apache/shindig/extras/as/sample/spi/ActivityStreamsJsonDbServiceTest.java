package org.apache.shindig.extras.as.sample.spi;

import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.extras.as.opensocial.model.ActivityEntry;
import org.apache.shindig.extras.as.sample.ActivityStreamsJsonDbService;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests the sample ActivityStreamsJsonDbService.
 */
public class ActivityStreamsJsonDbServiceTest extends Assert {
  private ActivityStreamsJsonDbService db;

  private static final UserId CANON_USER = new UserId(UserId.Type.userId, "canonical");
  private static final UserId JOHN_DOE = new UserId(UserId.Type.userId, "john.doe");

  private static final GroupId SELF_GROUP = new GroupId(GroupId.Type.self, null);
  private static final String APP_ID = "1";
  
  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(new SocialApiTestsGuiceModule());
    db = injector.getInstance(ActivityStreamsJsonDbService.class);
  }

  @Test
  public void testGetExpectedActivityEntries() throws Exception {
    RestfulCollection<ActivityEntry> responseItem = db.getActivityEntries(
        ImmutableSet.of(JOHN_DOE), SELF_GROUP, APP_ID, Collections.<String>emptySet(), null,
        new FakeGadgetToken()).get();
    assertSame(1, responseItem.getTotalResults());
  }

  @Test
  public void testGetExpectedActivityEntriesForPlural() throws Exception {
    RestfulCollection<ActivityEntry> responseItem = db.getActivityEntries(
        ImmutableSet.of(CANON_USER, JOHN_DOE), SELF_GROUP, APP_ID, Collections.<String>emptySet(), null,
        new FakeGadgetToken()).get();
    assertSame(1, responseItem.getTotalResults());
  }

  @Test
  public void testGetExpectedActivityEntry() throws Exception {
    ActivityEntry entry = db.getActivityEntry(JOHN_DOE, SELF_GROUP, APP_ID,
        ImmutableSet.of("body"), "myObjectId123", new FakeGadgetToken()).get();
    assertNotNull(entry);
    // Check that some fields are fetched and others are not
    assertNotNull(entry.getBody());
    assertNull(entry.getPostedTime());
  }

  @Test
  public void testDeleteExpectedActivityEntry() throws Exception {
    db.deleteActivityEntries(JOHN_DOE, SELF_GROUP, APP_ID, ImmutableSet.of(APP_ID),
        new FakeGadgetToken());

    // Try to fetch the activity
    try {
      db.getActivityEntry(
          JOHN_DOE, SELF_GROUP, APP_ID,
          ImmutableSet.of("body"), APP_ID, new FakeGadgetToken()).get();
      fail();
    } catch (ProtocolException sse) {
      assertEquals(HttpServletResponse.SC_BAD_REQUEST, sse.getCode());
    }
  }
}

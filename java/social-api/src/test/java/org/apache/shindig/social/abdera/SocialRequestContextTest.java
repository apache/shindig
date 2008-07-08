package org.apache.shindig.social.abdera;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.common.collect.Sets;

import junit.framework.TestCase;

import org.apache.shindig.social.dataservice.PersonService;

import org.apache.abdera.protocol.server.RequestContext;

import org.junit.Before;

public class SocialRequestContextTest extends TestCase {
  private SocialRequestContext socrequest;
  private RequestContext request;

  @Before
  public void setUp() throws Exception {
    request = createMock(RequestContext.class);
    socrequest = new SocialRequestContext(request);
  }

  public void tearDown() {
    verify(request);
  }

  public void testGetStartIndexNoParam() {
    expect(request.getParameter(SocialRequestContext.START_INDEX)).andReturn(null);
    replay(request);
    assertEquals(SocialRequestContext.DEFAULT_START_INDEX, socrequest.getStartIndex());
  }

  public void testGetStartIndexWithParam() {
    expect(request.getParameter(SocialRequestContext.START_INDEX)).andReturn("20");
    replay(request);
    assertEquals(20, socrequest.getStartIndex());
  }

  public void testGetCountNoParam() {
    expect(request.getParameter(SocialRequestContext.COUNT)).andReturn(null);
    replay(request);
    assertEquals(SocialRequestContext.DEFAULT_COUNT, socrequest.getCount());
  }

  public void testGetCountWithParam() {
    expect(request.getParameter(SocialRequestContext.COUNT)).andReturn("20");
    replay(request);
    assertEquals(20, socrequest.getCount());
  }

  public void testGetSortOrderNoParam() {
    expect(request.getParameter(SocialRequestContext.ORDER_BY)).andReturn(null);
    replay(request);
    assertEquals(PersonService.SortOrder.topFriends, socrequest.getOrderBy());
  }

  public void testGetOrderByWithParam() {
    expect(request.getParameter(SocialRequestContext.ORDER_BY)).andReturn("name");
    replay(request);
    assertEquals(PersonService.SortOrder.name, socrequest.getOrderBy());
  }

  public void testGetFilterTypeNoParam() {
    expect(request.getParameter(SocialRequestContext.FILTER_BY)).andReturn(null);
    replay(request);
    assertEquals(PersonService.FilterType.all, socrequest.getFilterBy());
  }

  public void testGetFilterTypeWithParam() {
    expect(request.getParameter(SocialRequestContext.FILTER_BY)).andReturn("hasApp");
    replay(request);
    assertEquals(PersonService.FilterType.hasApp, socrequest.getFilterBy());
  }

  public void testGetFieldsNoParam() {
    expect(request.getParameter(SocialRequestContext.FIELDS)).andReturn(null);
    replay(request);
    assertEquals(SocialRequestContext.DEFAULT_PERSON_FIELDS, socrequest.getFields());
  }

  public void testGetFieldsWithParam() {
    expect(request.getParameter(SocialRequestContext.FIELDS)).andReturn("blah,ha");
    replay(request);
    assertEquals(Sets.newHashSet("blah", "ha"), socrequest.getFields());
  }
}

package org.apache.shindig.social.abdera;

import com.google.common.collect.Sets;

import org.apache.shindig.social.dataservice.PersonService;
import org.apache.shindig.social.opensocial.model.Person;

import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.RequestContextWrapper;

import java.util.Set;

public class SocialRequestContext extends RequestContextWrapper {

  // Common OpenSocial RESTful parameters
  public static final String START_INDEX = "startIndex";
  public static final String COUNT = "count";
  public static final String ORDER_BY = "orderBy";
  public static final String FILTER_BY = "filterBy";
  public static final String FIELDS = "fields";
  public static final String SECURITY_TOKEN_PARAM = "st";

  // OpenSocial parameter defaults
  public static final int DEFAULT_START_INDEX = 0;
  public static final int DEFAULT_COUNT = 20;
  public static final Set<String> DEFAULT_PERSON_FIELDS = Sets.newHashSet(
      Person.Field.ID.toString(),
      Person.Field.NAME.toString(),
      Person.Field.THUMBNAIL_URL.toString());
  
  public SocialRequestContext(RequestContext request) {
    super(request);
  }

 //these parameter handling methods are from social/dataservice/RequestItem.java
  public int getStartIndex() {
    String startIndex = request.getParameter(START_INDEX);
    return startIndex == null ? DEFAULT_START_INDEX : Integer.valueOf(startIndex);
  }

  public int getCount() {
    String count = request.getParameter(COUNT);
    return count == null ? DEFAULT_COUNT : Integer.valueOf(count);
  }

  public PersonService.SortOrder getOrderBy() {
    String orderBy = request.getParameter(ORDER_BY);
    return orderBy == null
        ? PersonService.SortOrder.topFriends
        : PersonService.SortOrder.valueOf(orderBy);
  }

  public PersonService.FilterType getFilterBy() {
    String filterBy = request.getParameter(FILTER_BY);
    return filterBy == null
        ? PersonService.FilterType.all
        : PersonService.FilterType.valueOf(filterBy);
  }
 
  public Set<String> getFields() {
    String paramValue = request.getParameter(FIELDS);
    return paramValue == null
        ? DEFAULT_PERSON_FIELDS
        : Sets.newHashSet(paramValue.split(","));
  }
  
}

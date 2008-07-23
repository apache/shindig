/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.core.util.EnumUtil;

import com.google.inject.ImplementedBy;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * see
 * http://code.google.com/apis/opensocial/docs/0.8/reference/#opensocial.Person.Field
 * for all field meanings. All fields are represented in the js api at this time except for
 * lastUpdated. This field is currently only in the RESTful spec.
 *
 */

@ImplementedBy(PersonImpl.class)

public interface Person {

  public static enum Field {
    ABOUT_ME("aboutMe"),
    ACTIVITIES("activities"),
    ADDRESSES("addresses"),
    AGE("age"),
    BODY_TYPE("bodyType"),
    BOOKS("books"),
    CARS("cars"),
    CHILDREN("children"),
    CURRENT_LOCATION("currentLocation"),
    DATE_OF_BIRTH("dateOfBirth"),
    DRINKER("drinker"),
    EMAILS("emails"),
    ETHNICITY("ethnicity"),
    FASHION("fashion"),
    FOOD("food"),
    GENDER("gender"),
    HAPPIEST_WHEN("happiestWhen"),
    HAS_APP("hasApp"),
    HEROES("heroes"),
    HUMOR("humor"),
    ID("id"),
    INTERESTS("interests"),
    JOB_INTERESTS("jobInterests"),
    JOBS("jobs"),
    LANGUAGES_SPOKEN("languagesSpoken"),
    LAST_UPDATED("updated"), /** Needed to support the RESTful api. **/
    LIVING_ARRANGEMENT("livingArrangement"),
    LOOKING_FOR("lookingFor"),
    MOVIES("movies"),
    MUSIC("music"),
    NAME("name"),
    NETWORKPRESENCE("networkPresence"),
    NICKNAME("nickname"),
    PETS("pets"),
    PHONE_NUMBERS("phoneNumbers"),
    POLITICAL_VIEWS("politicalViews"),
    PROFILE_SONG("profileSong"),
    PROFILE_URL("profileUrl"),
    PROFILE_VIDEO("profileVideo"),
    QUOTES("quotes"),
    RELATIONSHIP_STATUS("relationshipStatus"),
    RELIGION("religion"),
    ROMANCE("romance"),
    SCARED_OF("scaredOf"),
    SCHOOLS("schools"),
    SEXUAL_ORIENTATION("sexualOrientation"),
    SMOKER("smoker"),
    SPORTS("sports"),
    STATUS("status"),
    TAGS("tags"),
    THUMBNAIL_URL("thumbnailUrl"),
    TIME_ZONE("timeZone"),
    TURN_OFFS("turnOffs"),
    TURN_ONS("turnOns"),
    TV_SHOWS("tvShows"),
    URLS("urls");

    private final String jsonString;

    public static final Set<String> DEFAULT_FIELDS =
        EnumUtil.getEnumStrings(ID, NAME, THUMBNAIL_URL);

    public static final Set<String> ALL_FIELDS =
        EnumUtil.getEnumStrings(Field.values());

    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  String getAboutMe();

  void setAboutMe(String aboutMe);

  List<String> getActivities();

  void setActivities(List<String> activities);

  List<Address> getAddresses();

  void setAddresses(List<Address> addresses);

  Integer getAge();

  void setAge(Integer age);

  BodyType getBodyType();

  void setBodyType(BodyType bodyType);

  List<String> getBooks();

  void setBooks(List<String> books);

  List<String> getCars();

  void setCars(List<String> cars);

  String getChildren();

  void setChildren(String children);

  Address getCurrentLocation();

  void setCurrentLocation(Address currentLocation);

  Date getDateOfBirth();

  void setDateOfBirth(Date dateOfBirth);

  Enum<Enum.Drinker> getDrinker();

  void setDrinker(Enum<Enum.Drinker> newDrinker);

  List<Email> getEmails();

  void setEmails(List<Email> emails);

  String getEthnicity();

  void setEthnicity(String ethnicity);

  String getFashion();

  void setFashion(String fashion);

  List<String> getFood();

  void setFood(List<String> food);

  Enum<Enum.Gender> getGender();

  void setGender(Enum<Enum.Gender> newGender);

  String getHappiestWhen();

  void setHappiestWhen(String happiestWhen);

  Boolean getHasApp();

  void setHasApp(Boolean hasApp);

  List<String> getHeroes();

  void setHeroes(List<String> heroes);

  String getHumor();

  void setHumor(String humor);

  String getId();

  void setId(String id);

  List<String> getInterests();

  void setInterests(List<String> interests);

  String getJobInterests();

  void setJobInterests(String jobInterests);

  List<Organization> getJobs();

  void setJobs(List<Organization> jobs);

  List<String> getLanguagesSpoken();

  void setLanguagesSpoken(List<String> languagesSpoken);

  Date getUpdated();

  void setUpdated(Date updated);

  String getLivingArrangement();

  void setLivingArrangement(String livingArrangement);

  String getLookingFor();

  void setLookingFor(String lookingFor);

  List<String> getMovies();

  void setMovies(List<String> movies);

  List<String> getMusic();

  void setMusic(List<String> music);

  Name getName();

  void setName(Name name);

  Enum<Enum.NetworkPresence> getNetworkPresence();

  void setNetworkPresence(Enum<Enum.NetworkPresence> networkPresence);

  String getNickname();

  void setNickname(String nickname);

  String getPets();

  void setPets(String pets);

  List<Phone> getPhoneNumbers();

  void setPhoneNumbers(List<Phone> phoneNumbers);

  String getPoliticalViews();

  void setPoliticalViews(String politicalViews);

  Url getProfileSong();

  void setProfileSong(Url profileSong);

  String getProfileUrl();

  void setProfileUrl(String profileUrl);

  Url getProfileVideo();

  void setProfileVideo(Url profileVideo);

  List<String> getQuotes();

  void setQuotes(List<String> quotes);

  String getRelationshipStatus();

  void setRelationshipStatus(String relationshipStatus);

  String getReligion();

  void setReligion(String religion);

  String getRomance();

  void setRomance(String romance);

  String getScaredOf();

  void setScaredOf(String scaredOf);

  List<Organization> getSchools();

  void setSchools(List<Organization> schools);

  String getSexualOrientation();

  void setSexualOrientation(String sexualOrientation);

  Enum<Enum.Smoker> getSmoker();

  void setSmoker(Enum<Enum.Smoker> newSmoker);

  List<String> getSports();

  void setSports(List<String> sports);

  String getStatus();

  void setStatus(String status);

  List<String> getTags();

  void setTags(List<String> tags);

  String getThumbnailUrl();

  void setThumbnailUrl(String thumbnailUrl);

  Long getTimeZone();

  void setTimeZone(Long timeZone);

  List<String> getTurnOffs();

  void setTurnOffs(List<String> turnOffs);

  List<String> getTurnOns();

  void setTurnOns(List<String> turnOns);

  List<String> getTvShows();

  void setTvShows(List<String> tvShows);

  List<Url> getUrls();

  void setUrls(List<Url> urls);

  boolean getIsOwner();

  void setIsOwner(boolean isOwner);

  boolean getIsViewer();

  void setIsViewer(boolean isViewer);

}

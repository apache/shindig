/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.social.core.model;

import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.social.opensocial.model.Account;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Drinker;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.LookingFor;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.NetworkPresence;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Smoker;
import org.apache.shindig.social.opensocial.model.Url;

import com.google.common.collect.Lists;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Default Implementation of the Person object in the model.
 */
public class PersonImpl implements Person {
  private String aboutMe;
  private List<Account> accounts;
  private List<String> activities;
  private List<Address> addresses;
  private Integer age;
  private Map<String, ?> appData;
  private Date birthday;
  private BodyType bodyType;
  private List<String> books;
  private List<String> cars;
  private String children;
  private Address currentLocation;
  private String displayName;
  private Enum<Drinker> drinker;
  private List<ListField> emails;
  private String ethnicity;
  private String fashion;
  private List<String> food;
  private Gender gender;
  private String happiestWhen;
  private Boolean hasApp;
  private List<String> heroes;
  private String humor;
  private String id;
  private List<ListField> ims;
  private List<String> interests;
  private String jobInterests;
  private List<String> languagesSpoken;
  private String livingArrangement;
  private List<Enum<LookingFor>> lookingFor;
  private List<String> movies;
  private List<String> music;
  private Name name;
  private Enum<NetworkPresence> networkPresence;
  private String nickname;
  private List<Organization> organizations;
  private String pets;
  private List<ListField> phoneNumbers;
  private List<ListField> photos;
  private String politicalViews;
  private String preferredUsername;
  private Url profileSong;
  private Url profileVideo;
  private List<String> quotes;
  private String relationshipStatus;
  private String religion;
  private String romance;
  private String scaredOf;
  private String sexualOrientation;
  private Enum<Smoker> smoker;
  private List<String> sports;
  private String status;
  private List<String> tags;
  private Long utcOffset;
  private List<String> turnOffs;
  private List<String> turnOns;
  private List<String> tvShows;
  private Date updated;
  private List<Url> urls;

  // Note: Not in the opensocial js person object directly
  private boolean isOwner = false;
  private boolean isViewer = false;

  public PersonImpl() {
  }

  /**
   * A constructor which contains all of the required fields on a person object
   * @param id The id of the person
   * @param displayName The displayName of the person
   * @param name The person's name broken down into components
   */
  public PersonImpl(String id, String displayName, Name name) {
    this.id = id;
    this.displayName = displayName;
    this.name = name;
  }

  public String getAboutMe() {
    return aboutMe;
  }

  public void setAboutMe(String aboutMe) {
    this.aboutMe = aboutMe;
  }

  public List<Account> getAccounts() {
    return accounts;
  }

  public void setAccounts(List<Account> accounts) {
    this.accounts = accounts;
  }

  public List<String> getActivities() {
    return activities;
  }

  public void setActivities(List<String> activities) {
    this.activities = activities;
  }

  public List<Address> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<Address> addresses) {
    this.addresses = addresses;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public Map<String, ?> getAppData() {
    return this.appData;
  }

  public void setAppData(Map<String, ?> appData) {
    this.appData = appData;
  }

  public BodyType getBodyType() {
    return bodyType;
  }

  public void setBodyType(BodyType bodyType) {
    this.bodyType = bodyType;
  }

  public List<String> getBooks() {
    return books;
  }

  public void setBooks(List<String> books) {
    this.books = books;
  }

  public List<String> getCars() {
    return cars;
  }

  public void setCars(List<String> cars) {
    this.cars = cars;
  }

  public String getChildren() {
    return children;
  }

  public void setChildren(String children) {
    this.children = children;
  }

  public Address getCurrentLocation() {
    return currentLocation;
  }

  public void setCurrentLocation(Address currentLocation) {
    this.currentLocation = currentLocation;
  }

  public Date getBirthday() {
    if (birthday == null) {
      return null;
    }
    return new Date(birthday.getTime());
  }

  public void setBirthday(Date birthday) {
    if (birthday == null) {
      this.birthday = null;
    } else {
      this.birthday = new Date(birthday.getTime());
    }
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public org.apache.shindig.protocol.model.Enum<Drinker> getDrinker() {
    return this.drinker;
  }

  public void setDrinker(Enum<Drinker> newDrinker) {
    this.drinker = newDrinker;
  }

  public List<ListField> getEmails() {
    return emails;
  }

  public void setEmails(List<ListField> emails) {
    this.emails = emails;
  }

  public String getEthnicity() {
    return ethnicity;
  }

  public void setEthnicity(String ethnicity) {
    this.ethnicity = ethnicity;
  }

  public String getFashion() {
    return fashion;
  }

  public void setFashion(String fashion) {
    this.fashion = fashion;
  }

  public List<String> getFood() {
    return food;
  }

  public void setFood(List<String> food) {
    this.food = food;
  }

  public Gender getGender() {
    return gender;
  }

  public void setGender(Gender newGender) {
    this.gender = newGender;
  }

  public String getHappiestWhen() {
    return happiestWhen;
  }

  public void setHappiestWhen(String happiestWhen) {
    this.happiestWhen = happiestWhen;
  }

  public Boolean getHasApp() {
    return hasApp;
  }

  public void setHasApp(Boolean hasApp) {
    this.hasApp = hasApp;
  }

  public List<String> getHeroes() {
    return heroes;
  }

  public void setHeroes(List<String> heroes) {
    this.heroes = heroes;
  }

  public String getHumor() {
    return humor;
  }

  public void setHumor(String humor) {
    this.humor = humor;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<ListField> getIms() {
    return ims;
  }

  public void setIms(List<ListField> ims) {
    this.ims = ims;
  }

  public List<String> getInterests() {
    return interests;
  }

  public void setInterests(List<String> interests) {
    this.interests = interests;
  }

  public String getJobInterests() {
    return jobInterests;
  }

  public void setJobInterests(String jobInterests) {
    this.jobInterests = jobInterests;
  }

  public List<String> getLanguagesSpoken() {
    return languagesSpoken;
  }

  public void setLanguagesSpoken(List<String> languagesSpoken) {
    this.languagesSpoken = languagesSpoken;
  }

  public Date getUpdated() {
    if (updated == null) {
      return null;
    }
    return new Date(updated.getTime());
  }

  public void setUpdated(Date updated) {
    if (updated == null) {
      this.updated = null;
    } else {
      this.updated = new Date(updated.getTime());
    }
  }

  public String getLivingArrangement() {
    return livingArrangement;
  }

  public void setLivingArrangement(String livingArrangement) {
    this.livingArrangement = livingArrangement;
  }

  public List<Enum<LookingFor>> getLookingFor() {
    return lookingFor;
  }

  public void setLookingFor(List<Enum<LookingFor>> lookingFor) {
    this.lookingFor = lookingFor;
  }

  public List<String> getMovies() {
    return movies;
  }

  public void setMovies(List<String> movies) {
    this.movies = movies;
  }

  public List<String> getMusic() {
    return music;
  }

  public void setMusic(List<String> music) {
    this.music = music;
  }

  public Name getName() {
    return name;
  }

  public void setName(Name name) {
    this.name = name;
  }

  public Enum<NetworkPresence> getNetworkPresence() {
    return networkPresence;
  }

  public void setNetworkPresence(Enum<NetworkPresence> networkPresence) {
    this.networkPresence = networkPresence;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public List<Organization> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(List<Organization> organizations) {
    this.organizations = organizations;
  }

  public String getPets() {
    return pets;
  }

  public void setPets(String pets) {
    this.pets = pets;
  }

  public List<ListField> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(List<ListField> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  public List<ListField> getPhotos() {
    return photos;
  }

  public void setPhotos(List<ListField> photos) {
    this.photos = photos;
  }

  public String getPoliticalViews() {
    return politicalViews;
  }

  public void setPoliticalViews(String politicalViews) {
    this.politicalViews = politicalViews;
  }

  public String getPreferredUsername() {
    return preferredUsername;
  }

  public void setPreferredUsername(String preferredUsername) {
    this.preferredUsername = preferredUsername;
  }

  public Url getProfileSong() {
    return profileSong;
  }

  public void setProfileSong(Url profileSong) {
    this.profileSong = profileSong;
  }

  public Url getProfileVideo() {
    return profileVideo;
  }

  public void setProfileVideo(Url profileVideo) {
    this.profileVideo = profileVideo;
  }

  public List<String> getQuotes() {
    return quotes;
  }

  public void setQuotes(List<String> quotes) {
    this.quotes = quotes;
  }

  public String getRelationshipStatus() {
    return relationshipStatus;
  }

  public void setRelationshipStatus(String relationshipStatus) {
    this.relationshipStatus = relationshipStatus;
  }

  public String getReligion() {
    return religion;
  }

  public void setReligion(String religion) {
    this.religion = religion;
  }

  public String getRomance() {
    return romance;
  }

  public void setRomance(String romance) {
    this.romance = romance;
  }

  public String getScaredOf() {
    return scaredOf;
  }

  public void setScaredOf(String scaredOf) {
    this.scaredOf = scaredOf;
  }

  public String getSexualOrientation() {
    return sexualOrientation;
  }

  public void setSexualOrientation(String sexualOrientation) {
    this.sexualOrientation = sexualOrientation;
  }

  public Enum<Smoker> getSmoker() {
    return this.smoker;
  }

  public void setSmoker(Enum<Smoker> newSmoker) {
    this.smoker = newSmoker;
  }

  public List<String> getSports() {
    return sports;
  }

  public void setSports(List<String> sports) {
    this.sports = sports;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public Long getUtcOffset() {
    return utcOffset;
  }

  public void setUtcOffset(Long utcOffset) {
    this.utcOffset = utcOffset;
  }

  public List<String> getTurnOffs() {
    return turnOffs;
  }

  public void setTurnOffs(List<String> turnOffs) {
    this.turnOffs = turnOffs;
  }

  public List<String> getTurnOns() {
    return turnOns;
  }

  public void setTurnOns(List<String> turnOns) {
    this.turnOns = turnOns;
  }

  public List<String> getTvShows() {
    return tvShows;
  }

  public void setTvShows(List<String> tvShows) {
    this.tvShows = tvShows;
  }

  public List<Url> getUrls() {
    return urls;
  }

  public void setUrls(List<Url> urls) {
    this.urls = urls;
  }

  public boolean getIsOwner() {
    return isOwner;
  }

  public void setIsOwner(boolean isOwner) {
    this.isOwner = isOwner;
  }

  public boolean getIsViewer() {
    return isViewer;
  }

  public void setIsViewer(boolean isViewer) {
    this.isViewer = isViewer;
  }

  // Proxied fields

  public String getProfileUrl() {
    Url url = getListFieldWithType(PROFILE_URL_TYPE, getUrls());
    return url == null ? null : url.getValue();
  }

  public void setProfileUrl(String profileUrl) {
    Url url = getListFieldWithType(PROFILE_URL_TYPE, getUrls());
    if (url != null) {
      url.setValue(profileUrl);
    } else {
      if (profileUrl != null)
        setUrls(addListField(new UrlImpl(profileUrl, null, PROFILE_URL_TYPE), getUrls()));
    }
  }

  public String getThumbnailUrl() {
    ListField photo = getListFieldWithType(THUMBNAIL_PHOTO_TYPE, getPhotos());
    return photo == null ? null : photo.getValue();
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    ListField photo = getListFieldWithType(THUMBNAIL_PHOTO_TYPE, getPhotos());
    if (photo != null) {
      photo.setValue(thumbnailUrl);
    } else {
      if (thumbnailUrl != null)
        setPhotos(addListField(new ListFieldImpl(THUMBNAIL_PHOTO_TYPE, thumbnailUrl), getPhotos()));
    }
  }

  private <T extends ListField> T getListFieldWithType(String type, List<T> list) {
    if (list != null) {
      for (T url : list) {
        if (type.equalsIgnoreCase(url.getType())) {
          return url;
        }
      }
    }

    return null;
  }

  private <T extends ListField> List<T> addListField(T field, List<T> list) {
    if (list == null) {
      list = Lists.newArrayList();
    }
    list.add(field);
    return list;
  }
}

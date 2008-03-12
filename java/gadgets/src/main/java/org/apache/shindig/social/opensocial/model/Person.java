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

import org.apache.shindig.social.AbstractGadgetData;
import org.apache.shindig.social.Mandatory;

import java.util.Date;

/**
 * see
 * http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Person.Field.html
 *
 */
public class Person extends AbstractGadgetData {
  // TODO: Create the rest of the person fields and objects
  // TODO: Change all of the array objects to list objects for easier
  // manipulation
  private String aboutMe;
  private String[] activities;
  private Address[] addresses;
  private Integer age;
  private BodyType bodyType;
  private String[] books;
  private String[] cars;
  private String children;
  private Address currentLocation;
  private Date dateOfBirth;
  // drinker : Person's drinking status, specified as an opensocial.Enum with
  // the enum's key referencing opensocial.Enum.Drinker.
  private Email[] emails;
  private String ethnicity;
  private String fashion;
  private String[] food;
  // gender : Person's gender, specified as an opensocial.Enum with the enum's
  // key referencing opensocial.Enum.Gender.
  private String happiestWhen;
  private String[] hereos;
  private String humor;
  @Mandatory private String id;
  private String[] interests;
  private String jobInterests;
  private Organization[] jobs;
  private String[] languagesSpoken;
  private String livingArrangement;
  private String lookingFor;
  private String[] movies;
  private String[] music;
  @Mandatory private Name name;
  private String nickname;
  private String pets;
  private Phone[] phoneNumbers;
  private String politicalViews;
  private Url profileSong;
  private String profileUrl;
  private Url profileVideo;
  private String[] quotes;
  private String relationshipStatus;
  private String religion;
  private String romance;
  private String scaredOf;
  private Organization[] schools;
  private String sexualOrientation;
  // smoker Person's smoking status, specified as an opensocial.Enum with the
  // enum's key referencing opensocial.Enum.Smoker.
  private String[] sports;
  private String status;
  private String[] tags;
  private String thumbnailUrl;
  private Long timeZone;
  private String[] turnOffs;
  private String[] turnOns;
  private String[] tvShows;
  private Url[] urls;

  public Person(String id, Name name) {
    this.id = id;
    this.name = name;
  }

  public String getAboutMe() {
    return aboutMe;
  }

  public void setAboutMe(String aboutMe) {
    this.aboutMe = aboutMe;
  }

  public String[] getActivities() {
    return activities;
  }

  public void setActivities(String[] activities) {
    this.activities = activities;
  }

  public Address[] getAddresses() {
    return addresses;
  }

  public void setAddresses(Address[] addresses) {
    this.addresses = addresses;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public BodyType getBodyType() {
    return bodyType;
  }

  public void setBodyType(BodyType bodyType) {
    this.bodyType = bodyType;
  }

  public String[] getBooks() {
    return books;
  }

  public void setBooks(String[] books) {
    this.books = books;
  }

  public String[] getCars() {
    return cars;
  }

  public void setCars(String[] cars) {
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

  public Date getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public Email[] getEmails() {
    return emails;
  }

  public void setEmails(Email[] emails) {
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

  public String[] getFood() {
    return food;
  }

  public void setFood(String[] food) {
    this.food = food;
  }

  public String getHappiestWhen() {
    return happiestWhen;
  }

  public void setHappiestWhen(String happiestWhen) {
    this.happiestWhen = happiestWhen;
  }

  public String[] getHereos() {
    return hereos;
  }

  public void setHereos(String[] hereos) {
    this.hereos = hereos;
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

  public String[] getInterests() {
    return interests;
  }

  public void setInterests(String[] interests) {
    this.interests = interests;
  }

  public String getJobInterests() {
    return jobInterests;
  }

  public void setJobInterests(String jobInterests) {
    this.jobInterests = jobInterests;
  }

  public Organization[] getJobs() {
    return jobs;
  }

  public void setJobs(Organization[] jobs) {
    this.jobs = jobs;
  }

  public String[] getLanguagesSpoken() {
    return languagesSpoken;
  }

  public void setLanguagesSpoken(String[] languagesSpoken) {
    this.languagesSpoken = languagesSpoken;
  }

  public String getLivingArrangement() {
    return livingArrangement;
  }

  public void setLivingArrangement(String livingArrangement) {
    this.livingArrangement = livingArrangement;
  }

  public String getLookingFor() {
    return lookingFor;
  }

  public void setLookingFor(String lookingFor) {
    this.lookingFor = lookingFor;
  }

  public String[] getMovies() {
    return movies;
  }

  public void setMovies(String[] movies) {
    this.movies = movies;
  }

  public String[] getMusic() {
    return music;
  }

  public void setMusic(String[] music) {
    this.music = music;
  }

  public Name getName() {
    return name;
  }

  public void setName(Name name) {
    this.name = name;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getPets() {
    return pets;
  }

  public void setPets(String pets) {
    this.pets = pets;
  }

  public Phone[] getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(Phone[] phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  public String getPoliticalViews() {
    return politicalViews;
  }

  public void setPoliticalViews(String politicalViews) {
    this.politicalViews = politicalViews;
  }

  public Url getProfileSong() {
    return profileSong;
  }

  public void setProfileSong(Url profileSong) {
    this.profileSong = profileSong;
  }

  public String getProfileUrl() {
    return profileUrl;
  }

  public void setProfileUrl(String profileUrl) {
    this.profileUrl = profileUrl;
  }

  public Url getProfileVideo() {
    return profileVideo;
  }

  public void setProfileVideo(Url profileVideo) {
    this.profileVideo = profileVideo;
  }

  public String[] getQuotes() {
    return quotes;
  }

  public void setQuotes(String[] quotes) {
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

  public Organization[] getSchools() {
    return schools;
  }

  public void setSchools(Organization[] schools) {
    this.schools = schools;
  }

  public String getSexualOrientation() {
    return sexualOrientation;
  }

  public void setSexualOrientation(String sexualOrientation) {
    this.sexualOrientation = sexualOrientation;
  }

  public String[] getSports() {
    return sports;
  }

  public void setSports(String[] sports) {
    this.sports = sports;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public Long getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(Long timeZone) {
    this.timeZone = timeZone;
  }

  public String[] getTurnOffs() {
    return turnOffs;
  }

  public void setTurnOffs(String[] turnOffs) {
    this.turnOffs = turnOffs;
  }

  public String[] getTurnOns() {
    return turnOns;
  }

  public void setTurnOns(String[] turnOns) {
    this.turnOns = turnOns;
  }

  public String[] getTvShows() {
    return tvShows;
  }

  public void setTvShows(String[] tvShows) {
    this.tvShows = tvShows;
  }

  public Url[] getUrls() {
    return urls;
  }

  public void setUrls(Url[] urls) {
    this.urls = urls;
  }
}

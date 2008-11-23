<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Person test case.
 */
class PersonTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Person
   */
  private $Person;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->Person = new Person('ID', 'NAME');
    $this->Person->aboutMe = 'ABOUTME';
    $this->Person->activities = 'ACTIVITIES';
    $this->Person->addresses = 'ADDRESSES';
    $this->Person->age = 'AGE';
    $this->Person->bodyType = 'BODYTYPE';
    $this->Person->books = 'BOOKS';
    $this->Person->cars = 'CARS';
    $this->Person->children = 'CHILDREN';
    $this->Person->currentLocation = 'CURRENTLOCATION';
    $this->Person->dateOfBirth = 'DATEOFBIRTH';
    $this->Person->drinker = 'HEAVILY';
    $this->Person->emails = 'EMAILS';
    $this->Person->ethnicity = 'ETHNICITY';
    $this->Person->fashion = 'FASHION';
    $this->Person->food = 'FOOD';
    $this->Person->gender = 'GENDER';
    $this->Person->happiestWhen = 'HAPPIESTWHEN';
    $this->Person->hasApp = 'HASAPP';
    $this->Person->heroes = 'HEROES';
    $this->Person->humor = 'HUMOR';
    $this->Person->interests = 'INTERESTS';
    $this->Person->jobInterests = 'JOBINTERESTS';
    $this->Person->jobs = 'JOBS';
    $this->Person->languagesSpoken = 'LANGUAGESSPOKEN';
    $this->Person->livingArrangement = 'LIVINGARRANGEMENT';
    //$this->Person->lookingFor = new EnumLookingFor('FRIENDS');
    $this->Person->movies = 'MOVIES';
    $this->Person->music = 'MUSIC';
    $this->Person->networkPresence = 'NETWORKPRESENCE';
    $this->Person->nickname = 'NICKNAME';
    $this->Person->pets = 'PETS';
    $this->Person->phoneNumbers = 'PHONENUMBERS';
    $this->Person->politicalViews = 'POLITICALVIEWS';
    $this->Person->profileSong = 'PROFILESONG';
    $this->Person->profileUrl = 'PROFILEURL';
    $this->Person->profileVideo = 'PROFILEVIDEO';
    $this->Person->quotes = 'QUOTES';
    $this->Person->relationshipStatus = 'RELATIONSHIPSTATUS';
    $this->Person->religion = 'RELIGION';
    $this->Person->romance = 'ROMANCE';
    $this->Person->scaredOf = 'SCAREDOF';
    $this->Person->schools = 'SCHOOLS';
    $this->Person->sexualOrientation = 'SEXUALORIENTATION';
    $this->Person->smoker = 'SMOKER';
    $this->Person->sports = 'SPORTS';
    $this->Person->status = 'STATUS';
    $this->Person->tags = 'TAGS';
    $this->Person->thumbnailUrl = 'THUMBNAILSURL';
    $this->Person->timeZone = 'TIMEZONE';
    $this->Person->turnOffs = 'TURNOFFS';
    $this->Person->turnOns = 'TURNONS';
    $this->Person->tvShows = 'TVSHOWS';
    $this->Person->urls = 'URLS';
    $this->Person->isOwner = 'ISOWNER';
    $this->Person->isViewer = 'ISVIEWER';
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Person = null;
    parent::tearDown();
  }

  /**
   * Tests Person->getAboutMe()
   */
  public function testGetAboutMe() {
    $this->assertEquals('ABOUTME', $this->Person->getAboutMe());
  }

  /**
   * Tests Person->getActivities()
   */
  public function testGetActivities() {
    $this->assertEquals('ACTIVITIES', $this->Person->getActivities());
  }

  /**
   * Tests Person->getAddresses()
   */
  public function testGetAddresses() {
    $this->assertEquals('ADDRESSES', $this->Person->getAddresses());
  }

  /**
   * Tests Person->getAge()
   */
  public function testGetAge() {
    $this->assertEquals('AGE', $this->Person->getAge());
  }

  /**
   * Tests Person->getBodyType()
   */
  public function testGetBodyType() {
    $this->assertEquals('BODYTYPE', $this->Person->getBodyType());
  }

  /**
   * Tests Person->getBooks()
   */
  public function testGetBooks() {
    $this->assertEquals('BOOKS', $this->Person->getBooks());
  }

  /**
   * Tests Person->getCars()
   */
  public function testGetCars() {
    $this->assertEquals('CARS', $this->Person->getCars());
  
  }

  /**
   * Tests Person->getChildren()
   */
  public function testGetChildren() {
    $this->assertEquals('CHILDREN', $this->Person->getChildren());
  }

  /**
   * Tests Person->getCurrentLocation()
   */
  public function testGetCurrentLocation() {
    $this->assertEquals('CURRENTLOCATION', $this->Person->getCurrentLocation());
  }

  /**
   * Tests Person->getDateOfBirth()
   */
  public function testGetDateOfBirth() {
    $this->Person->setBirthday('10/10/2010');
    $this->assertEquals('2010-10-10', $this->Person->getBirthday());
  }

  /**
   * Tests Person->getDrinker()
   */
  public function testGetDrinker() {
    //$drinker = new EnumDrinker('HEAVILY');
    $this->Person->setDrinker('HEAVILY');
    $this->assertEquals('HEAVILY', $this->Person->getDrinker());
  }

  /**
   * Tests Person->getEmails()
   */
  public function testGetEmails() {
    $this->assertEquals('EMAILS', $this->Person->getEmails());
  
  }

  /**
   * Tests Person->getEthnicity()
   */
  public function testGetEthnicity() {
    $this->assertEquals('ETHNICITY', $this->Person->getEthnicity());
  }

  /**
   * Tests Person->getFashion()
   */
  public function testGetFashion() {
    $this->assertEquals('FASHION', $this->Person->getFashion());
  
  }

  /**
   * Tests Person->getFood()
   */
  public function testGetFood() {
    $this->assertEquals('FOOD', $this->Person->getFood());
  }

  /**
   * Tests Person->getGender()
   */
  public function testGetGender() {
    $this->Person->setGender('FEMALE');
    $this->assertEquals('FEMALE', $this->Person->getGender());
  }

  /**
   * Tests Person->getHappiestWhen()
   */
  public function testGetHappiestWhen() {
    $this->assertEquals('HAPPIESTWHEN', $this->Person->getHappiestWhen());
  }

  /**
   * Tests Person->getHasApp()
   */
  public function testGetHasApp() {
    $this->assertEquals('HASAPP', $this->Person->getHasApp());
  }

  /**
   * Tests Person->getHeroes()
   */
  public function testGetHeroes() {
    $this->assertEquals('HEROES', $this->Person->getHeroes());
  }

  /**
   * Tests Person->getHumor()
   */
  public function testGetHumor() {
    $this->assertEquals('HUMOR', $this->Person->getHumor());
  }

  /**
   * Tests Person->getId()
   */
  public function testGetId() {
    $this->assertEquals('ID', $this->Person->getId());
  }

  /**
   * Tests Person->getInterests()
   */
  public function testGetInterests() {
    $this->assertEquals('INTERESTS', $this->Person->getInterests());
  }

  /**
   * Tests Person->getIsOwner()
   */
  public function testGetIsOwner() {
    $this->assertEquals('ISOWNER', $this->Person->getIsOwner());
  
  }

  /**
   * Tests Person->getIsViewer()
   */
  public function testGetIsViewer() {
    $this->assertEquals('ISVIEWER', $this->Person->getIsViewer());
  }

  /**
   * Tests Person->getJobInterests()
   */
  public function testGetJobInterests() {
    $this->assertEquals('JOBINTERESTS', $this->Person->getJobInterests());
  }

  /**
   * Tests Person->getLanguagesSpoken()
   */
  public function testGetLanguagesSpoken() {
    $this->assertEquals('LANGUAGESSPOKEN', $this->Person->getLanguagesSpoken());
  }

  /**
   * Tests Person->getLivingArrangement()
   */
  public function testGetLivingArrangement() {
    $this->assertEquals('LIVINGARRANGEMENT', $this->Person->getLivingArrangement());
  }

  /**
   * Tests Person->getMovies()
   */
  public function testGetMovies() {
    $this->assertEquals('MOVIES', $this->Person->getMovies());
  }

  /**
   * Tests Person->getMusic()
   */
  public function testGetMusic() {
    $this->assertEquals('MUSIC', $this->Person->getMusic());
  }

  /**
   * Tests Person->getName()
   */
  public function testGetName() {
    $this->assertEquals('NAME', $this->Person->getName());
  }

  /**
   * Tests Person->getNetworkPresence()
   */
  public function testGetNetworkPresence() {
    $presence = new EnumPresence('DND');
    $this->Person->setNetworkPresence('DND');
    $this->assertEquals($presence, $this->Person->getNetworkPresence());
  }

  /**
   * Tests Person->getNickname()
   */
  public function testGetNickname() {
    $this->assertEquals('NICKNAME', $this->Person->getNickname());
  
  }

  /**
   * Tests Person->getPets()
   */
  public function testGetPets() {
    $this->assertEquals('PETS', $this->Person->getPets());
  }

  /**
   * Tests Person->getPhoneNumbers()
   */
  public function testGetPhoneNumbers() {
    $this->assertEquals('PHONENUMBERS', $this->Person->getPhoneNumbers());
  }

  /**
   * Tests Person->getPoliticalViews()
   */
  public function testGetPoliticalViews() {
    $this->assertEquals('POLITICALVIEWS', $this->Person->getPoliticalViews());
  }

  /**
   * Tests Person->getProfileSong()
   */
  public function testGetProfileSong() {
    $this->assertEquals('PROFILESONG', $this->Person->getProfileSong());
  }

  /**
   * Tests Person->getProfileUrl()
   */
  public function testGetProfileUrl() {
    $this->assertEquals('PROFILEURL', $this->Person->getProfileUrl());
  }

  /**
   * Tests Person->getProfileVideo()
   */
  public function testGetProfileVideo() {
    $this->assertEquals('PROFILEVIDEO', $this->Person->getProfileVideo());
  }

  /**
   * Tests Person->getQuotes()
   */
  public function testGetQuotes() {
    $this->assertEquals('QUOTES', $this->Person->getQuotes());
  }

  /**
   * Tests Person->getRelationshipStatus()
   */
  public function testGetRelationshipStatus() {
    $this->assertEquals('RELATIONSHIPSTATUS', $this->Person->getRelationshipStatus());
  }

  /**
   * Tests Person->getReligion()
   */
  public function testGetReligion() {
    $this->assertEquals('RELIGION', $this->Person->getReligion());
  }

  /**
   * Tests Person->getRomance()
   */
  public function testGetRomance() {
    $this->assertEquals('ROMANCE', $this->Person->getRomance());
  }

  /**
   * Tests Person->getScaredOf()
   */
  public function testGetScaredOf() {
    $this->assertEquals('SCAREDOF', $this->Person->getScaredOf());
  }

  /**
   * Tests Person->getSexualOrientation()
   */
  public function testGetSexualOrientation() {
    $this->assertEquals('SEXUALORIENTATION', $this->Person->getSexualOrientation());
  }

  /**
   * Tests Person->getSmoker()
   */
  public function testGetSmoker() {
    $smoker = new EnumSmoker('OCCASIONALLY');
    $this->Person->setSmoker('OCCASIONALLY');
    $this->assertEquals($smoker, $this->Person->getSmoker());
  }

  /**
   * Tests Person->getSports()
   */
  public function testGetSports() {
    $this->assertEquals('SPORTS', $this->Person->getSports());
  }

  /**
   * Tests Person->getStatus()
   */
  public function testGetStatus() {
    $this->assertEquals('STATUS', $this->Person->getStatus());
  }

  /**
   * Tests Person->getTags()
   */
  public function testGetTags() {
    $this->assertEquals('TAGS', $this->Person->getTags());
  }

  /**
   * Tests Person->getThumbnailUrl()
   */
  public function testGetThumbnailUrl() {
    $this->assertEquals('THUMBNAILSURL', $this->Person->getThumbnailUrl());
  }

  /**
   * Tests Person->getTurnOffs()
   */
  public function testGetTurnOffs() {
    $this->assertEquals('TURNOFFS', $this->Person->getTurnOffs());
  }

  /**
   * Tests Person->getTurnOns()
   */
  public function testGetTurnOns() {
    $this->assertEquals('TURNONS', $this->Person->getTurnOns());
  }

  /**
   * Tests Person->getTvShows()
   */
  public function testGetTvShows() {
    $this->assertEquals('TVSHOWS', $this->Person->getTvShows());
  }

  /**
   * Tests Person->getUrls()
   */
  public function testGetUrls() {
    $this->assertEquals('URLS', $this->Person->getUrls());
  }

  /**
   * Tests Person->setAboutMe()
   */
  public function testSetAboutMe() {
    $this->Person->setAboutMe('aboutme');
    $this->assertEquals('aboutme', $this->Person->aboutMe);
  }

  /**
   * Tests Person->setActivities()
   */
  public function testSetActivities() {
    $this->Person->setActivities('activities');
    $this->assertEquals('activities', $this->Person->activities);
  }

  /**
   * Tests Person->setAddresses()
   */
  public function testSetAddresses() {
    $this->Person->setAddresses('addresses');
    $this->assertEquals('addresses', $this->Person->addresses);
  }

  /**
   * Tests Person->setAge()
   */
  public function testSetAge() {
    $this->Person->setAge('age');
    $this->assertEquals('age', $this->Person->age);
  }

  /**
   * Tests Person->setBodyType()
   */
  public function testSetBodyType() {
    $this->Person->setBodyType('bodytype');
    $this->assertEquals('bodytype', $this->Person->bodyType);
  }

  /**
   * Tests Person->setBooks()
   */
  public function testSetBooks() {
    $this->Person->setBooks('books');
    $this->assertEquals('books', $this->Person->books);
  }

  /**
   * Tests Person->setCars()
   */
  public function testSetCars() {
    $this->Person->setCars('cars');
    $this->assertEquals('cars', $this->Person->cars);
  }

  /**
   * Tests Person->setChildren()
   */
  public function testSetChildren() {
    $this->Person->setChildren('children');
    $this->assertEquals('children', $this->Person->children);
  }

  /**
   * Tests Person->setCurrentLocation()
   */
  public function testSetCurrentLocation() {
    $this->Person->setCurrentLocation('currentlocation');
    $this->assertEquals('currentlocation', $this->Person->currentLocation);
  }

  /**
   * Tests Person->setDateOfBirth()
   */
  public function testSetDateOfBirth() {
    $this->Person->setBirthday('10/10/2010');
    $this->assertEquals('2010-10-10', $this->Person->getBirthday());
  }

  /**
   * Tests Person->setEmails()
   */
  public function testSetEmails() {
    $this->Person->setEmails('emails');
    $this->assertEquals('emails', $this->Person->emails);
  }

  /**
   * Tests Person->setEthnicity()
   */
  public function testSetEthnicity() {
    $this->Person->setEthnicity('ethnicity');
    $this->assertEquals('ethnicity', $this->Person->ethnicity);
  }

  /**
   * Tests Person->setFashion()
   */
  public function testSetFashion() {
    $this->Person->setFashion('fashion');
    $this->assertEquals('fashion', $this->Person->fashion);
  }

  /**
   * Tests Person->setFood()
   */
  public function testSetFood() {
    $this->Person->setFood('food');
    $this->assertEquals('food', $this->Person->food);
  }

  /**
   * Tests Person->setGender()
   */
  public function testSetGender() {
    $this->Person->setGender('MALE');
    $this->assertEquals('MALE', $this->Person->gender);
  }

  /**
   * Tests Person->setHappiestWhen()
   */
  public function testSetHappiestWhen() {
    $this->Person->setHappiestWhen('happiestwhen');
    $this->assertEquals('happiestwhen', $this->Person->happiestWhen);
  }

  /**
   * Tests Person->setHasApp()
   */
  public function testSetHasApp() {
    $this->Person->setHasApp('hasapp');
    $this->assertEquals('hasapp', $this->Person->hasApp);
  }

  /**
   * Tests Person->setHeroes()
   */
  public function testSetHeroes() {
    $this->Person->setHeroes('heroes');
    $this->assertEquals('heroes', $this->Person->heroes);
  }

  /**
   * Tests Person->setHumor()
   */
  public function testSetHumor() {
    $this->Person->setHumor('humor');
    $this->assertEquals('humor', $this->Person->humor);
  }

  /**
   * Tests Person->setId()
   */
  public function testSetId() {
    $this->Person->setId('id');
    $this->assertEquals('id', $this->Person->id);
  }

  /**
   * Tests Person->setInterests()
   */
  public function testSetInterests() {
    $this->Person->setInterests('interests');
    $this->assertEquals('interests', $this->Person->interests);
  }

  /**
   * Tests Person->setIsOwner()
   */
  public function testSetIsOwner() {
    $this->Person->setIsOwner('isowner');
    $this->assertEquals('isowner', $this->Person->isOwner);
  }

  /**
   * Tests Person->setIsViewer()
   */
  public function testSetIsViewer() {
    $this->Person->setIsViewer('isviewer');
    $this->assertEquals('isviewer', $this->Person->isViewer);
  }

  /**
   * Tests Person->setJobInterests()
   */
  public function testSetJobInterests() {
    $this->Person->setJobInterests('jobinterests');
    $this->assertEquals('jobinterests', $this->Person->jobInterests);
  }

  /**
   * Tests Person->setLanguagesSpoken()
   */
  public function testSetLanguagesSpoken() {
    $this->Person->setLanguagesSpoken('languagesspoken');
    $this->assertEquals('languagesspoken', $this->Person->languagesSpoken);
  }

  /**
   * Tests Person->setLivingArrangement()
   */
  public function testSetLivingArrangement() {
    $this->Person->setLivingArrangement('livingarrangement');
    $this->assertEquals('livingarrangement', $this->Person->livingArrangement);
  }

  /**
   * Tests Person->setLookingFor()
   */
  public function testSetLookingFor() {
    $lookingFor = new EnumLookingFor('FRIENDS');
    $this->Person->setLookingFor('FRIENDS');
    $this->assertEquals($lookingFor, $this->Person->getLookingFor());
  }

  /**
   * Tests Person->setMovies()
   */
  public function testSetMovies() {
    $this->Person->setMovies('movies');
    $this->assertEquals('movies', $this->Person->movies);
  }

  /**
   * Tests Person->setMusic()
   */
  public function testSetMusic() {
    $this->Person->setMusic('music');
    $this->assertEquals('music', $this->Person->music);
  }

  /**
   * Tests Person->setName()
   */
  public function testSetName() {
    $this->Person->setName('name');
    $this->assertEquals('name', $this->Person->name);
  }

  /**
   * Tests Person->setNickname()
   */
  public function testSetNickname() {
    $this->Person->setNickname('nickname');
    $this->assertEquals('nickname', $this->Person->nickname);
  }

  /**
   * Tests Person->setPets()
   */
  public function testSetPets() {
    $this->Person->setPets('pets');
    $this->assertEquals('pets', $this->Person->pets);
  }

  /**
   * Tests Person->setPhoneNumbers()
   */
  public function testSetPhoneNumbers() {
    $this->Person->setPhoneNumbers('phonenumbers');
    $this->assertEquals('phonenumbers', $this->Person->phoneNumbers);
  }

  /**
   * Tests Person->setPoliticalViews()
   */
  public function testSetPoliticalViews() {
    $this->Person->setPoliticalViews('politicalviews');
    $this->assertEquals('politicalviews', $this->Person->politicalViews);
  }

  /**
   * Tests Person->setProfileSong()
   */
  public function testSetProfileSong() {
    $this->Person->setProfileSong('profilesong');
    $this->assertEquals('profilesong', $this->Person->profileSong);
  }

  /**
   * Tests Person->setProfileUrl()
   */
  public function testSetProfileUrl() {
    $this->Person->setProfileUrl('profileurl');
    $this->assertEquals('profileurl', $this->Person->profileUrl);
  }

  /**
   * Tests Person->setProfileVideo()
   */
  public function testSetProfileVideo() {
    $this->Person->setProfileVideo('profilevideo');
    $this->assertEquals('profilevideo', $this->Person->profileVideo);
  }

  /**
   * Tests Person->setQuotes()
   */
  public function testSetQuotes() {
    $this->Person->setQuotes('quotes');
    $this->assertEquals('quotes', $this->Person->quotes);
  }

  /**
   * Tests Person->setRelationshipStatus()
   */
  public function testSetRelationshipStatus() {
    $this->Person->setRelationshipStatus('relationshipstatus');
    $this->assertEquals('relationshipstatus', $this->Person->relationshipStatus);
  }

  /**
   * Tests Person->setReligion()
   */
  public function testSetReligion() {
    $this->Person->setReligion('religion');
    $this->assertEquals('religion', $this->Person->religion);
  }

  /**
   * Tests Person->setRomance()
   */
  public function testSetRomance() {
    $this->Person->setRomance('romance');
    $this->assertEquals('romance', $this->Person->romance);
  }

  /**
   * Tests Person->setScaredOf()
   */
  public function testSetScaredOf() {
    $this->Person->setScaredOf('scaredof');
    $this->assertEquals('scaredof', $this->Person->scaredOf);
  }

  /**
   * Tests Person->setSexualOrientation()
   */
  public function testSetSexualOrientation() {
    $this->Person->setSexualOrientation('sexualorientation');
    $this->assertEquals('sexualorientation', $this->Person->sexualOrientation);
  }

  /**
   * Tests Person->setSports()
   */
  public function testSetSports() {
    $this->Person->setSports('sports');
    $this->assertEquals('sports', $this->Person->sports);
  }

  /**
   * Tests Person->setStatus()
   */
  public function testSetStatus() {
    $this->Person->setStatus('status');
    $this->assertEquals('status', $this->Person->status);
  }

  /**
   * Tests Person->setTags()
   */
  public function testSetTags() {
    $this->Person->setTags('tags');
    $this->assertEquals('tags', $this->Person->tags);
  }

  /**
   * Tests Person->setThumbnailUrl()
   */
  public function testSetThumbnailUrl() {
    $this->Person->setThumbnailUrl('thumbnailurl');
    $this->assertEquals('thumbnailurl', $this->Person->thumbnailUrl);
  }

  /**
   * Tests Person->setTurnOffs()
   */
  public function testSetTurnOffs() {
    $this->Person->setTurnOffs('turnoffs');
    $this->assertEquals('turnoffs', $this->Person->turnOffs);
  }

  /**
   * Tests Person->setTurnOns()
   */
  public function testSetTurnOns() {
    $this->Person->setTurnOns('turnons');
    $this->assertEquals('turnons', $this->Person->turnOns);
  }

  /**
   * Tests Person->setTvShows()
   */
  public function testSetTvShows() {
    $this->Person->setTvShows('tvshows');
    $this->assertEquals('tvshows', $this->Person->tvShows);
  }

  /**
   * Tests Person->setUrls()
   */
  public function testSetUrls() {
    $this->Person->setUrls('urls');
    $this->assertEquals('urls', $this->Person->urls);
  }
}

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

var gadgets = gadgets || {};

function JsonAlbumTest(name) {
  TestCase.call(this, name);
};
JsonAlbumTest.inherits(TestCase);

JsonAlbumTest.prototype.setUp = function() {
  // Prepare for mocks
  this.oldGetField = opensocial.Container.getField;
  opensocial.Container.getField = function(fields, key, opt_params) {
    return fields[key];
  };
};

JsonAlbumTest.prototype.tearDown = function() {
  // Remove mocks
  opensocial.Container.getField = this.oldGetField;
};

JsonAlbumTest.prototype.testJsonAlbumConstructor = function() {
	var album = new JsonAlbum( {
		'id' : 1,
		'ownerId' : 2,
		'title' : 'test-title',
		'description' : 'test-description',
		'location' : {'locality' : 'test-locality', 'country' : 'test-country'},
		'mediaItemCount' : 3,
		'mediaMimeType' : [ 'jpg' ],
		'mediaType' : 'image',
		'thumbnailUrl' : 'test-thumbnailUrl'
	});
	
	var fields = opensocial.Album.Field;
	this.assertEquals(1, album.getField(fields.ID));
	this.assertEquals(2, album.getField(fields.OWNER_ID));
	this.assertEquals(3, album.getField(fields.MEDIA_ITEM_COUNT));
	this.assertEquals('test-title', album.getField(fields.TITLE));
	this.assertEquals('test-description', album.getField(fields.DESCRIPTION));
	
	var location = album.getField(fields.LOCATION);
	this.assertTrue(location instanceof opensocial.Address);
	this.assertEquals('test-locality', location.getField(opensocial.Address.Field.LOCALITY));
	this.assertEquals('test-country', location.getField(opensocial.Address.Field.COUNTRY));
	
	var mimeTypes = album.getField(fields.MEDIA_MIME_TYPE);
	this.assertTrue(mimeTypes instanceof Array);
	this.assertEquals('jpg', mimeTypes[0]);
	
	this.assertEquals(opensocial.MediaItem.Type.IMAGE, album.getField(fields.MEDIA_TYPE));
	this.assertEquals('test-thumbnailUrl', album.getField(fields.THUMBNAIL_URL));
};
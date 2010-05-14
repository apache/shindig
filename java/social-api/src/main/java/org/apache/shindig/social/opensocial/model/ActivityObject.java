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

import java.util.List;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.ActivityObjectImpl;

import com.google.inject.ImplementedBy;

@ImplementedBy(ActivityObjectImpl.class)
@Exportablebean
public interface ActivityObject {
	
	/*
	 * Fields that represent JSON elements for an activity entry.
	 */
	public static enum Field {
		ID("id"),
		DISPLAY_NAME("displayName"),
		SUMMARY("summary"),
		PERMALINK("permalink"),
		IMAGE("image"),
		CONTENT("content"),
		AUDIO_STREAM("audioStream"),
		VIDEO_STREAM("videoStream"),
		PLAYER_APPLET("playerApplet"),
		BOOKMARK_TARGET("bookmarkTarget"),
		THUMBNAIL("thumbnail"),
		SUBJECT("subject"),
		DESCRIPTION("description"),
		RATING("rating"),
		ICON("icon"),
		IN_REPLY_TO("inReplyTo"),
		OBJECT_TYPE("objectType");
		
		/*
		 * The name of the JSON element.
		 */
		private final String jsonString;
		
		/*
		 * Constructs the field base for the JSON element.
		 * 
		 * @param jsonString the name of the element
		 */
		private Field(String jsonString) {
			this.jsonString = jsonString;
		}
		
		/*
		 * Returns the name of the JSON element.
		 * 
		 * @return String the name of the JSON element
		 */
		public String toString() {
			return jsonString;
		}
	}
	
	String getId();

	void setId(String id);

	String getDisplayName();

	void setDisplayName(String displayName);

	String getSummary();

	void setSummary(String summary);

	String getPermalink();

	void setPermalink(String permalink);

	String getImage();

	void setImage(String image);

	String getContent();

	void setContent(String content);

	String getAudioStream();

	void setAudioStream(String audioStream);

	String getVideoStream();

	void setVideoStream(String videoStream);

	String getPlayerApplet();

	void setPlayerApplet(String playerApplet);

	String getBookmarkTarget();

	void setBookmarkTarget(String bookmarkTarget);
	
	String getThumbnail();

	void setThumbnail(String thumbnail);

	String getSubject();

	void setSubject(String subject);

	String getDescription();

	void setDescription(String description);

	String getRating();

	void setRating(String rating);

	String getIcon();

	void setIcon(String icon);

	List<String> getObjectType();

	void setObjectType(List<String> objectType);

	ActivityObject getInReplyTo();

	void setInReplyTo(ActivityObject inReplyTo);
}
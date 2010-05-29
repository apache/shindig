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

package org.apache.shindig.extras.as.opensocial.model;

import java.util.List;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.extras.as.core.model.ActivityEntryImpl;

import com.google.inject.ImplementedBy;

@ImplementedBy(ActivityEntryImpl.class)
@Exportablebean
public interface ActivityEntry {
	
	/*
	 * Fields that represent JSON elements for an activity entry.
	 */
	public static enum Field {
		ID("id"),
		STREAM_FAVICON_("streamFavicon"),
		POSTED_TIME("postedTime"),
		ACTOR("actor"),
		VERB("verb"),
		OBJECT("object"),
		TARGET("target"),
		PERMALINK_("permalink"),
		TITLE("title"),
		BODY("body");
		
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
	
	/*
	 * Possible verbs for an activity stream entry.
	 */
	public static enum Verb {
		MARK_AS_FAVORITE("markAsFavorite"),
		START_FOLLOWING("startFollowing"),
		MARK_AS_LIKED("markAsLiked"),
		MAKE_FRIEND("makeFriend"),
		JOIN("join"),
		PLAY("play"),
		POST("post"),
		SAVE("save"),
		SHARE("share"),
		TAG("tag"),
		UPDATE("update");
		
		/*
		 * The name of the JSON element.
		 */
		private final String jsonString;
		
		/*
		 * Constructs the field base for the JSON element.
		 * 
		 * @param jsonString the name of the element
		 */
		private Verb(String jsonString) {
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
	
	void setId(String id);
	
	String getId();
	
	void setStreamFavicon(String streamFavicon);
	
	String getStreamFavicon();
	
	void setPostedTime(String postedTime);
	
	String getPostedTime();
	
	void setActor(ActivityObject actor);
	
	ActivityObject getActor();
	
	void setVerb(List<String> verb);
	
	List<String> getVerb();
	
	void setObject(ActivityObject object);
	
	ActivityObject getObject();
	
	void setTarget(ActivityObject target);
	
	ActivityObject getTarget();
	
	void setPermalink(String permalink);
	
	String getPermalink();
	
	void setTitle(String title);
	
	String getTitle();
	
	void setBody(String body);
	
	String getBody();
}
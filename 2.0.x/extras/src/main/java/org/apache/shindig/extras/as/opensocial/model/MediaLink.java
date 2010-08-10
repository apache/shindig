package org.apache.shindig.extras.as.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.extras.as.core.model.MediaLinkImpl;
import com.google.inject.ImplementedBy;

/*
 * TODO: comment this class.
 */
@ImplementedBy(MediaLinkImpl.class)
@Exportablebean
public interface MediaLink {

	/*
	 * Fields that represent the JSON elements.
	 */
	public static enum Field {
		TARGET("target"),
		TYPE("type"),
		WIDTH("width"),
		HEIGHT("height"),
		DURATION("duration");
		
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
	
	String getTarget();

	void setTarget(String target);

	String getType();

	void setType(String type);

	String getWidth();

	void setWidth(String width);

	String getHeight();

	void setHeight(String height);

	String getDuration();

	void setDuration(String duration);
}

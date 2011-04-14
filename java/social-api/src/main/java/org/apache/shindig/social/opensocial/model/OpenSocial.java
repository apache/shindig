package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.OpenSocialImpl;

import com.google.inject.ImplementedBy;

/**
 * Represents the OpenSocial namespace in ActivityStreams.
 *
 */
@ImplementedBy(OpenSocialImpl.class)
@Exportablebean
public interface OpenSocial {
	
	public static enum Field {
		//Add OpenSocial field extensions here
		;
		/**
		 * The name of the JSON element.
		 */
		private final String jsonString;

		/**
		 * Constructs the field base for the JSON element.
		 * 
		 * @param jsonString the name of the element
		 */
		private Field(String jsonString) {
			this.jsonString = jsonString;
		}

		/**
		 * Returns the name of the JSON element.
		 * 
		 * @return String the name of the JSON element
		 */
		public String toString() {
			return jsonString;
		}
	}
	

}

package org.apache.shindig.extras.as.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.extras.as.core.model.ActionLinkImpl;

import com.google.inject.ImplementedBy;

/*
 * TODO: comment this class.
 */
@ImplementedBy(ActionLinkImpl.class)
@Exportablebean
public interface ActionLink {
	
	/*
	 * Fields that represent JSON elements for an activity entry.
	 */
	public static enum Field {
		TARGET("target"),
		CAPTION("caption");
		
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

	String getCaption();

	void setCaption(String caption);
}

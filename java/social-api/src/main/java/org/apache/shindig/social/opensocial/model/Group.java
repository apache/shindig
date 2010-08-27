package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.GroupImpl;
import org.apache.shindig.social.opensocial.spi.GroupId;

import com.google.inject.ImplementedBy;

/**
 * <p>
 * OpenSocial Groups are owned by people, and are used to tag or categorize people and their relationships.
 * Each group has a display name, an identifier which is unique within the groups owned by that person, and a URI link.
 * A group may be a private, invitation-only, public or a personal group used to organize friends.
 * </p>
 * <p>
 * From http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Data.xml#Group
 * </p>
 */
@ImplementedBy(GroupImpl.class)
@Exportablebean
public interface Group {

	 public static enum Field {
		    /** Unique ID for this group Required. */
		    ID("Id"),
		    /** Title of group Required. */
		    TITLE("title"),
		    /** Description of group Optional. */
		    DESCRIPTION("description");

		    /**
		     * The json field that the instance represents.
		     */
		    private final String jsonString;

		    /**
		     * create a field base on the a json element.
		     *
		     * @param jsonString the name of the element
		     */
		    private Field(String jsonString) {
		      this.jsonString = jsonString;
		    }

		    /**
		     * emit the field as a json element.
		     *
		     * @return the field name
		     */
		    @Override
		    public String toString() {
		      return jsonString;
		    }
	 }

	 GroupId getId();
	 void setId(GroupId id);

	 String getTitle();
	 void setTitle(String title);

	 String getDescription();
	 void setDescription(String description);

}

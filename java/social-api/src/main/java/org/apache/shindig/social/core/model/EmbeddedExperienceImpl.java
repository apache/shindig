package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.EmbeddedExperience;

/**
 * Embedded experience implementation.
 *
 */
public class EmbeddedExperienceImpl implements EmbeddedExperience {
	
	private Object context;
	private String gadget;
	private String previewImage;
	private String url;
	
	/**
	 * Constructor
	 */
	public EmbeddedExperienceImpl(){}

	/** {@inheritDoc} */
	public Object getContext() {
		return context;
	}

	/** {@inheritDoc} */
	public String getGadget() {
		return gadget;
	}

	/** {@inheritDoc} */
	public String getPreviewImage() {
		return previewImage;
	}
	
	/** {@inheritDoc} */
	public String getUrl(){
		return url;
	}

	/** {@inheritDoc} */
	public void setContext(Object context) {
		this.context = context;
	}

	/** {@inheritDoc} */
	public void setGadget(String gadget) {
		this.gadget = gadget;
	}

	/** {@inheritDoc} */
	public void setPreviewImage(String previewImage) {
		this.previewImage = previewImage;
	}
	
	/** {@inheritDoc} */
	public void setUrl(String url){
		this.url = url;
	}

}

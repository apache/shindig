<?php

class JsonRpcGadgetContext extends GadgetContext {

	public function __construct($jsonContext, $url)
	{
		parent::__construct('GADGET');
		$this->url = $url;
		$this->view = $jsonContext->view;
		$this->locale = new Locale($jsonContext->language, $jsonContext->country);
		$this->container = $jsonContext->container;
	}

	public function getView()
	{
		return $this->view;
	}
}
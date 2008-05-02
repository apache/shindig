<?php
// Response item error codes
define('NOT_IMPLEMENTED', "notImplemented");
define('UNAUTHORIZED', "unauthorized");
define('FORBIDDEN', "forbidden");
define('BAD_REQUEST', "badRequest");
define('INTERNAL_ERROR', "internalError");

class GadgetDataServlet extends HttpServlet {
	private $handlers = array();
	
	public function __construct()
	{
		parent::__construct();
		$handlers = Config::get('handlers');
		if (empty($handlers)) {
			$this->handlers[] = new OpenSocialDataHandler();
			$this->handlers[] = new StateFileDataHandler();
		} else {
			$handlers = explode(',', $handlers);
			foreach ( $handlers as $handler ) {
				$this->handlers[] = new $handler();
			}
		}
	}
	
	public function doPost()
	{
		$requestParam = isset($_POST['request']) ? $_POST['request'] : '';
		$token = isset($_POST['st']) ? $_POST['st'] : '';
		// detect if magic quotes are on, and if so strip them from the request
		if (get_magic_quotes_gpc()) {
			$requestParam = stripslashes($requestParam);
		}
		$request = json_decode($requestParam, true);
		if ($request == $requestParam) {
			// oddly enough if the json_decode function can't parse the code,
			// it just returns the original string (instead of something usefull like 'null' or false :))
			throw new Exception("Invalid request JSON");
		}
		
		try {
			$response = new DataResponse($this->createResponse($requestParam, $token));
		} catch ( Exception $e ) {
			$response = new DataResponse(false, BAD_REQUEST);
		}
		echo json_encode($response);
	}
	
	private function createResponse($requestParam, $token)
	{
		global $config;
		if (empty($token)) {
			throw new Exception("INVALID_GADGET_TOKEN");
		}
		$gadgetSigner = new $config['gadget_signer']();
		//FIXME currently don't have a propper token, impliment and re-enable this asap
		$securityToken = $gadgetSigner->createToken($token);
		$responseItems = array();
		$requests = json_decode($requestParam, true);
		if ($requests == $requestParam) {
			// oddly enough if the json_decode function can't parse the code,
			// it just returns the original string
			throw new Exception("Invalid request JSON");
		}
		foreach ( $requests as $request ) {
			$requestItem = new RequestItem($request['type'], $request, $securityToken);
			$response = new ResponseItem(NOT_IMPLEMENTED, $request['type'] . " has not been implemented yet.", array());
			foreach ( $this->handlers as $handler ) {
				if ($handler->shouldHandle($request['type'])) {
					$response = $handler->handleRequest($requestItem);
				}
			}
			$responseItems[] = $response;
		}
		return $responseItems;
	}
	
	public function doGet()
	{
		echo header("HTTP/1.0 400 Bad Request", true, 400);
		die("<h1>Bad Request</h1>");
	}
}
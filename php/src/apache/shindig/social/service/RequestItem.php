<?php
namespace apache\shindig\social\service;
use apache\shindig\common\IllegalArgumentException;
use apache\shindig\social\spi\UserId;
use apache\shindig\social\spi\GroupId;
use apache\shindig\social\spi\CollectionOptions;
use apache\shindig\common\SecurityToken;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Abstract base type for social API requests.
 */
abstract class RequestItem {

  // Common OpenSocial API fields
  public static $APP_ID = "appId";

  public static $USER_ID = "userId";

  public static $GROUP_ID = "groupId";

  public static $START_INDEX = "startIndex";

  public static $COUNT = "count";

  public static $SORT_BY = "sortBy";
  public static $SORT_ORDER = "sortOrder";

  public static $FILTER_BY = "filterBy";
  public static $FILTER_OPERATION = "filterOp";
  public static $FILTER_VALUE = "filterValue";

  public static $FIELDS = "fields";

  // Opensocial defaults
  public static $DEFAULT_START_INDEX = 0;

  public static $DEFAULT_COUNT = 20;

  public static $APP_SUBSTITUTION_TOKEN = "@app";

  /**
   * @var SecurityToken
   */
  protected $token;

  protected $operation;

  protected $service;

  /**
   *
   * @param object $service
   * @param string $operation
   * @param SecurityToken $token
   */
  public function __construct($service, $operation, SecurityToken $token) {
    $this->service = $service;
    $this->operation = $operation;
    $this->token = $token;
  }

  /**
   *
   * @return int
   */
  public function getAppId() {
    $appId = $this->getParameter(self::$APP_ID);
    if ($appId != null && $appId == self::$APP_SUBSTITUTION_TOKEN) {
      return $this->token->getAppId();
    } else {
      return $appId;
    }
  }

  /**
   *
   * @return array
   */
  public function getUsers() {
    $ids = $this->getListParameter(self::$USER_ID);
    if (empty($ids)) {
      if ($this->token->getViewerId() != null) {
        // Assume @me
        $ids = array("@me");
      } else {
        throw new IllegalArgumentException("No userId provided and viewer not available");
      }
    }
    $userIds = array();
    foreach ($ids as $id) {
      $userIds[] = UserId::fromJson($id);
    }
    return $userIds;
  }

  /**
   *
   * @return string
   */
  public function getGroup() {
    return GroupId::fromJson($this->getParameter(self::$GROUP_ID, "@self"));
  }

  /**
   *
   * @return int
   */
  public function getStartIndex() {
    $startIndex = $this->getParameter(self::$START_INDEX);
    if ($startIndex == null) {
      return self::$DEFAULT_START_INDEX;
    } elseif (is_numeric($startIndex)) {
      return intval($startIndex);
    } else {
      throw new SocialSpiException("Parameter " . self::$START_INDEX . " (" . $startIndex . ") is not a number.", ResponseError::$BAD_REQUEST);
    }
  }

  /**
   *
   * @return int
   */
  public function getCount() {
    $count = $this->getParameter(self::$COUNT);
    if ($count == null) {
      return self::$DEFAULT_COUNT;
    } elseif (is_numeric($count)) {
      return intval($count);
    } else {
      throw new SocialSpiException("Parameter " . self::$COUNT . " (" . $count . ") is not a number.", ResponseError::$BAD_REQUEST);
    }
  }

  /**
   *
   * @return string
   */
  public function getSortBy() {
    $sortBy = $this->getParameter(self::$SORT_BY);
    return $sortBy == null ? CollectionOptions::TOP_FRIENDS_SORT : $sortBy;
  }

  /**
   *
   * @return string
   */
  public function getSortOrder() {
    $sortOrder = $this->getParameter(self::$SORT_ORDER);
    if (empty($sortOrder)) {
      return CollectionOptions::SORT_ORDER_ASCENDING;
    } elseif ($sortOrder == CollectionOptions::SORT_ORDER_ASCENDING || $sortOrder == CollectionOptions::SORT_ORDER_DESCENDING) {
      return $sortOrder;
    } else {
      throw new SocialSpiException("Parameter " . self::$SORT_ORDER . " (" . $sortOrder . ") is not valid.", ResponseError::$BAD_REQUEST);
    }
  }

  /**
   *
   * @return string
   */
  public function getFilterBy() {
    return $this->getParameter(self::$FILTER_BY);
  }

  /**
   *
   * @return string
   */
  public function getFilterOperation() {
    $filterOp = $this->getParameter(self::$FILTER_OPERATION);
    if (empty($filterOp)) {
      return CollectionOptions::FILTER_OP_CONTAINS;
    } elseif ($filterOp == CollectionOptions::FILTER_OP_EQUALS || $filterOp == CollectionOptions::FILTER_OP_CONTAINS || $filterOp == CollectionOptions::FILTER_OP_STARTSWITH || $filterOp == CollectionOptions::FILTER_OP_PRESENT) {
      return $filterOp;
    } else {
      throw new SocialSpiException("Parameter " . self::$FILTER_OPERATION . " (" . $filterOp . ") is not valid.", ResponseError::$BAD_REQUEST);
    }
  }

  /**
   *
   * @return string
   */
  public function getFilterValue() {
    $filterValue = $this->getParameter(self::$FILTER_VALUE);
    return empty($filterValue) ? "" : $filterValue;
  }

  /**
   *
   * @param array $defaultValue
   * @return array
   */
  public function getFields(Array $defaultValue = array()) {
    $result = array();
    $fields = $this->getListParameter(self::$FIELDS);
    if (is_array($fields)) {
      $result = $fields;
    }
    if (! count($result)) {
      return $defaultValue;
    } else {
      // often we get duplicate fields, remove'm
      $cleanResult = array();
      foreach ($result as $field) {
        if (! in_array($field, $cleanResult)) {
          $cleanResult[urldecode($field)] = urldecode($field);
        }
      }
      $result = $cleanResult;
    }
    return $result;
  }

  /**
   *
   * @param string $rpcMethod
   * @return string
   */
  public function getOperation($rpcMethod = null) {
    return $this->operation;
  }

  /**
   *
   * @param string $rpcMethod
   * @return object
   */
  public function getService($rpcMethod = null) {
    return $this->service;
  }

  /**
   * @return SecurityToken
   */
  public function getToken() {
    return $this->token;
  }

  public abstract function applyUrlTemplate($urlTemplate);

  public abstract function getParameter($paramName, $defaultValue = null);

  public abstract function getListParameter($paramName);
}

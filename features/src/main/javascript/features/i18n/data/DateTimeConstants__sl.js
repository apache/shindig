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

var gadgets = gadgets || {};

gadgets.i18n = gadgets.i18n || {};

gadgets.i18n.DateTimeConstants = {
  ERAS:["pr. n. \u0161t.","po Kr."],
  ERANAMES:["pred na\u0161im \u0161tetjem","na\u0161e \u0161tetje"],
  NARROWMONTHS:["j","f","m","a","m","j","j","a","s","o","n","d"],
  MONTHS:["januar","februar","marec","april","maj","junij","julij","avgust","september","oktober","november","december"],
  SHORTMONTHS:["jan","feb","mar","apr","maj","jun","jul","avg","sep","okt","nov","dec"],
  WEEKDAYS:["nedelja","ponedeljek","torek","sreda","\u010detrtek","petek","sobota"],
  SHORTWEEKDAYS:["ned","pon","tor","sre","\u010det","pet","sob"],
  NARROWWEEKDAYS:["n","p","t","s","\u010d","p","s"],
  SHORTQUARTERS:["Q1","Q2","Q3","Q4"],
  QUARTERS:["1. \u010detrtletje","2. \u010detrtletje","3. \u010detrtletje","4. \u010detrtletje"],
  AMPMS:["dop.","pop."],
  DATEFORMATS:["EEEE, dd. MMMM y","dd. MMMM y","d. MMM. yyyy","d. MM. yy"],
  TIMEFORMATS:["H:mm:ss zzzz","HH:mm:ss z","HH:mm:ss","HH:mm"],
  FIRSTDAYOFWEEK: 0,
  WEEKENDRANGE: [5, 6],
  FIRSTWEEKCUTOFFDAY: 3
};
gadgets.i18n.DateTimeConstants.STANDALONENARROWMONTHS = gadgets.i18n.DateTimeConstants.NARROWMONTHS;
gadgets.i18n.DateTimeConstants.STANDALONEMONTHS = gadgets.i18n.DateTimeConstants.MONTHS;
gadgets.i18n.DateTimeConstants.STANDALONESHORTMONTHS = gadgets.i18n.DateTimeConstants.SHORTMONTHS;
gadgets.i18n.DateTimeConstants.STANDALONEWEEKDAYS = gadgets.i18n.DateTimeConstants.WEEKDAYS;
gadgets.i18n.DateTimeConstants.STANDALONESHORTWEEKDAYS = gadgets.i18n.DateTimeConstants.SHORTWEEKDAYS;
gadgets.i18n.DateTimeConstants.STANDALONENARROWWEEKDAYS = gadgets.i18n.DateTimeConstants.NARROWWEEKDAYS;

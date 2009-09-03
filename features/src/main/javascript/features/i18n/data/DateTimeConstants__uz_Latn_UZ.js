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
  ERAS:["BCE","CE"],
  ERANAMES:["BCE","CE"],
  NARROWMONTHS:["Y","F","M","A","M","I","I","A","S","O","N","D"],
  MONTHS:["\u041c\u0443\u04b3\u0430\u0440\u0440\u0430\u043c","\u0421\u0430\u0444\u0430\u0440","\u0420\u0430\u0431\u0438\u0443\u043b-\u0430\u0432\u0432\u0430\u043b","\u0420\u0430\u0431\u0438\u0443\u043b-\u043e\u0445\u0438\u0440","\u0416\u0443\u043c\u043e\u0434\u0438\u0443\u043b-\u0443\u043b\u043e","\u0416\u0443\u043c\u043e\u0434\u0438\u0443\u043b-\u0443\u0445\u0440\u043e","\u0420\u0430\u0436\u0430\u0431","\u0428\u0430\u044a\u0431\u043e\u043d","\u0420\u0430\u043c\u0430\u0437\u043e\u043d","\u0428\u0430\u0432\u0432\u043e\u043b","\u0417\u0438\u043b-\u049b\u0430\u044a\u0434\u0430","\u0417\u0438\u043b-\u04b3\u0438\u0436\u0436\u0430"],
  SHORTMONTHS:["Yanv","Fev","Mar","Apr","May","Iyun","Iyul","Avg","Sen","Okt","Noya","Dek"],
  WEEKDAYS:["yakshanba","dushanba","seshanba","chorshanba","payshanba","juma","shanba"],
  SHORTWEEKDAYS:["Yaksh","Dush","Sesh","Chor","Pay","Jum","Shan"],
  NARROWWEEKDAYS:["Y","D","S","C","P","J","S"],
  SHORTQUARTERS:["Q1","Q2","Q3","Q4"],
  QUARTERS:["Q1","Q2","Q3","Q4"],
  AMPMS:["AM","PM"],
  DATEFORMATS:["EEEE, y MMMM dd","y MMMM d","y MMM d","yy/MM/dd"],
  TIMEFORMATS:["HH:mm:ss zzzz","HH:mm:ss z","HH:mm:ss","HH:mm"],
  FIRSTDAYOFWEEK: 6,
  WEEKENDRANGE: [5, 6],
  FIRSTWEEKCUTOFFDAY: 2
};
gadgets.i18n.DateTimeConstants.STANDALONENARROWMONTHS = gadgets.i18n.DateTimeConstants.NARROWMONTHS;
gadgets.i18n.DateTimeConstants.STANDALONEMONTHS = gadgets.i18n.DateTimeConstants.MONTHS;
gadgets.i18n.DateTimeConstants.STANDALONESHORTMONTHS = gadgets.i18n.DateTimeConstants.SHORTMONTHS;
gadgets.i18n.DateTimeConstants.STANDALONEWEEKDAYS = gadgets.i18n.DateTimeConstants.WEEKDAYS;
gadgets.i18n.DateTimeConstants.STANDALONESHORTWEEKDAYS = gadgets.i18n.DateTimeConstants.SHORTWEEKDAYS;
gadgets.i18n.DateTimeConstants.STANDALONENARROWWEEKDAYS = gadgets.i18n.DateTimeConstants.NARROWWEEKDAYS;

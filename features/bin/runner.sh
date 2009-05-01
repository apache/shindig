#!/bin/sh

# This script is designed to be run from the features module root directory.
# It's very simple; feel free to make it more robust.

java -cp ./bin/js.jar org.mozilla.javascript.tools.shell.Main src/test/javascript/features/alltests.js BatchTest ActivitiesTest PeopleTest AppdataTest OsapiTest

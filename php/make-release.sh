#!/bin/bash

#### Settings, feel free to adjust these as required ####

VERSION=SNAPSHOT
NAME="shindig-${VERSION}"
OPWD=`pwd`

#### Build a clean php-shindig tarball

echo "Creating $NAME release files"

# remove any old work that may have remained
echo "  Removing old temp directory and files.."
rm -rf /tmp/$NAME
rm -rf $NAME.tar.gz $NAME.zip $NAME.tar.bz2

# create temp dir and copy files from current path
echo "  Creating new structure, copying php, features and samples files.."
mkdir -p /tmp/$NAME
cp -r * /tmp/$NAME
cp -r ../features /tmp/$NAME/
cp -r ../javascript /tmp/$NAME
cp .htaccess /tmp/$NAME
cp ../{COMMITTERS,COPYING,DISCLAIMER,LICENSE,NOTICE} /tmp/$NAME
cp ../config/{oauth.json,container.js} /tmp/$NAME/config/
cd /tmp/$NAME

# remove those pesky .svn directories
echo "  Removing unwanted files"
rm -f *.gz *.bz2 *.zip make-release.sh
find . -name ".svn" -exec rm -rf {} \; &>/dev/null
find . -name "pom.xml" -exec rm -f {} \; &>/dev/null

# and rewrite the container.php file to use the different release file paths (features/ instead of ../features, etc)
echo "  Rewriting default configuration to release structure"
cd /tmp/$NAME/config
cat container.php | sed "s/\/..\/..\//\/..\//" > container.php.new
mv container.php.new container.php

# and create the final tar.gz, tar.bz2 and .zip files
cd /tmp
echo "  Creating $OPWD/$NAME.tar.gz"
tar c $NAME | gzip > $OPWD/$NAME.tar.gz
echo "  Creating $OPWD/$NAME.tar.bz2"
tar c $NAME | bzip2 > $OPWD/$NAME.tar.bz2
echo "  Creating $OPWD/$NAME.zip"
zip -r $OPWD/$NAME.zip $NAME &>/dev/null

echo "  Removing temp files"
rm -rf /tmp/$NAME


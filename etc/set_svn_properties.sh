
# this script will set the proper svn properties on all the files in the tree
# It pretty much requires a gnu compatible xargs (for the -r flag).  Running
# on Linux is probably the best option


find . -name "*.java" | xargs -n 1 -r svn propset svn:eol-style native
find . -name "*.java" | xargs -n 1 -r  svn propset svn:keywords "Rev Date"


find . -name "*.xml" | xargs -n 1 -r  svn propset svn:mime-type text/xml
find . -name "*.xml" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.xml" | xargs -n 1 -r  svn propset svn:keywords "Rev Date"

find . -name "*.xsl" | xargs -n 1 -r  svn propset svn:mime-type text/xml
find . -name "*.xsl" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.xsl" | xargs -n 1 -r  svn propset svn:keywords "Rev Date"

find . -name "*.xsd" | xargs -n 1 -r  svn propset svn:mime-type text/xml
find . -name "*.xsd" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.xsd" | xargs -n 1 -r  svn propset svn:keywords "Rev Date"

find . -name "*.wsdl" | xargs -n 1 -r  svn propset svn:mime-type text/xml
find . -name "*.wsdl" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.wsdl" | xargs -n 1 -r  svn propset svn:keywords "Rev Date"

find . -name "*.properties" | xargs -n 1 -r  svn propset svn:mime-type text/plain
find . -name "*.properties" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.properties" | xargs -n 1 -r  svn propset svn:keywords "Rev Date"

find . -name "*.txt" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.txt" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "*.htm*" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.htm*" | xargs -n 1 -r  svn propset svn:mime-type text/html
find . -name "*.htm*" | xargs -n 1 -r  svn propset svn:keywords "Rev Date"

find . -name "README*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "README*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "LICENSE*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "LICENSE*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "NOTICE*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "NOTICE*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "TODO*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "TODO*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "KEYS*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "KEYS*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "*.png" | xargs -n 1 -r  svn propset svn:mime-type image/png
find . -name "*.gif" | xargs -n 1 -r  svn propset svn:mime-type image/gif
find . -name "*.jpg" | xargs -n 1 -r  svn propset svn:mime-type image/jpeg
find . -name "*.jpeg" | xargs -n 1 -r  svn propset svn:mime-type image/jpeg

find . -name "*.fragment" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.fragment" | xargs -n 1 -r  svn propset svn:mime-type text/xml

find . -name "*.wsdd" | xargs -n 1 -r  svn propset svn:mime-type text/xml
find . -name "*.wsdd" | xargs -n 1 -r  svn propset svn:eol-style native

find . -name "ChangeLog*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "ChangeLog*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "*.sh" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.sh" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain
find . -name "*.sh" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:executable ""

find . -name "*.bat" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.bat" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain
find . -name "*.bat" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:executable ""

find . -name "*.cmd" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "*.cmd" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain
find . -name "*.cmd" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:executable ""

find . -name "INSTALL*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "INSTALL*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "COPYING*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "COPYING*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "NEWS*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "NEWS*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

find . -name "DISCLAIMER*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:eol-style native
find . -name "DISCLAIMER*" | grep -v ".svn" | xargs -n 1 -r  svn propset svn:mime-type text/plain

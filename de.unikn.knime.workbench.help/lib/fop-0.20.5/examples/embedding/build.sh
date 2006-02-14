#!/bin/sh
# This file should be executable.
echo
echo "Fop Build System"
echo "----------------"
echo

if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi
LIBDIR=../../lib
LOCALCLASSPATH=$JAVA_HOME/lib/tools.jar:$JAVA_HOME/lib/classes.zip
LOCALCLASSPATH=$LOCALCLASSPATH:$LIBDIR/ant-1.5.1.jar
LOCALCLASSPATH=$LOCALCLASSPATH:$LIBDIR/xml-apis.jar
LOCALCLASSPATH=$LOCALCLASSPATH:$LIBDIR/xercesImpl-2.2.1.jar
LOCALCLASSPATH=$LOCALCLASSPATH:$LIBDIR/xalan-2.4.1.jar

ANT_HOME=$LIBDIR

echo
echo Building with classpath $LOCALCLASSPATH
echo Starting Ant...
echo

$JAVA_HOME/bin/java -Dant.home=$ANT_HOME -classpath "$LOCALCLASSPATH" org.apache.tools.ant.Main $*

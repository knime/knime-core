#! /bin/sh
#
# Shell script to run FOP, adapted from the Jakarta-Ant project.

if [ -f $HOME/.foprc ] ; then 
  . $HOME/.foprc
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
esac

if [ -z "$FOP_HOME" ] ; then
  # try to find FOP
  if [ -d /opt/fop ] ; then 
    FOP_HOME=/opt/fop
  fi

  if [ -d ${HOME}/opt/fop ] ; then 
    FOP_HOME=${HOME}/opt/fop
  fi

  ## resolve links - $0 may be a link to fop's home
  PRG=$0
  progname=`basename $0`
  
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '.*/.*' > /dev/null; then
	PRG="$link"
    else
	PRG="`dirname $PRG`/$link"
    fi
  done
  
  FOP_HOME=`dirname "$PRG"`

fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$FOP_HOME" ] &&
    FOP_HOME=`cygpath --unix "$FOP_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

if [ -z "$JAVACMD" ] ; then 
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then 
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD=$JAVA_HOME/jre/sh/java
    else
      JAVACMD=$JAVA_HOME/bin/java
    fi
  else
    JAVACMD=java
  fi
fi
 
if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit
fi

if [ -n "$CLASSPATH" ] ; then
  LOCALCLASSPATH=$CLASSPATH
fi

# add fop.jar, which resides in $FOP_HOME/build
LOCALCLASSPATH=${FOP_HOME}/build/fop.jar:$LOCALCLASSPATH

# add in the dependency .jar files, which reside in $FOP_HOME/lib
DIRLIBS=${FOP_HOME}/lib/*.jar
for i in ${DIRLIBS}
do
    # if the directory is empty, then it will return the input string
    # this is stupid, so case for it
    if [ "$i" != "${DIRLIBS}" ] ; then
      if [ -z "$LOCALCLASSPATH" ] ; then
        LOCALCLASSPATH=$i
      else
        LOCALCLASSPATH="$i":$LOCALCLASSPATH
      fi
    fi
done

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  FOP_HOME=`cygpath --path --windows "$FOP_HOME"`
  JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  LOCALCLASSPATH=`cygpath --path --windows "$LOCALCLASSPATH"`
fi

$JAVACMD -classpath "$LOCALCLASSPATH" $FOP_OPTS org.apache.xalan.xslt.Process "$@"


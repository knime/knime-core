#!/bin/bash

SOURCE="$0"
while [[ -h "$SOURCE" ]]; do
  SOURCE="$(readlink "$SOURCE")"
done
KNIME_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
APP_DIR="$KNIME_DIR/knime.app/Contents/MacOS"

if [[ "$1" == "-vm" ]]; then
  JAVA="$2"
  shift 2
elif [[ -x "$KNIME_DIR/jre/bin/java" ]]; then
  # start KNIME with the java in the jre folder
  JAVA="$KNIME_DIR/jre/bin/java"
fi

if [[ -n "$JAVA" ]]; then
  # extract parameters from knime.ini
  INI=$(cat "$APP_DIR/knime.ini")
  APPARGS="-product org.knime.product.KNIME_PRODUCT -name KNIME -showsplash 600"
  VMARGS=""
  for arg in $INI; do
    if [[ -n "$VMARGS" ]]; then
      VMARGS="$VMARGS $arg"
    elif [[ "$arg" == "-vmargs" ]]; then
      VMARGS=" "
    else
      APPARGS="$APPARGS $arg"
    fi
  done

  # lookup the launcher jar
  CLASSPATH=$(find "$KNIME_DIR/plugins" -name "org.eclipse.equinox.launcher_*.jar" | sort | tail -1)

  # fire it up
  cd "$APP_DIR"
  "$JAVA" $VMARGS -classpath $CLASSPATH org.eclipse.equinox.launcher.Main $APPARGS
  cd -
else
  # start normally
  "$APP_DIR/knime" "$@"
fi

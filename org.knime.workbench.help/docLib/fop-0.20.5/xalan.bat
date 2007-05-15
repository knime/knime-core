@ECHO OFF

set LIBDIR=lib
set LOCALCLASSPATH=build/fop.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xml-apis.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xercesImpl-2.2.1.jar
set LOCALCLASSPATH=%LOCALCLASSPATH%;%LIBDIR%\xalan-2.4.1.jar
java -cp %LOCALCLASSPATH% org.apache.xalan.xslt.Process %1 %2 %3 %4 %5 %6 %7 %8
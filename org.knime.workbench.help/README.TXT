|===============================================================
| Header
|
|
|
|===============================================================

Plugin
----------------
org.knime.workbench.help

Description
----------------
This plugin contributes help for the KNIME Framework into
the Eclipse Help-System.
Additionally, this contains the official design documents 
(UML diagrams, javadoc, ...)

Clients may contribute additional help (e.g. node info) through
extension points, offered by this plugin.

The folder "buildDoc" contains the build system that turns the
docbook source into the different output formats.
Note that this makes heavy use of the libs/xslt that reside
inside the "lib" folder.
 

Extension-Points
----------------


TODO
----------------
- Check, how diagrams etc. ... can be provided to the user.

The META-INF folder contains a copy of a Linux Debian mime.types file (/etc/mime.types). It's used to find
the mime types for file attachments. The VM should be able to do that to (though a mime.types file in 
jre/lib/resources.jar/META-INF/mimetypes.default is only 0.5kb large - this copy is 25kb).

The mimetypes.default file is missing java java 7u7:
http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7096063

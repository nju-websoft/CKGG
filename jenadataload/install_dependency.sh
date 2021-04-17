mvn install:install-file -q \
 -Dfile=/tmp/virt_jena3.jar \
 -DgroupId=com.openlink.virtuoso \
 -DartifactId=virt_jena3 \
 -Dversion=3.0 \
 -Dpackaging=jar \
 -DgeneratePom=true

mvn install:install-file -q \
 -Dfile=/tmp/virtjdbc4.jar \
 -DgroupId=com.openlink.virtuoso \
 -DartifactId=virtjdbc4 \
 -Dversion=4.0 \
 -Dpackaging=jar \
 -DgeneratePom=true
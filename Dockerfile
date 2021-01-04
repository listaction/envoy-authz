FROM amazoncorretto:11

## JAVA_HOME
ENV JAVA_HOME /usr/lib/jvm/java-11-oracle

VOLUME /tmp
ADD target/auth.jar app.jar
ENTRYPOINT ["java","-server","-Djava.security.egd=file:/dev/./urandom","-Xmx350M","-Xms350M","-XX:+CMSClassUnloadingEnabled","-XX:+UseConcMarkSweepGC","-jar","/app.jar"]


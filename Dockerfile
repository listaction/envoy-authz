FROM amazoncorretto:11

ENV JAVA_HOME /usr/lib/jvm/java-11-oracle

VOLUME /tmp
ADD auth/target/auth.jar app.jar
CMD ["java","-server","-Djava.security.egd=file:/dev/./urandom","-Xmx350M","-Xms350M","-XX:+CMSClassUnloadingEnabled","-XX:+UseConcMarkSweepGC","-jar","/app.jar"]

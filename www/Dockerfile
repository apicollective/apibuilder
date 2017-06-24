FROM flowdocker/play:0.0.67

ADD . /opt/play

WORKDIR /opt/play

RUN sbt 'project www' clean stage

WORKDIR www/target/universal/stage

ENTRYPOINT ["java", "-jar", "/root/environment-provider.jar", "--service", "play", "apibuilder-www", "bin/apibuilder-www"]

HEALTHCHECK --interval=5s --timeout=5s --retries=10 \
  CMD curl -f http://localhost:9000/_internal_/healthcheck || exit 1

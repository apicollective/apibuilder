FROM flowdocker/play:0.0.37

ADD . /opt/play

WORKDIR /opt/play

RUN sbt 'project www' clean stage

WORKDIR www/target/universal/stage

ENTRYPOINT ["java", "-jar", "/root/environment-provider.jar", "--service", "play", "apidoc", "bin/apidoc-www"]

FROM flowdocker/play_builder:latest-java17 as builder
ADD . /opt/play
WORKDIR /opt/play
RUN sbt 'project app' clean stage

FROM flowdocker/play:latest-java17
COPY --from=builder /opt/play /opt/play
WORKDIR /opt/play/app/target/universal/stage
ENTRYPOINT ["java", "-jar", "/root/environment-provider.jar", "--service", "play", "apibuilder-app", "bin/apibuilder-app"]
HEALTHCHECK --interval=5s --timeout=5s --retries=10 \
  CMD curl -f http://localhost:9000/_internal_/healthcheck || exit 1

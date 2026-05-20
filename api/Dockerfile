FROM flowcommerce/play_builder_java17:latest as builder
ADD . /opt/play
WORKDIR /opt/play
RUN sbt 'project api' clean stage

FROM flowcommerce/play_java17:latest
RUN wget -q https://cdn.flow.io/util/environment-provider/environment-provider.jar -O /root/environment-provider.jar && test -s /root/environment-provider.jar
COPY --from=builder /opt/play /opt/play
WORKDIR /opt/play/api/target/universal/stage
ENTRYPOINT ["java", "-jar", "/root/environment-provider.jar", "--service", "play", "apibuilder-api", "bin/apibuilder-api"]
HEALTHCHECK --interval=5s --timeout=5s --retries=10 \
  CMD curl -f http://localhost:9000/_internal_/healthcheck || exit 1

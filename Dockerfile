FROM flowdocker/play:0.0.65

MAINTAINER mbryzek@alum.mit.edu

ADD . /opt/play

WORKDIR /opt/play

RUN sbt clean stage

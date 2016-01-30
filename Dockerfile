FROM flowcommerce/play:0.0.7

MAINTAINER mbryzek@alum.mit.edu

ADD . /opt/play

WORKDIR /opt/play

RUN sbt -Dsbt.ivy.home=.ivy2 clean stage

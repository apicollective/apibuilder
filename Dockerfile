FROM giltarchitecture/ubuntu-jvm:0.6

MAINTAINER mbryzek@alum.mit.edu

ADD . /usr/share/apidoc

WORKDIR /usr/share/apidoc

RUN sbt -Dsbt.ivy.home=.ivy2 clean stage

RUN ln -s /usr/share/apidoc/api/target/universal/stage /usr/share/apidoc-api
RUN ln -s /usr/share/apidoc/www/target/universal/stage /usr/share/apidoc-www

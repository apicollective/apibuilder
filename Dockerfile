FROM giltarchitecture/ubuntu-jvm:0.5

MAINTAINER architecture@gilt.com

ADD . /usr/share/apidoc

WORKDIR /usr/share/apidoc

RUN sbt -Dsbt.ivy.home=.ivy2 clean stage

RUN ln -s /usr/share/apidoc/generator/target/universal/stage /usr/share/apidoc-generator
RUN ln -s /usr/share/apidoc/api/target/universal/stage /usr/share/apidoc-api
RUN ln -s /usr/share/apidoc/www/target/universal/stage /usr/share/apidoc-www

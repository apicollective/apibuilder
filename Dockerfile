FROM ubuntu:14.04

MAINTAINER architecture@gilt.com

RUN apt-get install -y wget

WORKDIR /tmp

RUN apt-get install -y software-properties-common
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
RUN echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
RUN add-apt-repository -y ppa:webupd8team/java
RUN apt-get update
RUN apt-get install -y oracle-java8-installer
RUN javac -version # test
RUN wget http://dl.bintray.com/sbt/debian/sbt-0.13.5.deb
RUN dpkg -i sbt-0.13.5.deb
RUN sbt update # first run of sbt is slow so cache the results

ADD . /usr/share/apidoc

WORKDIR /usr/share/apidoc

RUN sbt stage

RUN ln -s /usr/share/apidoc/api/target/universal/stage /usr/share/apidoc-api
RUN ln -s /usr/share/apidoc/www/target/universal/stage /usr/share/apidoc-www

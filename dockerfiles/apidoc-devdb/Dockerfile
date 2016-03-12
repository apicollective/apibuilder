FROM fedora

MAINTAINER michae1T@live.ca

RUN    dnf -y update \
    && dnf -y install ruby postgresql-server postgresql-contrib git which \
    && dnf clean all

RUN    cd /tmp \
    && git clone git://github.com/mbryzek/schema-evolution-manager.git \
    && cd schema-evolution-manager \
    && ruby ./configure.rb  --prefix / \
    && ./install.rb \
    && rm -rf /tmp/schema-evolution-manager

ADD start /opt/bin/
ADD start-pg.sh /opt/bin/

VOLUME ["/opt/schema"]
EXPOSE 5432

ENTRYPOINT [ "/opt/bin/start" ]


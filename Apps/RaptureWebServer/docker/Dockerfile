FROM gliderlabs/alpine:3.3
#see https://github.com/sillelien/base-alpine for DNS hacks below
MAINTAINER jonathan.major@incapturetechnologies.com

# About nsswitch.conf - see https://registry.hub.docker.com/u/frolvlad/alpine-oraclejdk8/dockerfile/
COPY rootfs /

# S6 is a process manager (helps with DNS)
ADD https://github.com/just-containers/s6-overlay/releases/download/v1.13.0.0/s6-overlay-amd64.tar.gz /tmp/s6-overlay.tar.gz
RUN tar xvfz /tmp/s6-overlay.tar.gz -C / && \
  apk -U add dnsmasq jq curl && \
  chmod 755 /bin/*.sh /etc/services.d/dns/run /etc/services.d/dns-hack/run /etc/services.d/syslog/run && \
  mkdir /app && \
  echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf && \
  addgroup -g 999 app && \
  adduser -D  -G app -s /bin/false -u 999 app

# Install certificates. See https://github.com/denvazh/java/blob/master/openjdk7-jdk/Dockerfile for refrence
RUN apk --update upgrade
RUN apk-install bash openjdk8-jre ca-certificates && \
  find /usr/share/ca-certificates/mozilla/ -name *.crt -exec keytool -import -trustcacerts \
  -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts -storepass changeit -noprompt \
  -file {} -alias {} \; && \
  keytool -list -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts --storepass changeit

RUN  rm -rf /var/cache/apk/*

# Setup up required resources
COPY RaptureWebServer /opt/rapture/RaptureWebServer
# this contains config files for docker environment using link names
COPY config /opt/rapture/RaptureWebServer/etc/rapture/config

# Setup environment
ENV RAPTURE_CONFIG_HOME /opt/rapture/RaptureWebServer/etc/rapture/config
VOLUME ["/opt/rapture/RaptureWebServer/etc"]
WORKDIR /opt/rapture/RaptureWebServer/bin
EXPOSE 8665

# Allows jmx support on jmxremote.port=9011 and jmxremote.rmi.port=9012
EXPOSE 9011
EXPOSE 9012
ENTRYPOINT [ "/init" ]
CMD ["/opt/rapture/RaptureWebServer/bin/RaptureWebServer"]

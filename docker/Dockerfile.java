FROM eclipse-temurin:21-jdk-alpine

ARG MAVEN_VERSION=3.9.9
ARG MAVEN_SHA=7a9cdf674fc1703d6382f5f330b3d110ea1b512b51f1652846d9e4e8a588d766

RUN apk add --no-cache curl bash \
    && mkdir -p /opt/maven \
    && curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
       -o /tmp/maven.tar.gz \
    && echo "${MAVEN_SHA}  /tmp/maven.tar.gz" | sha256sum -c - \
    && tar -xzf /tmp/maven.tar.gz -C /opt/maven --strip-components=1 \
    && rm /tmp/maven.tar.gz \
    && ln -s /opt/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME=/opt/maven
ENV MAVEN_OPTS="-XX:MaxMetaspaceSize=512m -Xmx1g"

WORKDIR /workspace

CMD ["mvn", "--version"]

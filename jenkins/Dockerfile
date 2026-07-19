FROM jenkins/jenkins:lts-jdk21

USER root

# Debian's `maven` package pulls in its own default-jdk as a dependency,
# which on this base OS would be an older JDK than the one Jenkins itself
# bundles -- explicitly pinning JAVA_HOME/PATH here ensures `mvn` shell
# steps use the SAME JDK 21 Jenkins runs on (the project's Spring Boot
# modules target --release 21), not whatever apt resolves.
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Docker CLI only (no daemon) -- the host's docker.sock is bind-mounted in,
# so `docker build`/`docker compose` run "outside of docker" against the
# same engine that runs the rest of the stack, no nested daemon needed.
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl gnupg unzip \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" \
      > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*

# Maven (backend build/test) and Firefox (headless Angular unit tests, same
# karma-firefox-launcher setup used for local dev -- see Frontend/karma.conf.js)
RUN apt-get update && apt-get install -y --no-install-recommends \
      maven firefox-esr \
    && rm -rf /var/lib/apt/lists/*

# Node.js 20 (Angular build/test)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# Official SonarQube Scanner CLI (used for the Angular/TypeScript analysis --
# the backend Java modules use the sonar-maven-plugin instead, invoked via
# `mvn sonar:sonar`, so they don't need this)
RUN curl -fsSL -o /tmp/sonar-scanner.zip \
      https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-6.2.1.4610-linux-x64.zip \
    && unzip -q /tmp/sonar-scanner.zip -d /opt \
    && mv /opt/sonar-scanner-6.2.1.4610-linux-x64 /opt/sonar-scanner \
    && rm /tmp/sonar-scanner.zip \
    && ln -s /opt/sonar-scanner/bin/sonar-scanner /usr/local/bin/sonar-scanner

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Skip the first-run setup wizard entirely -- JCasC (mounted in via
# docker-compose, see casc.yaml) provisions the admin user and the pipeline
# job declaratively instead.
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc.yaml

# Stays root: the mounted host docker.sock already grants root-equivalent
# access to the host's Docker engine regardless of the container's own user,
# so dropping to the jenkins user here would need its GID matched to the
# socket's group (fragile/host-dependent) for no real security gain.

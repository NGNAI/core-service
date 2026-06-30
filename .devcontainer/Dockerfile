FROM maven:3.9.11-eclipse-temurin-25

ENV MAVEN_CONFIG=/home/vscode/.m2

RUN apt-get update \
    && apt-get install -y --no-install-recommends git openssh-client procps curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -ms /bin/bash vscode \
    && mkdir -p /home/vscode/.m2 \
    && chown -R vscode:vscode /home/vscode

USER vscode
WORKDIR /workspaces/core-service
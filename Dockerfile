FROM ubuntu:20.04

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && apt-get -y install make curl build-essential default-jdk git
RUN cd /opt && git clone https://github.com/LimeChain/wasmer-java
RUN curl https://sh.rustup.rs -sSf | bash -s -- -y
ENV PATH="/root/.cargo/bin:${PATH}"
RUN whereis java
RUN find /usr/lib/jvm/java*
RUN cd /opt/wasmer-java && make build && make run-example EXAMPLE=Import > output.txt && make run-example EXAMPLE=Memory >> output.txt
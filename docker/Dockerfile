FROM public.ecr.aws/amazonlinux/amazonlinux:2023

ENV LANG=C.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
ENV PATH="$JAVA_HOME/bin:$PATH"

# Install dependencies
RUN yum update -y && \
    yum install -y wget tar gzip gcc gcc-c++ make git maven 

# Install OpenJDK 21 (Amazon Corretto)
RUN yum install -y java-21-amazon-corretto-devel

# Install GCC 13 (Amazon Linux 2023 provides gcc13)
#RUN yum install -y gcc13 gcc13-c++

# Set GCC 13 as default
#RUN alternatives --install /usr/bin/gcc gcc /usr/bin/gcc13 100 && \
#    alternatives --install /usr/bin/g++ g++ /usr/bin/g++13 100

# Install Ollama
RUN curl -fsSL https://ollama.com/install.sh | sh

# Verify installations
RUN java -version && gcc --version && ollama --version

RUN ollama start & sleep 15 && \
    ollama pull codellama:latest 

COPY .ssh /root/.ssh

RUN chmod 700 /root/.ssh && chmod 600 /root/.ssh/*

RUN git clone git@github.com:abhilashraju/bmcshell.git /root/bmcshell # 

RUN cd /root/bmcshell && mvn package

#RUN ollama start & sleep 15 && \
 #   ollama pull llama4

RUN yum install -y zstd
#install PEL tool
RUN yum install -y python3 python3-devel python3-pip
RUN yum install -y  python3-setuptools
RUN git clone git@github.com:ibm-openbmc/openpower-pel-parsers.git /root/openpower-pel-parsers
RUN cd /root/openpower-pel-parsers ;python3 setup.py install;pip3 install .

RUN cd /root/bmcshell && git pull && mvn package #run14
RUN rm -rf /root/.ssh
RUN echo 'java -jar /root/bmcshell/target/bmcshell-0.0.1-SNAPSHOT.jar' > bmc-shell.sh;chmod 777 bmc-shell.sh
CMD ollama serve & ./bmc-shell.sh ; bash

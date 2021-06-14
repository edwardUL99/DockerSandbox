FROM ubuntu:18.04

RUN useradd -ms /bin/bash sandbox \
    && chown root:sandbox /home/sandbox \
    && chmod 1775 /home/sandbox

RUN apt-get -y update && apt-get -y install \
                    software-properties-common \
                   && add-apt-repository ppa:deadsnakes/ppa \
                   && apt-get -y install \
                        g++ \
                        freeglut3-dev \
                        libglu1-mesa-dev \
                        mesa-common-dev \
                        libomp-dev && rm -rf /var/lib/apt/lists/*

WORKDIR /home/sandbox
CMD [ "/bin/bash" ]

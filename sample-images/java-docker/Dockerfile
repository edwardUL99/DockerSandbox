FROM openjdk:11.0.1-slim

RUN useradd -ms /bin/bash sandbox \
    && chown root:sandbox /home/sandbox \
    && chmod 1775 /home/sandbox

WORKDIR /home/sandbox
CMD [ "/bin/bash" ]

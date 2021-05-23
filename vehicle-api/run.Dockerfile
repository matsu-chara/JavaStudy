FROM index.docker.io/paketobuildpacks/run:base-cnb
USER root
RUN apt-get update && \
   apt-get install -y --no-install-recommends imagemagick && \
   apt-get clean && \
   rm -rf /var/lib/apt/lists/*
USER cnb

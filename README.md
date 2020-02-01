# HTTP Dope

Online utilities for whatever.

[![Build](https://github.com/alexandru/http-dope/workflows/build/badge.svg?branch=master)](https://github.com/alexandru/http-dope/actions?query=branch%3Amaster+workflow%3Abuild) [![Deploy](https://github.com/alexandru/http-dope/workflows/deploy/badge.svg?branch=master)](https://github.com/alexandru/http-dope/actions?query=branch%3Amaster+workflow%3Adeploy)

## Docker workflow

```bash
# For publishing a local image

sbt docker:publishLocal

# For running locally
#
# Tip: specify host.docker.internal instead of localhost for connecting to
# local services

docker run -p 8080:8080 --name http-dope -d \
  -e "DOPE_MAXMIND_GEOIP_API_KEY=$DOPE_MAXMIND_GEOIP_API_KEY" \
  alexelcu/http-dope:latest

# Show built images
docker images
# To create a tag
docker tag <image-id> alexelcu/http-dope:<tag-name>
# To push a tag to Docker Hub
docker push alexelcu/http-dope:<tag-name>
```

Also for publishing we can simply do:

```bash
sbt docker:publish
```

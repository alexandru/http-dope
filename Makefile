NAME   := alexelcu/http-dope
TAG    := $$(./scripts/new-version)
IMG    := ${NAME}:${TAG}
LATEST := ${NAME}:latest

build:
	./scripts/docker-publish-local.sh
	docker tag "${LATEST}" "${IMG}"

push:
	docker push "${LATEST}"
	docker push "${IMG}"

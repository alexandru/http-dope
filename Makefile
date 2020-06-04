NAME   := alexelcu/http-dope
TAG    := $$(./scripts/new-version)
IMG    := ${NAME}:${TAG}
LATEST := ${NAME}:latest
SBT    := ./scripts/sbt -java-home "$${JAVA_HOME}"

clean:
	${SBT} clean

build:
	${SBT} update stage

test: build
	${SBT} test

refresh-db:
	./scripts/refresh-maxmind-db.sh

publish-local: build
	${SBT} docker:publishLocal
	docker tag "${LATEST}" "${IMG}"

push: publish-local
	docker push "${LATEST}"
	docker push "${IMG}"

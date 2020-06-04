NAME   := alexelcu/http-dope
TAG    := $$(./scripts/new-version)
IMG    := ${NAME}:${TAG}
LATEST := ${NAME}:latest

clean:
	sbt clean

build:
	sbt update stage

test: build
	sbt test

refresh-db:
	./scripts/refresh-maxmind-db.sh

publish-local: build
	sbt docker:publishLocal
	docker tag "${LATEST}" "${IMG}"

push: publish-local
	docker push "${LATEST}"
	docker push "${IMG}"

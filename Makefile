NAME   := alexelcu/http-dope
TAG    := $$(./scripts/new-version)
IMG    := ${NAME}:${TAG}
LATEST := ${NAME}:latest
SBT    := ./scripts/sbt -java-home "$${JAVA_HOME}"

clean:
	${SBT} clean

test:
	${SBT} test

refresh-db:
	./scripts/refresh-maxmind-db.sh

package:
	${SBT} clean update stage docker:publishLocal
	docker tag "${LATEST}" "${IMG}"

push: package
	docker push "${LATEST}"
	docker push "${IMG}"

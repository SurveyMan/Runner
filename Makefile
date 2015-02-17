SHELL:=/bin/bash

.PHONY := clean compile test test_travis package

clean:
	mvn clean

compile: clean
	mvn compile

test: compile
	mvn test

test_travis : compile
	mvn -Ptravis test

package: compile
	mvn package -DskipTests

SHELL:=/bin/bash
mvnargs := -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2
projectdir := $(shell pwd)

.PHONY := clean compile test test_travis package deps

clean:
	mvn clean

compile: clean
	mvn compile -DskipTests

test: compile
	mvn test

test_travis : deps compile
	mvn -Ptravis test

package: compile
	mvn package -DskipTests

deps: lib/java-aws-mturk.jar 
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/java-aws-mturk.jar -DartifactId=java-aws-mturk
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/aws-mturk-dataschema.jar -DartifactId=aws-mturk-dataschema
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/aws-mturk-wsdl.jar -DartifactId=aws-mturk-wsdl

lib/java-aws-mturk.jar:
	./scripts/setup.sh

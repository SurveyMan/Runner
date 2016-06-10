SHELL:=/bin/bash
mvnargs := -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2
projectdir := $(shell pwd)

.PHONY := clean compile test test_travis package deps

package: compile
	mvn package -DskipTests
	unzip -o lib/aws-mturk-clt.jar
	unzip -o lib/aws-mturk-dataschema.jar
	unzip -o lib/aws-mturk-wsdl.jar
	unzip -o lib/java-aws-mturk.jar
	jar uf runner.jar com/*

clean:
	rm -rf ~/surveyman/.metadata
	rm -rf lib
	mvn clean

compile: deps
	mvn compile -DskipTests

test: compile
	mvn test

test_travis : compile
	mvn -Ptravis test

deps: lib/java-aws-mturk.jar 
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/java-aws-mturk.jar -DartifactId=java-aws-mturk
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/aws-mturk-dataschema.jar -DartifactId=aws-mturk-dataschema
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/aws-mturk-wsdl.jar -DartifactId=aws-mturk-wsdl

lib/java-aws-mturk.jar:
	./scripts/setup.sh

#!/bin/env -S bash -u

mvn clean package
java -jar target/icfpc-2019-0.0.0.1-SNAPSHOT-jar-with-dependencies.jar --merge-solutions --sol-folder saved --candidates-folder $1


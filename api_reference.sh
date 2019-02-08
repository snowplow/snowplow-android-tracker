#!/bin/sh

javadoc -d docs/ $(find ./snowplow-tracker/src/main/ -name *.java)

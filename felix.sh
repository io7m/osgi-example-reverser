#!/bin/sh -ex

exec java -Dfelix.config.properties=file:felix.properties -jar "$HOME/var/felix/current/bin/felix.jar"

#!/bin/sh -ex

exec java \
  -Dfelix.config.properties=file:felix.properties \
  -Djava.security.policy=all.policy \
  -jar "$HOME/var/felix/current/bin/felix.jar"

#!/usr/bin/env bash

./gradlew clean build jib
kubectl delete deploy micrometer-stackdriver-demo
kubectl create --save-config -f deployment.yml

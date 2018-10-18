#!/usr/bin/env bash

./gradlew jib
kubectl delete svc micrometer-stackdriver-demo-svc
kubectl delete deploy micrometer-stackdriver-demo
kubectl create --save-config -f deployment.yml
kubectl create --save-config -f service.yml

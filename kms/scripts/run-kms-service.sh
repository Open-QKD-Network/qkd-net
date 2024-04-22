#!/bin/sh

touch ~/qkd_logs/kms_service.log
JAR_FILE="kms-service/target/kms-service-0.0.1-SNAPSHOT.jar"
if [ ! -f $JAR_FILE ]; then
    echo "File ${JAR_FILE} not found. Please build the project first."
    exit 1
fi
java -jar kms-service/target/kms-service-0.0.1-SNAPSHOT.jar > ~/qkd_logs/kms_service.log

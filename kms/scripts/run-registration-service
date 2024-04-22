#!/bin/sh

touch ~/qkd_logs/registration_service.log
JAVA_FILE="registration-service/target/registration-service-0.0.1-SNAPSHOT.jar"
if [  ! -f $JAVA_FILE ]; then
    echo "File ${JAVA_FILE} not found. Please build the project first."
    exit 1
fi
java -jar registration-service/target/registration-service-0.0.1-SNAPSHOT.jar > ~/qkd_logs/registration_service.log

#!/bin/sh

touch ~/qkd_logs/kms_service.log
java -jar kms-service/target/kms-service-0.0.1-SNAPSHOT.jar > ~/qkd_logs/kms_service.log

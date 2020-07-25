#!/bin/sh

touch ~/qkd_logs/registration_service.log
java -jar registration-service/target/registration-service-0.0.1-SNAPSHOT.jar > ~/qkd_logs/registration_service.log

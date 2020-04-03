#!/bin/sh

touch ~/qkd_logs/kms_qnl_service.log
./mvnw exec:java > ~/qkd_logs/kms_qnl_service.log

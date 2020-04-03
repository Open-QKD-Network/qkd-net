#!/bin/sh

touch ~/qkd_logs/key_routing_service.log
./mvnw exec:java > ~/qkd_logs/key_routing_service.log

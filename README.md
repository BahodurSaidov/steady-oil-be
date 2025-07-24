# MQTT Data Processor

This app receives data using api, and stores data to MongoDB and exchange data with EMQX MQTT server by sensor id.

## Commands

For running temporary mongodb server in docker container:
`docker run -d -p 27017:27017 --name some-mongo -e MONGO_INITDB_DATABASE=sensors -e MONGO_INITDB_ROOT_USERNAME=admin -e MONGO_INITDB_ROOT_PASSWORD=secret mongo:7.0.4`
For running EMQX server in docker container:
`docker run -d --name mqtt-emqx -p 1883:1883 -p 8883:8883 -p 18083:18083 -e EMQX_NAME=mqtt-emqx-node -e EMQX_NODE__PROCESS_LIMIT=2097152 emqx/emqx:latest`

## EMQX Dashboard

http://localhost:18083/
admin
admin
#!/bin/bash
set -ex

cd /certs
openssl req -new -nodes -out root.csr \
 -keyout root.key -subj "/CN=docker.internal"
chmod og-rwx root.key

openssl x509 -req -in root.csr -days 3650 \
 -extfile /etc/ssl/openssl.cnf -extensions v3_ca \
 -signkey root.key -out root.crt

openssl req -new -nodes -out server.csr \
 -keyout server.key -subj "/CN=host.docker.internal"
chmod og-rwx server.key

openssl x509 -req -in server.csr -days 3650 \
 -CA root.crt -CAkey root.key -CAcreateserial \
 -out server.crt

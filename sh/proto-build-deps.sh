#!/usr/bin/env bash

set -o errexit

GOLANG_PROTOBUF_VERSION=1.28.1
GRPC_GATEWAY_VERSION=1.16.0

go install github.com/cosmos/cosmos-proto/cmd/protoc-gen-go-pulsar@latest
go install google.golang.org/protobuf/cmd/protoc-gen-go@v${GOLANG_PROTOBUF_VERSION}
go install github.com/grpc-ecosystem/grpc-gateway/protoc-gen-grpc-gateway@v${GRPC_GATEWAY_VERSION}
go install github.com/grpc-ecosystem/grpc-gateway/protoc-gen-swagger@v${GRPC_GATEWAY_VERSION}
go install cosmossdk.io/orm/cmd/protoc-gen-go-cosmos-orm@v1.0.0-beta.1

# install all gogo protobuf binaries
# git clone https://github.com/cosmos/gogoproto.git; \
#  cd gogoproto; \
#  go mod download; \
#  make install

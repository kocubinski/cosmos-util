#!/bin/bash

set -o errexit

go list ./... > pkgs.txt
lines=`cat pkgs.txt | wc -l`
length=$(( $lines / 4 ))
split -d -l $length pkgs.txt pkgs.txt.part.

#xargs --arg-file=pkgs.txt.part."$1" ruby -e 'p ARGV' go test -mod=readonly -timeout 30m -race -tags='cgo ledger test_ledger_mock' -count=1
#xargs --arg-file=pkgs.txt.part."$1" -L 1 --interactive go test -mod=readonly -timeout 30m -race -tags='cgo ledger test_ledger_mock' -count=1

cat pkgs.txt.part."$1" | xargs go test -mod=readonly -timeout 30m -race -tags='norace ledger test_ledger_mock' -count=1

rm pkgs.txt*

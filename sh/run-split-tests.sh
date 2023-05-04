#!/bin/bash

go list ./... > pkgs.txt && split -d -n l/4 pkgs.txt pkgs.txt.part.

#xargs --arg-file=pkgs.txt.part."$1" ruby -e 'p ARGV' go test -mod=readonly -timeout 30m -race -tags='cgo ledger test_ledger_mock' -count=1
#xargs --arg-file=pkgs.txt.part."$1" -L 1 --interactive go test -mod=readonly -timeout 30m -race -tags='cgo ledger test_ledger_mock' -count=1

xargs --arg-file=pkgs.txt.part."$1" go test -mod=readonly -timeout 30m -race -tags='norace ledger test_ledger_mock' -count=1

rm pkgs.txt*

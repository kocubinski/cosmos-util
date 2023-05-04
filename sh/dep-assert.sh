#!/usr/bin/env bash

DEP_REGEX=$1


for d in $(find . -name 'go.mod' | xargs -L 1 dirname)
do
    # if `cd $d && go list -test -f '{{ .Imports }}' ./... | grep ${DEP_REGEX}`; then
    #     echo "${d} has a dependency on $DEP_REGEX!"
    #     exit 1
    # fi

    cd $d
    #go list -test -f '{{ with $i := .ImportPath }} {{ range .Imports }} {{$i}} : {{.}}\n {{end}}{{end}}' ./... | grep $DEP_REGEX
    #go list -test -f '{{ join .Imports (printf " : %s\n" .ImportPath) }}' ./... | grep -E $DEP_REGEX
    #go list -test -f '{{ with $i := .ImportPath }} {{ range .Imports }} {{$i}} : {{.}}\n {{end}}{{end}}' ./... | grep $DEP_REGEX
    go list -test -f '{{ join .Imports (printf ";%s\n" .ImportPath) }}' ./... \
        | grep -E "^.*${DEP_REGEX}.*;" \
        | grep -v -E ";.*${DEP_REGEX}.*" \
        | awk -F ';' '{print $2 " -> " $1}'
    cd - > /dev/null
done

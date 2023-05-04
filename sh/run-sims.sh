#!/bin/bash

set -o errexit

make test-sim-import-export &
sim1=$!
make test-sim-after-import &
sim2=$!
make test-sim-multi-seed-short &
sim3=$!

wait $sim1
wait $sim2
wait $sim3
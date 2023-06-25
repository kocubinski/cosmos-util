
set -o errexit -o nounset -o pipefail

# Reset
Color_Off='\033[0m'       # Text Reset

# Regular Colors
Black='\033[0;30m'        # Black
Red='\033[0;31m'          # Red
Green='\033[0;32m'        # Green
Yellow='\033[0;33m'       # Yellow
Blue='\033[0;34m'         # Blue
Purple='\033[0;35m'       # Purple
Cyan='\033[0;36m'         # Cyan
White='\033[0;37m'        # White

NODENAME='mtkoan-petals'
OSMOHOME=$HOME/.osmosisd
CHAINID='osmosis-1'
PRIME=73

function colorprint {
    echo ${Green}$1${Color_Off}
}

function client_init {
    osmosisd init $NODENAME --chain-id $CHAINID --home $OSMOHOME
    sed -i -E 's/chain-id = ""/chain-id = "osmosis-1"/g' $OSMOHOME/config/client.toml
    sed -i -E 's|node = "tcp://localhost:26657"|node = "http://osmosis.artifact-staking.io:26657"|g' \
        $OSMOHOME/config/client.toml
}
    
function full_init {
    rm -f $OSMOHOME/config/client.toml
    rm -f $OSMOHOME/config/app.toml
    rm -f $OSMOHOME/config/addrbook.json
    osmosisd init $NODENAME --chain-id=$CHAINID -o --home $OSMOHOME
    colorprint "Downloading and Replacing Genesis..."
    wget -O $OSMOHOME/config/genesis.json \
        https://github.com/osmosis-labs/networks/raw/main/osmosis-1/genesis.json
    colorprint "Downloading and Replacing Addressbook..."
    wget -O $OSMOHOME/config/addrbook.json \
        https://quicksync.io/addrbook.osmosis.json
}

# Pruning: Keep last 10,000 states and prune at a random prime block interval
# If not selected then default pruning will be used:
# keep last 100,000 states to query the last week worth of data and prune at 
# 100 block intervals
function custom_pruning {
    colorprint "Customizing Pruning Settings"
    sed -i '' -E 's/pruning = "default"/pruning = "custom"/g' $OSMOHOME/config/app.toml
    sed -i '' -E 's/pruning-keep-recent = "0"/pruning-keep-recent = "10000"/g' $OSMOHOME/config/app.toml
    sed -i '' 's/pruning-interval = "0"/pruning-interval = "73"/g' $OSMOHOME/config/app.toml 
}

# Note decompression requirements:
# sudo apt-get install jq wget liblz4-tool aria2 -y
# brew install wget lz4 aria2 jq
function data_sync_from_snapshots {
    # default snapshot
    FILENAME="osmosis-1-pruned"
    # other options
    # FILENAME="osmosis-1-default"
    # FILENAME="osmosis-1-archive"

    colorprint "Downloading Snapshot..."
    curl -L https://quicksync.io/osmosis.json \
     | jq -r '.[] |select(.file=="'$FILENAME'")|select (.mirror=="Netherlands")|.url' \
     | wget -O - -i - | lz4 -d | tar -xvf - -C $OSMOHOME
}

function data_sync_from_rocksdb {
    colorprint "TODO" 
    # see replayFromGenesisRocksDb
 }

# first `make install` osmosis to $PATH (probably $GOPATH/bin) 
# then run this script

rm -rf $OSMOHOME && mkdir -p $OSMOHOME
full_init
custom_pruning
# instead of below;
# can also try starting a node with statesync from the ./scripts/statesync.bash script in osmosis
# data_sync_from_snapshots

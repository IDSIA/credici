#!/bin/bash

function read_param () {
    local filename="$1"
    local cname="$2"
    
    local col=$(head -1 "$filename" | tr ',' \\n | grep -n "$cname" | cut -d':' -f1)
    ret="$(cut -d',' -f${col} "$filename" | tail -1)"
}

csv="$1"
modelspath="$2"

read_param "$csv" "cause"
cause="$ret"
read_param "$csv" "effect"
effect="$ret"
read_param "$csv" "pns_l"
pnsl="$ret"
read_param "$csv" "pns_u"
pnsu="$ret"
read_param "$csv" "modelPath"
model="$ret"


model=${model##*/}
model=${model%.uai*}


echo -n "$cause,$effect,$pnsl,$pnsu,$model,"

$JAVA_HOME/bin/java -cp credici.jar ch.idsia.credici.utility.apps.PNS \
    -c          \
    -r 200      \
    -i 500     \
    -t 16       \
    -f "${modelspath}/${model}.uai" \
    --ace ace/compile \
    pns $cause $effect

#!/bin/bash

which gtime
if [[ "$?" -eq 1 ]]; then 
    echo setting gtime
    alias gtime=/usr/bin/time 
else 
    echo gtime present
fi

# read some params from a previous exection. 
# Looks for the offset of the column in the first line, 
# then return its value
function read_param () {
    local filename="$1"
    local cname="$2"
    
    local col=$(head -1 "$filename" | tr ',' \\n | grep -n "$cname" | cut -d':' -f1)
    ret="$(cut -d',' -f${col} "$filename" | tail -1)"
}

# read the parameters from command line
csv="$1"
modelspath="$2"
uuid="$3"
output="$4"

# get info about stuff
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

# NOTE: data file is obtained replacing the uai extension with csv
gtime -f '%M,%c,%F,%e,%S,%U,%P' -o "timeings_$UUID.csv" $JAVA_HOME/bin/java -cp credici.jar ch.idsia.credici.utility.apps.PNS \
    -c          \
    -r 200      \
    -i 500     \
    -t 16       \
    -o "results_$UUID.csv" \
    -f "${modelspath}/${model}.uai" \
    --ace ace/compile \
    pns $cause $effect


# concatenate the two files
{ tail -1 "results_$UUID.csv" ; tail -1 "timeings_$UUID.csv" ; } | paste -s -d, - >> $output


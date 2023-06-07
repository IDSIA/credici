#!/bin/bash

which gtime
if [[ "$?" -eq 1 ]]; then 
    echo setting gtime
    TIME_APP=/usr/bin/time 
else 
    TIME_APP=$(which gtime)
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
UUID="$3"
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



FIRST=1

IFS="$(echo -en "\n\r")" 
for settings in $(cat experiment-flags.txt ); do 
    echo running $settings

    rm -f "timeings_$UUID.csv"
    rm -f "results_$UUID.csv" 
    
    # NOTE: data file is obtained replacing the uai extension with csv
    $TIME_APP -f '%M,%c,%F,%e,%S,%U,%P' -o "timeings_$UUID.csv" $JAVA_HOME/bin/java -cp credici.jar ch.idsia.credici.utility.apps.PNS \
        $line \
        -o "results_$UUID.csv" \
        -f "${modelspath}/${model}.uai" \
        --ace ace/compile \
        pns $cause $effect
    echo "done"

    # first experiment? add header to output
    if [[ "$FIRST" -eq "1" ]]; then
        echo header
        { echo -n "old_pns_l,old_pns_u," ; \
          head -1 "results_$UUID.csv" ; \
          echo "memory,cntx-switch,page-faults,wall-clock,system-time,user-time,cpu" ; \
        } | paste -s -d, - > $output
        FIRST=0
    fi

    # concatenate the two files
    { echo -n "$pnsl,$pnsu," ; tail -1 "results_$UUID.csv" ; tail -1 "timeings_$UUID.csv" ; } | paste -s -d, - >> $output
done

# cleanup
rm -f "timeings_$UUID.csv"
rm -f "results_$UUID.csv" 
    
unset IFS

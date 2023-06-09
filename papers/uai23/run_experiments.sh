#!/bin/bash

which gtime
if [[ "$?" -eq 1 ]]; then 
    echo setting gtime
    TIME_APP=/usr/bin/time 
else 
    TIME_APP=$(which gtime)
fi


# read the parameters from command line
modellist="$1"
experiments="$2"
output="$3"
maxtime="$4"

UUID=$(uuidgen)


FIRST=1

IFS="$(echo -en "\n\r")" 
for model in $(cat "$modellist"); do

    IFS="$(echo -en "\n\r")" 
    for settings in $(cat "$experiments"); do 
        echo running $settings

        rm -f "timeings_$UUID.csv"
        rm -f "results_$UUID.csv" 
        
        # NOTE: data file is obtained replacing the uai extension with csv
        unset IFS
        $TIME_APP -f '%M,%c,%F,%e,%S,%U,%P' -o "timeings_$UUID.csv" timeout $maxtime $JAVA_HOME/bin/java -cp credici.jar ch.idsia.credici.utility.apps.PNS \
            $(echo "$settings") \
            -o "results_$UUID.csv" \
            -f "${model}" \
            --ace ace/compile \
            --header \
            learn 0 0
        echo "done"
        touch "results_$UUID.csv"

        # first experiment? add header to output
        if [[ "$FIRST" -eq "1" ]]; then
            echo header
            { \
                echo "memory,cntx-switch,page-faults,wall-clock,system-time,user-time,cpu" ; \
                cat "results_$UUID.csv" | head -1  ; \
            } | paste -s -d, - > $output
            FIRST=0
        fi

        # concatenate the two files
        { \
            tail -1 "timeings_$UUID.csv" ; \
            cat "results_$UUID.csv" | tail -1 ; \
        } | paste -s -d, - >> $output
    done
done
# cleanup
rm -f "timeings_$UUID.csv"
rm -f "results_$UUID.csv" 
    
unset IFS

#!/bin/bash
# 
#

if [ "$1" == "" ]
then
    echo Usage: $0 path to info and queries
    exit
fi

if [ ! -d "$1" ]
then 
    echo \"$1\" is not a folder
    exit
fi


query_header='cause,effect,ace_l,ace_u,pns_l,pns_u'
info_header='topology,avg_exo_card,num_exo_vars,num_endo_vars,markovianity'
folder=$1
echo $info_header,$query_header,identifiable

for path in $folder/*.uai; do
    file=${path##*/}
    base=${path%%.uai}

    # load file contents
    info=$(tail -1 ${base}_info.csv)
    query=$(tail -1 ${base}_queries.csv)
    
    pns_l=$(echo $query | cut -d, -f5 | sed -E 's/([+-]?[0-9.]+)[eE]\+?(-?)([0-9]+)/(\1*10^\2\3)/g')
    pns_u=$(echo $query | cut -d, -f6 | sed -E 's/([+-]?[0-9.]+)[eE]\+?(-?)([0-9]+)/(\1*10^\2\3)/g')

    ident=$(echo "
        define abs(i) {
            if (i < 0) return (-i)
            return (i)
        }
        abs(${pns_u} - ${pns_l}) < 0.000001" | bc)

    echo $info,$query,$ident
done
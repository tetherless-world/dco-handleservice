#!/bin/ksh

exdir="/usr/local/etc/hs/hsj"
outdir="/opt/backups/handles"
admpriv="/usr/share/tomcat6/work/dcohandleservice/admpriv.bin"

if [ "$#" != 0 ]
then
    exdir=$1
    outdir=$2
    admpriv=$3
fi
ex="${exdir}/bin/hdl-list"

if [ ! -x $ex ]
then
    echo "hdl-list app not found in ${exdir}"
    exit 1
fi

if [ "$outdir" = "" -o ! -d $outdir ]
then
    echo "output directory \"${outdir}\" does not exist"
    exit 1
fi

if [ "$admpriv" = "" -o ! -f $admpriv ]
then
    echo "password file \"${admpriv}\" not found"
    exit 1
fi

TMP=${outdir}/tmp.list
FILE=${outdir}/handles.list
pass=${outdir}/pass
out=${outdir}/result
base="http://dx.deepcarbon.net/"
all="no"

> $out

start=$SECONDS
${ex} -s 0.NA/11121 300 ${admpriv} 11121 < $pass > $TMP 2> /dev/null
duration=$(( SECONDS - start ))
echo "hdl-list took $duration seconds" >> $out

cat $TMP | grep "^11121" > $FILE

handle=""
redirect=""
((count=0))
start=$SECONDS
while read CMD; do
	saveme=$CMD
	short=${CMD:0:5}
	if [ "$short" = "11121" ]
	then
		((count+=1))
		ans=$(( $count % 100 ))
		if [ $ans = 0 ]
		then
			echo $count >> $out
			response=`curl -sL -w "%{http_code}\n" "${base}${CMD}" -o /dev/null`
			if [ "$response" != "200" ]
			then
				bad=1
				echo "${saveme}" >> $out
			fi
			sleep 1
		elif [ "$all" = "yes" ]
		then
			response=`curl -sL -w "%{http_code}\n" "${base}${CMD}" -o /dev/null`
			if [ "$response" != "200" ]
			then
				bad=1
				echo "${saveme}" >> $out
			fi
			sleep 1
		fi
	fi
done < "$FILE"
duration=$(( SECONDS - start ))
echo "checking took $duration seconds" >> $out

echo "total number of handles: $count" >> $out

if [ "$bad" = "1" ]
then
	mailx -s "Check: Check of handles FAILED on deepcarbon" westp@rpi.edu < ${out}
else
	mailx -s "Check: Check of handles OK on deepcarbon" westp@rpi.edu < ${out}
fi


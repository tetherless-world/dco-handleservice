#!/bin/sh

FILE=/opt/backups/handles/handles.list
pass=/opt/backups/handles/pass
out=/opt/backups/handles/result
base="http://dx.deepcarbon.net/"
all="yes"

> $out

start=$SECONDS
/usr/local/etc/hs/hsj/bin/hdl-list -s 0.NA/11121 300 /usr/share/tomcat6/work/dcohandleservice/admpriv.bin 11121 < $pass > $FILE 2> /dev/null
duration=$(( SECONDS - start ))
echo "hdl-list took $duration seconds" >> $out

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


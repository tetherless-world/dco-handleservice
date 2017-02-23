#!/bin/sh

FILE=/opt/backups/handles/handles.list
pass=/opt/backups/handles/pass
out=/opt/backups/handles/result
base="http://dx.deepcarbon.net/"
all="yes"

> $out

/usr/local/etc/hs/hsj/bin/hdl-list -s 0.NA/11121 300 /usr/share/tomcat6/work/dcohandleservice/admpriv.bin 11121 < $pass > $FILE 2> /dev/null

while read CMD; do
	handle=$CMD
	short=${CMD:0:5}
	if [ "$short" = "11121" ]
	then
        resolves=`php ./resolveone.php $handle`
        result=$?
        if [ "$result" != "0" ]
        then 
            echo "$handle did not resolve"
        else
            echo "$handle resolves to $resolves"
        fi
	fi
done < "$FILE"


<?php
$cmd="/usr/local/etc/hs/hsj/bin/hdl-list -s 0.NA/11121 300 /usr/share/tomcat6/work/dcohandleservice/admpriv.bin 11121 < /opt/backups/handles/pass 2> /dev/null" ;

$lines=array() ;

exec( $cmd, $lines ) ;

$handle = "" ;
$uri = "" ;

foreach( $lines as $line )
{
    if( 0 === strpos( $line, '11121' ) )
    {
        $handle = trim( $line ) ;
    }
    else if( FALSE !== strpos( $line, "http" ) )
    {
        $pos = strpos( $line, "http:" ) ;
        $uri = substr( $line, $pos, strlen( $line ) - $pos - 1 ) ;
        $newuri = str_replace( "http:", "https:", $uri ) ;
        print( "$handle redirects to $newuri\n" ) ;
    }
}
?>

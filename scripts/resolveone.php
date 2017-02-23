<?php

include_once( "utils.php" ) ;

$url = "http://deepcarbon.tw.rpi.edu:8080/dcohandleservice/services/handles" ;

if( $argc < 2 )
{
    print( "Usage: $argv[0] <list_of_handles>" ) ;
    exit( 1 ) ;
}

$ids = $argv ;
array_shift( $ids ) ;

foreach( $ids as $id )
{
    // retrieve the id and verify that I get the right url
    $cmd = "$url/resolve" ;
    $data = "<handle><id>$id</id><type>URL</type></handle>" ;
    $xml = getXML( $cmd, $data ) ;
    if( $xml === false )
    {
        exit( 1 ) ;
    }
    if( isset( $xml->handle ) )
    {
        $rid = $xml->handle->id ;
        $rval = trim( $xml->handle->value ) ;
        print( "$rval" ) ;
    }
    else if( isset( $xml->id ) )
    {
        $rid = $xml->id ;
        $rval = trim( $xml->value ) ;
        print( "$rval" ) ;
    }
    else
    {
        exit( 1 ) ;
    }
}

exit( 0 ) ;
?>

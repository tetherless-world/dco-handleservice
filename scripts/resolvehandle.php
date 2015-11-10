<?php

include_once( "utils.php" ) ;

$url = "http://deepcarbon.tw.rpi.edu:8080/dcohandleservice/services/handles" ;

if( $argc > 1 )
{
    $ids = $argv ;
    array_shift( $ids ) ;
}
else
{
    $ids = array(
                  "11121/8719-8951-9796-4955-CC"
    ) ;
}

foreach( $ids as $id )
{
    print( "RESOLVING $id\n" ) ;
    // retrieve the id and verify that I get the right url
    $cmd = "$url/resolve" ;
    $data = "<handle><id>$id</id><type>URL</type></handle>" ;
    $xml = getXML( $cmd, $data ) ;
    if( $xml === false )
    {
        print( "  failed to resolve\n" ) ;
        exit( 1 ) ;
    }
    if( isset( $xml->handle ) )
    {
        $rid = $xml->handle->id ;
        $rval = $xml->handle->value ;
        print( "  resolves to $rval\n" ) ;
    }
    else if( isset( $xml->id ) )
    {
        $rid = $xml->id ;
        $rval = $xml->value ;
        print( "  resolves to $rval\n" ) ;
    }
    else
    {
        print( "  did not resolve\n" ) ;
    }
}
?>


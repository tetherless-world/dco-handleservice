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
                  "x1",
                  "x2"
    ) ;
}

// remove the id
foreach( $ids as $id )
{
    print( "REMOVING $id\n" ) ;
    $cmd = "$url/remove" ;
    $data = "<handle><id>$id</id><type>URL</type><value></value></handle>" ;
    $result = getResult( $cmd, $data ) ;
    if( $result === false )
    {
        print( "Failed to remove the dcoid\n" ) ;
        exit( 1 ) ;
    }
    print( "result = $result\n" ) ;
}
?>


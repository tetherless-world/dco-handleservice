<?php

include_once( "utils.php" ) ;

$url = "http://deepcarbon.tw.rpi.edu:8080/dcohandleservice/services/handles" ;

if( $argc != 3 )
{
    print( "USAGE: $argv[0] <dcoid> <uri>\n" ) ;
    exit( 1 ) ;
}

$id = $argv[1] ;
$resolvesto = $argv[2] ;

// update the id
$id = trim( $id ) ;
if( strlen( $id ) > 5 && substr( $id, 0, 5 ) == "11121" )
{
    print( "UPDATING $id\n" ) ;
    $cmd = "$url/update" ;
    $data = "<handle><id>$id</id><type>URL</type><value>$resolvesto</value></handle>" ;
    $result = getResult( $cmd, $data ) ;
    if( $result === false )
    {
        print( "Failed to update the dcoid\n" ) ;
        exit( 1 ) ;
    }
    print( "result = $result\n" ) ;
}
?>


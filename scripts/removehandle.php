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
    if( file_exists( "toremove" ) )
    {
        $ids = file( "toremove" ) ;
    }
    else
    {
        print( "no dco-id specified\n" ) ;
        print( "Usage: $argv[0] [<handle1> [<handle2]]\n" ) ;
        print( "    if no handles specified then loads file \"toremove\"\n" ) ;
        exit( 1 ) ;
    }
}

// remove the id
foreach( $ids as $id )
{
    $id = trim( $id ) ;
    if( strlen( $id ) > 5 && substr( $id, 0, 5 ) == "11121" )
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
}
?>


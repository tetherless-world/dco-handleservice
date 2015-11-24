<?php

include_once( "utils.php" ) ;

$url = "http://localhost:8080/dcohandleservice/services/handles" ;

// create a new dcoid, point it to a dummy test page so I can compare,
print( "CREATING\n" ) ;
$cmd = "$url/create" ;
$data = "<handle><id></id><type>URL</type><value>http://tw.rpi.edu</value></handle>" ;
$xml = getXML( $cmd, $data ) ;
if( $xml === false )
{
    print( "Failed to create the dcoid\n" ) ;
    exit( 1 ) ;
}
if( isset( $xml->handle ) )
{
    $id = $xml->handle->id ;
}
else
{
    $id = $xml->id ;
}
if( $id === false || $id == "" )
{
    print( "Unable to retrieve dcoid from result\n" ) ;
    exit( 1 ) ;
}
$id = substr( $id, 25 ) ;
print( "id = $id\n" ) ;

print( "RESOLVING\n" ) ;
// retrieve the id and verify that I get the right url
$cmd = "$url/resolve" ;
$data = "<handle><id>$id</id><type>URL</type></handle>" ;
$xml = getXML( $cmd, $data ) ;
if( $xml === false )
{
    print( "Failed to retrieve the dcoid\n" ) ;
    exit( 1 ) ;
}
if( isset( $xml->handle ) )
{
    $rid = $xml->handle->id ;
    $rval = $xml->handle->value ;
}
else
{
    $rid = $xml->id ;
    $rval = $xml->value ;
}

// make sure we got back what we're expecting
$badthings = false ;
if( $rid != $id )
{
    print( "id $rid not equal to expeted value $id\n" ) ;
    $badthings = true ;
}
if( $rval != "http://tw.rpi.edu" )
{
    print( "vaue $rval not equal to expected value http://tw.rpi.edu\n" ) ;
    $badthings = true ;
}

// remove the id
print( "REMOVING\n" ) ;
$cmd = "$url/remove" ;
$data = "<handle><id>$id</id><type>URL</type><value></value></handle>" ;
$result = getResult( $cmd, $data ) ;
if( $result === false )
{
    print( "Failed to remove the dcoid\n" ) ;
    exit( 1 ) ;
}
print( "result = $result\n" ) ;
if( $result != "done" )
{
    print( "result of removing is $result, expected \"done\"\n" ) ;
    $badthings = true ;
}

if( $badthings == true )
{
    exit( 1 ) ;
}
exit( 0 ) ;
?>


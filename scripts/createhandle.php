<?php

include_once( "utils.php" ) ;

if( $argc != 2 )
{
    print( "USAGE: $argv[0] <uri>\n" ) ;
    exit( 1 ) ;
}

$resolvesto = $argv[1] ;

$url = "http://locahost:8080/dcohandleservice/services/handles" ;

print( "CREATING\n" ) ;
$cmd = "$url/create" ;
$data = "<handle><id></id><type>URL</type><value>$resolvesto</value></handle>" ;
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

$fullid = $id ;
print( "full id = $fullid\n" ) ;
$id = substr( $fullid, 25 ) ;
print( "id = $id\n" ) ;

print( "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." ) ;
print( "@prefix dco: <http://info.deepcarbon.net/schema#> .\n" ) ;
print( "\n" ) ;
print( "<$fullid> a dco:DCOID ;\n" ) ;
print( "  rdfs:label \"$id\" ;\n" ) ;
print( "  dco:dcoIdFor <$resolvesto> .\n" ) ;
print( "\n" ) ;
print( "<$resolvesto> dco:hasDcoId <$fullid> .\n" ) ;
exit( 0 ) ;
?>


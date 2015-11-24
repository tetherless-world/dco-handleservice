<?php

function getXML( $url, $data )
{
    $xml = null ;

    $result = getResult( $url, $data ) ;
    if( $result !== false )
    {
        $xml = simplexml_load_string( $result ) ;
    }

    return $xml ;
}

function getResult( $url, $data )
{
    $restult = null ;

    $data_len = strlen( $data ) ;
    // use key 'http' even if you send the request to https://...
    $options = array(
        'http' => array(
            'header'  => "Content-type: application/xml\r\nContent-Language: en-US\r\nContent-Length:$data_len\r\n",
            'method'  => 'POST',
            'content' => $data,
        ),
    );
            #'content' => http_build_query($data),

    $context  = stream_context_create( $options ) ;
    if( $context !== false )
    {
        $result = file_get_contents( $url, false, $context ) ;
    }

    return $result ;
}
?>


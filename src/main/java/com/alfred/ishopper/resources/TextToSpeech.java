package com.alfred.ishopper.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import org.apache.commons.codec.binary.Base64;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.nio.charset.Charset;

@Path("/text-to-speech")
@Produces(MediaType.MULTIPART_FORM_DATA)
public class TextToSpeech {
    private String username = "8da0bca8-c9d8-41e8-978b-b74febe37517";
    private String password = "lFKYnN9Z30Fu";
    private String url = "https://stream.watsonplatform.net/text-to-speech/api/v1/synthesize?text=Hello";

    @GET
    @Timed
    public String sayHello() {
        /*RestTemplate template = new RestTemplate();
        String plainCreds = username+":"+password;
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);

        //template.exchange("https://stream.watsonplatform.net/text-to-speech/api/v1/synthesize?text=Hello", HttpMethod.POST, new HttpEntity<String>(createHeaders(this.username,this.password)));

        // Create the request body as a MultiValueMap
        MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
        body.add("field", "value");
        // Note the body object as first parameter!
        HttpEntity<?> httpEntity = new HttpEntity<Object>(body, headers);

        //template.getForObject()
        HttpEntity<String> request = new HttpEntity<String>(headers);
        ResponseEntity<Object> response = template.exchange(url, HttpMethod.GET, request, Object.class);

        System.out.println(response);
        */
        return "What up!!!";
    }

    /*public HttpHeaders createHeaders(final String username, final String password ){
        return new HttpHeaders(){
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(Charset.forName("US-ASCII")) );
                String authHeader = "Basic " + new String( encodedAuth );
                set( "Authorization", authHeader );
            }
        };
    }*/
}

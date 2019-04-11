package com.nullendpoint;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SoapToJsonUnitTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    ApplicationContext applicationContext;

    //TODO: There is no activemq (java amq6) context, add one programmatically

    //TODO: add the test application.properties to point to this activemq (vm://localhost)

    @Before
    public void addJmsToMockRoute() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:hello").to("mock:someMock");
            }
        });
    }

    @Test
    public void sayHelloTest() throws InterruptedException {

        //Get the mock endpoint and set expectations
        MockEndpoint mockEndpoint = (MockEndpoint) camelContext.getEndpoint("mock://someMock");
        mockEndpoint.setExpectedMessageCount(1);

        // Call the REST APIs
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set("SOAPAction", "http://www.example.org/SimpleService/NewOperation");

        //TODO: Load the SOAP request content from file instead of this

        HttpEntity<String> entity = new HttpEntity<String>("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sim=\"http://www.example.org/SimpleService/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <sim:NewOperation>\n" +
                "         <FirstName>Camel</FirstName>\n" +
                "         <LastName>Fuse</LastName>\n" +
                "         <Other>LUSH</Other>\n" +
                "      </sim:NewOperation>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>", headers);

        //HttpEntity
        ResponseEntity<String> response = restTemplate.postForEntity("/services/SomeService", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String s = response.getBody();
        Thread.sleep(1000);
        mockEndpoint.assertIsSatisfied();
        //assertThat(s.equals("Hello World"));

        //TODO: compare soap response and expected soap response with XMLUnit

        //TODO: compare json content matches
    }

}

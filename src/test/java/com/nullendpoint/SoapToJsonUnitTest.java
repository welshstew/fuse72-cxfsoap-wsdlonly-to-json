package com.nullendpoint;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-${environment}.properties")
@ActiveProfiles("${environment}")
public class SoapToJsonUnitTest {
	
	//@Profile("${environment}")
	@Configuration
    @Import(Application.class) // override the ems configuration with local // activemq "vm://" for unit testing 
	public static class TestConfig {
		@Profile("local")
		@Bean
	    JmsComponent jms(){
	    	ActiveMQConnectionFactory fac = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
	        JmsComponent jmsComponent = new JmsComponent();
	        jmsComponent.setConnectionFactory(fac);
	        return jmsComponent;
	    }
    }
	
	//private static BrokerService broker = new BrokerService();

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
                from("jms:{{queue.name}}").to("mock:someMock");
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
        File file;
        String soapRequest = null;
		try {
			file = ResourceUtils.getFile("classpath:soap-request.xml");
			soapRequest = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Test SOAP Request:\n " + soapRequest);
		
        String soapReponse = null;
		try {
			file = ResourceUtils.getFile("classpath:expected-soap-response.xml");
			soapReponse = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        
        HttpEntity<String> entity = new HttpEntity<String>(soapRequest, headers);

        //HttpEntity
        ResponseEntity<String> response = restTemplate.postForEntity("/services/SomeService", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseString = response.getBody();
        Thread.sleep(1000);
        
		System.out.println("Test SOAP Response:\n " + soapReponse);
        
        //TODO: compare soap response and expected soap response with XMLUnit
        Diff diff = DiffBuilder.compare(soapReponse).withTest(responseString).ignoreComments().ignoreWhitespace().checkForSimilar().build();
        assertThat(diff.hasDifferences()==false);
	
       
        //TODO: compare json content matches using Gson
        String expectedJsonContext = null;
		try {
			file = ResourceUtils.getFile("classpath:expected-json-context.json");
			expectedJsonContext = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String jsonRequest = (String)mockEndpoint.getExchanges().get(0).getIn().getBody();
		
		System.out.println("Test JSON message from queue:\n " + jsonRequest);

		
        JsonParser parser = new JsonParser();
        JsonElement o1 = parser.parse(expectedJsonContext);
        JsonElement o2 = parser.parse(jsonRequest);
        assertThat(o1.equals(o2));
        
        mockEndpoint.assertIsSatisfied();
    }
/*    
    @After
    public void shutDownBroker() throws Exception {
        broker.stop();
    }*/


}

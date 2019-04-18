package com.nullendpoint;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("${environment}")
@TestPropertySource("classpath:application-${environment}.yml")
public class SoapToJsonUnitTest {
	
	@Value("${security.require-ssl}")
	private String requireSSL;
	
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
    private CamelContext camelContext;    
    
    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void addJmsToMockRoute() throws Exception {
    	
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("{{artemis.destination}}").to("mock:someMock");
            }
        });
        
    }
    
    //RestTemplate that trusts certificates for https
    @Bean
    private RestTemplate getRestTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                return true;
            }
        };
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
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

        File file;
        String soapRequest = null;
		try {
			file = ResourceUtils.getFile("classpath:soap-request.xml");
			soapRequest = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Test SOAP Request:\n " + soapRequest);
				
        String soapReponse = null;
		try {
			file = ResourceUtils.getFile("classpath:expected-soap-response.xml");
			soapReponse = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}        
        
        HttpEntity<String> entity = new HttpEntity<String>(soapRequest, headers);

        //HttpEntity
        RestTemplate restTemplate=null;
		try {
			restTemplate = getRestTemplate();
		} catch (KeyManagementException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (KeyStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        ResponseEntity<String> response = restTemplate.postForEntity("https://localhost:8080/services/SomeService", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseString = response.getBody();
        Thread.sleep(1000);
        
		System.out.println("Test SOAP Response:\n " + soapReponse);
        
        //compare soap response and expected soap response with XMLUnit
        Diff diff = DiffBuilder.compare(soapReponse).withTest(responseString).ignoreComments().ignoreWhitespace().checkForSimilar().build();
        assertThat(diff.hasDifferences()==false);
	
       
        //compare json content matches using Gson
        String expectedJsonContext = null;
		try {
			file = ResourceUtils.getFile("classpath:expected-json-content.json");
			expectedJsonContext = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
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

}

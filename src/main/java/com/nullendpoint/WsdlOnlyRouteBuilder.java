package com.nullendpoint;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "cxf.webservice")
public class WsdlOnlyRouteBuilder extends RouteBuilder {

    @Value("${cxf.webservice.name}")
    private String webserviceName;

    //The service name this service is implementing, it maps to the wsdl:service@name.
    // Example: {http://org.apache.camel}ServiceName
    @Value("${cxf.webservice.serviceName}")
    private String webserviceServiceName;

    //The port name this service is implementing, it maps to the wsdl:port@name.
    // Example: {http://org.apache.camel}PortName
    @Value("${cxf.webservice.endpointName}")
    private String webserviceEndpointName;

    //The location of the WSDL. It is obtained from endpoint address by default.
    //Example: file://local/wsdl/hello.wsdl or wsdl/hello.wsdl
    @Value("${cxf.webservice.wsdlUrl}")
    private String webserviceWsdlUrl;

    //xpath to content that is required to be extracted to JSON
    @Value("${cxf.webservice.xpath}")
    private String webserviceXpath;

    //camel simple response
    //Example: resource:classpath:static/response.xml
    @Value("${cxf.webservice.simpleResponse}")
    private String webserviceSimpleResponse;

    private HashMap<String, String> namespaces;

    public HashMap<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(HashMap<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public void configure() throws Exception {

        Namespaces ns = null;

        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if(ns == null){
                ns = new Namespaces(entry.getKey(),entry.getValue());
            }else{
                ns.add(entry.getKey(),entry.getValue());
            }
        }

        from("cxf://" + webserviceName + "?wsdlURL=" + webserviceWsdlUrl + "&dataFormat=RAW&serviceName=" + webserviceServiceName + "&endpointName=" + webserviceEndpointName).routeId("cxfRoute")
                .setBody(ns.xpath(webserviceXpath))
                .to("xslt:xsl/remove-namespaces.xsl")
                .log("XML body is ${body}")
                .convertBodyTo(String.class)
                .process(new Processor() {

                    String rootNode = webserviceXpath.substring(webserviceXpath.lastIndexOf(':')+1);

                    @Override
                    public void process(Exchange exchange) throws Exception {
                        int textIndent = 2;
                        JSONObject xmlJSONObj = XML.toJSONObject((String)exchange.getIn().getBody());
                        exchange.getIn().setBody(xmlJSONObj.getJSONObject(rootNode).toString(textIndent));
                    }
                })
                .log("Body to go to AMQ is ${body}")
                .to("direct:sendToJms")
                .setBody(simple(webserviceSimpleResponse))
                .setHeader("Content-Type", constant("application/xml"))
                .log("Body returned to WS-Client is ${body}");

        from("direct:sendToJms").routeId("jmsSend")
                .setExchangePattern(ExchangePattern.InOnly)
                .removeHeaders("*", "breadcrumbId")
                .to("{{artemis.destination}}");

    }
}

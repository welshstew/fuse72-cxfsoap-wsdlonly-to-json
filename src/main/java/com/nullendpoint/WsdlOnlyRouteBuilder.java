package com.nullendpoint;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.dataformat.xmljson.XmlJsonDataFormat;
import org.springframework.stereotype.Component;

@Component
public class WsdlOnlyRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        XmlJsonDataFormat xmlJsonFormat = new XmlJsonDataFormat();
        xmlJsonFormat.setEncoding("UTF-8");
        xmlJsonFormat.setForceTopLevelObject(false);
        xmlJsonFormat.setTrimSpaces(true);
        xmlJsonFormat.setSkipNamespaces(true);
        xmlJsonFormat.setRemoveNamespacePrefixes(true);

        Namespaces ns = new Namespaces("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        ns.add("sim", "http://www.example.org/SimpleService/");

        from("cxf://SomeService?wsdlURL=wsdl/simpleService.wsdl&dataFormat=RAW").routeId("cxfRoute")
                .setBody(ns.xpath("//soap:Body/sim:NewOperation"))
                .log("XML body is ${body}")
                .marshal(xmlJsonFormat)
                .convertBodyTo(String.class)
                .log("Body to go to AMQ is ${body}")
                .to("direct:sendToJms")
                .setBody(simple("resource:classpath:static/response.xml"))
                .setHeader("Content-Type", constant("application/xml"))
                .log("Body returned to WS-Client is ${body}");

        from("direct:sendToJms").routeId("jmsSend")
                .setExchangePattern(ExchangePattern.InOnly)
                .removeHeaders("*", "breadcrumbId")
                .to("{{artemis.destination}}");

//        from("{{artemis.destination}}").to("log:hello?showAll=true");

        //TODO: make the jms destination configurable via properties

    }
}

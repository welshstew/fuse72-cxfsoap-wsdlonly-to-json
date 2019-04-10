# cxf-soap-wsdlonly-to-json

This is an example use-case where a webservice is required.

No JAXB Marshalling, just plain soap body to json.  All in one neat camel route.

```xml
<route id="cxfRoute">
    <from uri="cxf://SomeService?wsdlURL=wsdl/simpleService.wsdl&amp;dataFormat=RAW"/>
    <setBody>
        <xpath>//soap:Body/sim:NewOperation</xpath>
    </setBody>
    <log message="XML body is ${body}"/>
    <marshal ref="xmljsonWithOptions" />
    <log message="Body to go to AMQ is ${body}"/>
    <!-- send to AMQ here -->

    <!-- construct a response to the WS Client -->
    <setBody>
        <simple>resource:classpath:static/response.xml</simple>
    </setBody>
    <setHeader headerName="Content-Type">
        <constant>application/xml</constant>
    </setHeader>
    <log message="Body returned to WS-Client is ${body}"/>
</route>
```

```text
mvn clean spring-boot:run
siege --rc=./siegerc
```



## Further Documentation

- https://access.redhat.com/documentation/en-us/red_hat_fuse/7.2/
- https://access.redhat.com/documentation/en-us/red_hat_fuse/7.2/html/deploying_into_spring_boot/index


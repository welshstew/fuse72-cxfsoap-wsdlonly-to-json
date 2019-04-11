/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.nullendpoint;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.apache.camel.Endpoint;
import org.apache.camel.component.jms.JmsComponent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import java.util.HashMap;

/**
 * The Spring-boot main class.
 */
@SpringBootApplication
//@ImportResource({"classpath:spring/camel-context.xml"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ActiveMQJMSConnectionFactory artemisConnectionFactory(ArtemisJmsConfiguration config){
        return new ActiveMQJMSConnectionFactory(config.getUrl(), config.getUsername(), config.getPassword());
    }

    @Bean
    JmsComponent jms(ActiveMQJMSConnectionFactory artemisConnectionFactory){
        JmsComponent jmsComponent = new JmsComponent();
        jmsComponent.setConnectionFactory(artemisConnectionFactory);
        return jmsComponent;
    }

}

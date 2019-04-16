package com.nullendpoint;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "artemis")
public class ArtemisJmsConfiguration {


    private String url;

    /**
     * AMQ username
     */
    private String username;

    /**
     * AMQ password
     */
    private String password;

    private boolean useAnonymousProducers = false;
    private Integer maxConnections = 5;

    public ArtemisJmsConfiguration() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseAnonymousProducers() {
        return useAnonymousProducers;
    }

    public void setUseAnonymousProducers(boolean useAnonymousProducers) {
        this.useAnonymousProducers = useAnonymousProducers;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }
}
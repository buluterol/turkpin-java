package io.github.buluterol.turkpin.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under the {@code turkpin.} prefix. */
@ConfigurationProperties(prefix = "turkpin")
public class TurkpinProperties {
    /** Turkpin dealer account username. */
    private String username;
    /** Turkpin dealer account password. */
    private String password;
    /** Use the sandbox (turkpin.net) instead of the live system. */
    private boolean test;
    /** Overrides the endpoint URL. */
    private String url;
    /** HTTP timeout in seconds. */
    private int timeout = 30;

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

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}

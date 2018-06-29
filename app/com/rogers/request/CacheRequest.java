package com.rogers.request;

import play.data.validation.Constraints;

import javax.validation.groups.Default;
import java.util.Map;

public class CacheRequest {

    @Constraints.Required(message = "database is required", groups = {Default.class, DatabaseRequest.class, KeyRequest.class})
    private String database;
    @Constraints.Required(message = "key is required", groups = {Default.class, KeyRequest.class,MultiInsertRequest.class,MultiDbRequest.class})
    private String key;
    @Constraints.Required(message = "value is required",groups = {Default.class,MultiInsertRequest.class})
    private Map<String, Object> value;
    @Constraints.Required(message = "expire is required",groups = {Default.class,MultiInsertRequest.class})
    private Boolean expire;
    private Integer ttl;
    @Constraints.Required(message = "pattern is required", groups = {DatabaseRequest.class})
    private String pattern;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Object> getValue() {
        return value;
    }

    public void setValue(Map<String, Object> value) {
        this.value = value;
    }

    public Boolean getExpire() {
        return expire;
    }

    public void setExpire(Boolean expire) {
        this.expire = expire;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public CacheRequest() {
        super();
    }

    public CacheRequest(@Constraints.Required(message = "database is required", groups = {Default.class, DatabaseRequest.class}) String database, @Constraints.Required(message = "key is required") String key, @Constraints.Required(message = "value is required") Map<String, Object> value, @Constraints.Required(message = "expire is required") Boolean expire, Integer ttl, @Constraints.Required(message = "pattern is required", groups = {DatabaseRequest.class}) String pattern) {
        this.database = database;
        this.key = key;
        this.value = value;
        this.expire = expire;
        this.ttl = ttl;
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return "CacheRequest{" +
                "database='" + database + '\'' +
                ", key='" + key + '\'' +
                ", value=" + value +
                ", expire=" + expire +
                ", ttl=" + ttl +
                ", pattern='" + pattern + '\'' +
                '}';
    }
}



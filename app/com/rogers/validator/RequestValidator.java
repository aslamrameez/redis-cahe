package com.rogers.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rogers.request.CacheRequest;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Http;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RequestValidator {

    private final FormFactory formFactory;
    private final MessagesApi messagesApi;

    @Inject
    public RequestValidator(FormFactory formFactory, MessagesApi messagesApi) {
        this.formFactory = formFactory;
        this.messagesApi = messagesApi;
    }

    public static CacheRequest insertRequestValidator(Http.Request request) throws Exception {

        JsonNode jsonNode = request.body().asJson();
        ObjectMapper objectMapper = new ObjectMapper();
        CacheRequest cacheRequest = objectMapper.convertValue(jsonNode, CacheRequest.class);
        if (cacheRequest.getExpire()) {
            if (cacheRequest.getTtl() == null) throw new Exception("ttl is required");
            else if (cacheRequest.getTtl() <= 0) throw new Exception("ttl must be greater than 0");;
        } else {
            cacheRequest.setTtl(1440);
        }
        return cacheRequest;

    }
}

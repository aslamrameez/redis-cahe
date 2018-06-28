package validators;

import requestHandlers.CacheRequest;
import responses.ResponseMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.mvc.Http;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RequestValidator {

    private FormFactory formFactory;

    @Inject
    public RequestValidator(FormFactory formFactory) {
        this.formFactory = formFactory;
    }

    public Object requestValidator(Http.Request request, Class clazz) {

        Form<CacheRequest> form = formFactory.form(CacheRequest.class, clazz).bindFromRequest(request);
        for (ValidationError error : form.allErrors()) {
            if (error.message().equals("error.invalid"))
                return new ResponseMessage(400,error.key() + " -> Type Mismatch");
            return new ResponseMessage(400,error.message());
        }

        return form.get();
    }

    public Object insertRequestValidator(Http.Request request, Class clazz) {

        Object object = requestValidator(request,clazz);
        if(object instanceof ResponseMessage){
            return object;
        }
        JsonNode jsonNode = request.body().asJson();
        ObjectMapper objectMapper = new ObjectMapper();
        CacheRequest cacheRequest = objectMapper.convertValue(jsonNode, CacheRequest.class);
        if (cacheRequest.getExpire()) {
            if (cacheRequest.getTtl() == null) return new ResponseMessage(400,"ttl is required");
            else if (cacheRequest.getTtl() <= 0) return new ResponseMessage(400,"ttl must be greater than 0");
        } else {
            cacheRequest.setTtl(1440);
        }
        return cacheRequest;

    }
}

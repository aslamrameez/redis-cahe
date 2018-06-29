package com.rogers.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rogers.constants.RogersConstants;
import com.rogers.request.CacheRequest;
import com.rogers.response.ResponseMessage;
import com.rogers.validator.Annotations.QueryParamsValidator;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ValidatorAnnotationGetAction extends Action<QueryParamsValidator> {

    private FormFactory formFactory;
    private MessagesApi messagesApi;

    @Inject
    public ValidatorAnnotationGetAction(FormFactory formFactory, MessagesApi messagesApi) {
        this.formFactory = formFactory;
        this.messagesApi = messagesApi;
    }

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {

        Class clazz = configuration.value();
        Form<CacheRequest> form = formFactory.form(CacheRequest.class, clazz).bindFromRequest(ctx.request().queryString());
        for (ValidationError error : form.allErrors()) {
            ResponseMessage responseMessage = new ResponseMessage(400,messagesApi.get(Lang.defaultLang(),error.message(),error.key()));
            JsonNode jsonNode = new ObjectMapper().convertValue(responseMessage,JsonNode.class);
            return CompletableFuture.completedFuture(badRequest(jsonNode));
        }

        CacheRequest cacheRequest = form.get();
        ctx.args.put(RogersConstants.REQUEST_OBJECT,cacheRequest);

        return delegate.call(ctx);
    }
}

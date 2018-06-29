package com.rogers.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rogers.constants.RogersConstants;
import com.rogers.request.CacheRequest;
import com.rogers.request.KeyRequest;
import com.rogers.response.ResponseMessage;
import com.rogers.service.CacheService;
import com.rogers.validator.Annotations.PostBodyValidator;
import com.rogers.validator.Annotations.QueryParamsValidator;
import play.api.cache.redis.CacheAsyncApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Option;
import scala.compat.java8.FutureConverters;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static com.rogers.validator.RequestValidator.insertRequestValidator;

@Singleton
public class KeyController extends Controller {

    private CacheService cacheService;

    @Inject
    public KeyController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @PostBodyValidator()
    public CompletionStage<Result> insertKey() {

        final CompletionStage<Object> completionStage;
        final CacheRequest cacheRequest;

        try {
            cacheRequest = insertRequestValidator(request());
        } catch (Exception e) {
            ResponseMessage responseMessage = new ResponseMessage(400,e.getMessage());
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(),Json.toJson(responseMessage)));
        }

        completionStage = cacheService.getKeyDatabase(cacheRequest.getDatabase(), cacheRequest.getKey());

        return completionStage.thenCompose(completionStage1->{
           if(completionStage1 instanceof ResponseMessage){
               ResponseMessage responseMessage = (ResponseMessage)completionStage1;
               return CompletableFuture.completedFuture(status(responseMessage.getStatus(),Json.toJson(responseMessage)));
           }
           if(completionStage1 instanceof CacheAsyncApi) {
               return FutureConverters.toJava(((CacheAsyncApi) completionStage1).set(cacheRequest.getKey(), cacheRequest.getValue(), Duration.create(cacheRequest.getTtl(), TimeUnit.MINUTES)))
               .thenApply((value)->{
                   ResponseMessage responseMessage;
                   responseMessage = new ResponseMessage(200, "Inserted (" + cacheRequest.getKey() + "," + cacheRequest.getValue() + ") Successfully");
                   return status(responseMessage.getStatus(), Json.toJson(responseMessage));
               });

           }
           return CompletableFuture.completedFuture(status(400));
       });
    }

    @QueryParamsValidator(KeyRequest.class)
    public CompletionStage<Result> retrieveValue() {

        final CompletionStage<Object> completionStage;
        final CacheRequest cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);

        completionStage = cacheService.getKeyDatabase(cacheRequest.getDatabase(),cacheRequest.getKey());

        return completionStage.thenCompose(completionStage1->{
            if(completionStage1 instanceof CacheAsyncApi) {
               return FutureConverters.toJava(((CacheAsyncApi)completionStage1).get(cacheRequest.getKey(), scala.reflect.ClassTag$.MODULE$.apply(HashMap.class)))
                        .thenApply((value)->{
                            ObjectNode jsonResponse = Json.newObject();
                            jsonResponse.put(RogersConstants.RESPONSE_STATUS,200);
                            jsonResponse.put(RogersConstants.RESPONSE_MESSAGE,"Value Stored at " + cacheRequest.getKey() + " is " + ((Option) value).get().toString());
                            jsonResponse.putPOJO(RogersConstants.RESPONSE_VALUE,((Option) value).get());
                            return ok(jsonResponse);
                        }).exceptionally((throwable) -> {
                            ResponseMessage responseMessage;
                            responseMessage = new ResponseMessage(200, cacheRequest.getKey() + " doesn't existsKey. Please try with a different key..!!! ");
                            return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                        });
            } else {
                ResponseMessage responseMessage = (ResponseMessage) completionStage1;
                return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
            }
        });

    } // Haven't used ResponseMessage

    @QueryParamsValidator(KeyRequest.class)
    public CompletionStage<Result> removeKey() {

        final CompletionStage<Object> completionStage;
        final CacheRequest cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);

        completionStage = cacheService.getKeyDatabase(cacheRequest.getDatabase(),cacheRequest.getKey());

        return completionStage.thenCompose(completionStage1->{
            if(completionStage1 instanceof CacheAsyncApi) {
                return FutureConverters.toJava(((CacheAsyncApi)completionStage1).get(cacheRequest.getKey(), scala.reflect.ClassTag$.MODULE$.apply(HashMap.class)))
                        .thenApply((value)->{
                           ((CacheAsyncApi)completionStage1).remove(cacheRequest.getKey());
                            ResponseMessage responseMessage;
                            responseMessage = new ResponseMessage(200, "Successfully removed the key value pair (" + cacheRequest.getKey() + "," + ((Option) value).get().toString() + ")");
                            return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                        }).exceptionally((throwable) -> {
                            ResponseMessage responseMessage;
                            responseMessage = new ResponseMessage(200, cacheRequest.getKey() + " doesn't existsKey. Please try with a different key..!!! ");
                            return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                        });
            } else {
                ResponseMessage responseMessage = (ResponseMessage) completionStage1;
                return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
            }
        });
    }

    @QueryParamsValidator(KeyRequest.class)
    public CompletionStage<Result> existsKey(){

        final CompletionStage<Object> completionStage;
        final CacheRequest cacheRequest;

        cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);
        completionStage = cacheService.getKeyDatabase(cacheRequest.getDatabase(),cacheRequest.getKey());

        return completionStage.thenCompose(completionStage1->{
            if(completionStage1 instanceof CacheAsyncApi) {
                return FutureConverters.toJava(((CacheAsyncApi)completionStage1).exists(cacheRequest.getKey()))
                        .thenApply(aBoolean -> {
                            Boolean bBoolean = ((Boolean) aBoolean);
                            ResponseMessage responseMessage;
                            responseMessage = new ResponseMessage(200,bBoolean.toString());
                            return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                        });
            } else {
                ResponseMessage responseMessage = (ResponseMessage) completionStage1;
                return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
            }
        });
    }

}

package com.rogers.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rogers.requestHandlers.CacheRequest;
import com.rogers.requestHandlers.KeyRequest;
import com.rogers.responses.ResponseMessage;
import com.rogers.services.CacheService;
import com.rogers.validators.RequestValidator;
import play.api.cache.redis.CacheAsyncApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Option;
import scala.compat.java8.FutureConverters;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.groups.Default;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Singleton
public class KeyController extends Controller {

    private RequestValidator requestValidator;
    private CacheService cacheService;

    @Inject
    public KeyController(RequestValidator requestValidator, CacheService cacheService) {
        this.requestValidator = requestValidator;
        this.cacheService = cacheService;
    }

    public CompletionStage<Result> insertKey() {

        final Object object;
        final CompletionStage<Object> completionStage;

        object = requestValidator.insertRequestValidator(request(),Default.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;

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

    public CompletionStage<Result> retrieveValue() {

        final Object object;
        final CompletionStage<Object> completionStage;

        object = requestValidator.requestValidator(request(),KeyRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;

        completionStage = cacheService.getKeyDatabase(cacheRequest.getDatabase(),cacheRequest.getKey());

        return completionStage.thenCompose(completionStage1->{
            if(completionStage1 instanceof CacheAsyncApi) {
               return FutureConverters.toJava(((CacheAsyncApi)completionStage1).get(cacheRequest.getKey(), scala.reflect.ClassTag$.MODULE$.apply(HashMap.class)))
                        .thenApply((value)->{
                            ObjectNode jsonResponse = Json.newObject();
                            jsonResponse.put("status",200);
                            jsonResponse.put("message","Value Stored at " + cacheRequest.getKey() + " is " + ((Option) value).get().toString());
                            jsonResponse.putPOJO("value",((Option) value).get());
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

    public CompletionStage<Result> removeKey() {

        final Object object;
        final CompletionStage<Object> completionStage;

        object = requestValidator.requestValidator(request(),KeyRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;

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

    public CompletionStage<Result> existsKey(){

        final Object object;
        final CompletionStage<Object> completionStage;

        object = requestValidator.requestValidator(request(),KeyRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;

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

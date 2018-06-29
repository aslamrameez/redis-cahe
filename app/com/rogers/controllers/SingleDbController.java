package com.rogers.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rogers.constants.RogersConstants;
import com.rogers.request.CacheRequest;
import com.rogers.request.DatabaseRequest;
import com.rogers.response.ResponseMessage;
import com.rogers.service.CacheService;
import com.rogers.validator.Annotations.PostBodyValidator;
import com.rogers.validator.Annotations.QueryParamsValidator;
import com.rogers.validator.RequestValidator;
import play.api.cache.redis.CacheAsyncApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.collection.Seq;
import scala.compat.java8.FutureConverters;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class SingleDbController extends Controller {

    private RequestValidator requestValidator;
    private CacheService cacheService;

    @Inject
    public SingleDbController(RequestValidator requestValidator, CacheService cacheService) {
        this.requestValidator = requestValidator;
        this.cacheService = cacheService;
    }

    @PostBodyValidator(DatabaseRequest.class)
    public CompletionStage<Result> addDatabase(){

        final CacheRequest cacheRequest;
        final CompletionStage<ResponseMessage> completionStage;

        cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);
        completionStage = cacheService.addNewDatabase(cacheRequest.getDatabase(), cacheRequest.getPattern());

        return completionStage.thenApply(responseMessage ->
                status(responseMessage.getStatus(),Json.toJson(responseMessage))
        );
    }//Adds a database according to user request

    @QueryParamsValidator(DatabaseRequest.class)
    public CompletionStage<Result> clearDatabase() {

        final CacheRequest cacheRequest;
        final CompletionStage<Object> completionStage;

        cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);

        completionStage = cacheService.getPatternDatabase(cacheRequest.getDatabase(),cacheRequest.getPattern(),"removeKey");

        return completionStage.thenApply(completionStage1->{
            if(completionStage1 instanceof ResponseMessage){
                ResponseMessage responseMessage = (ResponseMessage)completionStage1;
                return status(responseMessage.getStatus(),Json.toJson(responseMessage));
            }
            if(completionStage1 instanceof CacheAsyncApi) {
                FutureConverters.toJava(((CacheAsyncApi) completionStage1).invalidate());
                ResponseMessage responseMessage;
                responseMessage = new ResponseMessage(200, "Successfully flushed the database Containing keys starting with " + cacheRequest.getPattern());
                return status(responseMessage.getStatus(), Json.toJson(responseMessage));
            }
            return status(400);
        });

    } // Deleting the content in a specific database

    @QueryParamsValidator(DatabaseRequest.class)
    public CompletionStage<Result> flushDatabase(){

        final CacheRequest cacheRequest;
        final CompletionStage<Object> completionStage;

        cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);
        completionStage = cacheService.getPatternDatabase(cacheRequest.getDatabase(),cacheRequest.getPattern(),"flush");

        return completionStage.thenApply(completionStage1->{
            if(completionStage1 instanceof ResponseMessage){
                ResponseMessage responseMessage = (ResponseMessage)completionStage1;
                return status(responseMessage.getStatus(),Json.toJson(responseMessage));
            }
            if(completionStage1 instanceof CacheAsyncApi) {
                FutureConverters.toJava(((CacheAsyncApi) completionStage1).invalidate());
                ResponseMessage responseMessage;
                responseMessage = new ResponseMessage(200, "Successfully flushed the database Containing keys starting with " + cacheRequest.getPattern());
                return status(responseMessage.getStatus(), Json.toJson(responseMessage));
            }
            return status(400);
        });
    }  // Deleting the whole database

    @QueryParamsValidator(DatabaseRequest.class)
    public CompletionStage<Result> getAll(){

        final CacheRequest cacheRequest;
        final CompletionStage<Object> completionStage;

        cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);
        completionStage = cacheService.getPatternDatabase(cacheRequest.getDatabase(),cacheRequest.getPattern(),"fetch");

        return completionStage.thenCompose(completionStage1->{
            if(completionStage1 instanceof CacheAsyncApi) {
             return completionStage.thenCompose(cacheAsyncApi ->
                FutureConverters.toJava(((CacheAsyncApi)cacheAsyncApi).matching("*"))
                        .thenApply((list)-> {
                                    Seq<String> seq = (Seq<String>) list;
                                    List<String> list1 = scala.collection.JavaConversions.seqAsJavaList(seq);
                                    ObjectNode jsonResponse = Json.newObject();
                                    jsonResponse.put(RogersConstants.RESPONSE_STATUS, 200);
                                    jsonResponse.put("No. of key/value pairs", list1.size());
                                    jsonResponse.putPOJO("Keys Present", Json.toJson(list1));
                                    return ok(jsonResponse);
                                }
                        ));
             }
            ResponseMessage responseMessage = (ResponseMessage) completionStage1;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
        });
}

    @QueryParamsValidator(DatabaseRequest.class)
    public CompletionStage<Result> existsDatabase(){

        final CacheRequest cacheRequest;

        cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);
        return cacheService.checkDbExistence(cacheRequest.getDatabase())
                .thenApply(aBoolean -> {
                    ResponseMessage responseMessage;
                    responseMessage = new ResponseMessage(200,aBoolean.toString());
                    return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                });
    }

    @QueryParamsValidator(DatabaseRequest.class)
    public CompletionStage<Result> getDbConf(){

        final CacheRequest cacheRequest;

        cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);

        return cacheService.getCacheConfigruration(cacheRequest.getDatabase())
                            .thenApply(map->{
                                if(map==null){
                                    return new ResponseMessage(400,"No Database with given name");
                                }
                                return map;
                            })
                            .thenApply(object->{
                                if(object instanceof ResponseMessage){
                                    ResponseMessage responseMessage = ((ResponseMessage) object);
                                    return status(responseMessage.getStatus(),Json.toJson(responseMessage));
                                }
                                return ok(Json.toJson(object));
                            });

    }
}

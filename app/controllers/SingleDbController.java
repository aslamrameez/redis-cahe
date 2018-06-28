package controllers;

import requestHandlers.CacheRequest;
import requestHandlers.DatabaseRequest;
import responses.ResponseMessage;
import validators.RequestValidator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.api.cache.redis.CacheAsyncApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.collection.Seq;
import scala.compat.java8.FutureConverters;
import services.CacheService;

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

    public CompletionStage<Result> addDatabase(){

        final Object object;
        final CompletionStage<ResponseMessage> completionStage;

        object = requestValidator.requestValidator(request(), DatabaseRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(),Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;
        completionStage = cacheService.addNewDatabase(cacheRequest.getDatabase(), cacheRequest.getPattern());

        return completionStage.thenApply(responseMessage ->
                status(responseMessage.getStatus(),Json.toJson(responseMessage))
        );
    }//Adds a database according to user request

    public CompletionStage<Result> clearDatabase() {

        final Object object;
        final CompletionStage<Object> completionStage;

        object = requestValidator.requestValidator(request(), DatabaseRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(),Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;

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

    public CompletionStage<Result> flushDatabase(){

        final Object object;
        final CompletionStage<Object> completionStage;

        object = requestValidator.requestValidator(request(), DatabaseRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(),Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;
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

    public CompletionStage<Result> getAll(){

        final Object object;
        final CompletionStage<Object> completionStage;

        object = requestValidator.requestValidator(request(), DatabaseRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(),Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;

        completionStage = cacheService.getPatternDatabase(cacheRequest.getDatabase(),cacheRequest.getPattern(),"fetch");

        return completionStage.thenCompose(completionStage1->{
            if(completionStage1 instanceof CacheAsyncApi) {
             return completionStage.thenCompose(cacheAsyncApi ->
                FutureConverters.toJava(((CacheAsyncApi)cacheAsyncApi).matching("*"))
                        .thenApply((list)-> {
                                    Seq<String> seq = (Seq<String>) list;
                                    List<String> list1 = scala.collection.JavaConversions.seqAsJavaList(seq);
                                    ObjectNode jsonResponse = Json.newObject();
                                    jsonResponse.put("status", 200);
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

    public CompletionStage<Result> existsDatabase(){

        final Object object;

        object = requestValidator.requestValidator(request(), DatabaseRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(), Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;

        return cacheService.checkDbExistence(cacheRequest.getDatabase())
                .thenApply(aBoolean -> {
                    ResponseMessage responseMessage;
                    responseMessage = new ResponseMessage(200,aBoolean.toString());
                    return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                });
    }

    public CompletionStage<Result> getDbConf(){

        final Object object;

        object = requestValidator.requestValidator(request(), DatabaseRequest.class);

        if(object instanceof ResponseMessage){
            ResponseMessage responseMessage = (ResponseMessage) object;
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(),Json.toJson(responseMessage)));
        }

        CacheRequest cacheRequest = (CacheRequest) object;

        return cacheService.getCacheConfigruration(cacheRequest.getDatabase())
                            .thenApply(map->{
                                if(map==null){
                                    return new ResponseMessage(400,"No Database with given name");
                                }
                                return map;
                            })
                            .thenApply(object1->{
                                if(object instanceof ResponseMessage){
                                    ResponseMessage responseMessage = ((ResponseMessage) object1);
                                    return status(responseMessage.getStatus(),Json.toJson(responseMessage));
                                }
                                return ok(Json.toJson(object1));
                            });

    }
}

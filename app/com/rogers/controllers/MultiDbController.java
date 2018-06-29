package com.rogers.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rogers.constants.RogersConstants;
import com.rogers.request.CacheRequest;
import com.rogers.request.MultiDbRequest;
import com.rogers.request.MultiInsertRequest;
import com.rogers.response.ResponseMessage;
import com.rogers.service.CacheService;
import com.rogers.validator.Annotations.PostBodyValidator;
import com.rogers.validator.Annotations.QueryParamsValidator;
import com.rogers.validator.RequestValidator;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Option;
import scala.compat.java8.FutureConverters;
import scala.concurrent.duration.Duration;
import scala.reflect.ClassTag$;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.rogers.validator.RequestValidator.insertRequestValidator;

public class MultiDbController extends Controller {

    private RequestValidator requestValidator;
    private CacheService cacheService;

    @Inject
    public MultiDbController(RequestValidator requestValidator, CacheService cacheService) {
        this.requestValidator = requestValidator;
        this.cacheService = cacheService;
    }


    public Result flushAll() {

        try {
            cacheService
                    .getAvailableDbs()
                    .thenAccept((list)->{
                        list.forEach(s -> {
                            if(!(s.equalsIgnoreCase("default") || s.equalsIgnoreCase("unused"))) {
                                cacheService.getCache(s)
                                        .thenAccept((cacheAsyncApi) -> {
                                            cacheAsyncApi.invalidate();
                                        });
                                        }
                            });
                        })
            .thenApply((some)->{
                cacheService.flushAll();
                return null;
            });
        } catch (Exception e) {
            ResponseMessage responseMessage;
            responseMessage = new ResponseMessage(400, "Couldn't flush the database --> " + e);
            return status(responseMessage.getStatus(), Json.toJson(responseMessage));
        }
        ResponseMessage responseMessage;
        responseMessage = new ResponseMessage(200, "Successfully flushed the database..!!!");
        return status(responseMessage.getStatus(), Json.toJson(responseMessage));
    } // All Databases are flushed and Comes to reset state

    public CompletionStage<Result> countAllDatabases(){

        return cacheService.countAvailableDbs()
                .thenApply(count -> {
                    ResponseMessage responseMessage;
                    responseMessage = new ResponseMessage(200, "No. of databases present " + count.toString());
                    return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                });
    }

    public CompletionStage<Result> listAllDatabases(){

        return cacheService.getAvailableDbs()
                        .thenApply((list)->{
                            ObjectNode jsonResponse = Json.newObject();
                            jsonResponse.put(RogersConstants.RESPONSE_STATUS,200);
                            jsonResponse.put("No. of key/value pairs",list.size());
                            jsonResponse.putPOJO("Keys Present",Json.toJson(list));
                            return ok(jsonResponse);
                                });
    }

    @QueryParamsValidator(MultiDbRequest.class)
    public CompletionStage<Result> retrieveValue(){

       final CacheRequest cacheRequest;

       cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);

          return  cacheService
                    .getAvailableDbs()
                    .thenApply((list)->{
                        ObjectNode jsonResponse = Json.newObject();
                        for(String database : list){
                            if(database.equalsIgnoreCase("default") || database.equalsIgnoreCase("unused")){continue;}
                            try {
                                cacheService.getCache(database)
                                        .thenCompose(cacheAsyncApi -> FutureConverters.toJava(cacheAsyncApi.get(cacheRequest.getKey(), ClassTag$.MODULE$.apply(HashMap.class)))
                                                .thenAccept((value)->{
                                                    jsonResponse.put(RogersConstants.RESPONSE_STATUS,200);
                                                    jsonResponse.put(RogersConstants.RESPONSE_MESSAGE,"Value Stored at " + cacheRequest.getKey() + " is " + ((Option) value).get().toString());
                                                    jsonResponse.putPOJO(RogersConstants.RESPONSE_VALUE,((Option) value).get());
                                                }))
                                .toCompletableFuture()
                                        .get();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                jsonResponse.put("status",400);
                                jsonResponse.put("message",cacheRequest.getKey() + " doesn't existsKey. Please try with a different key");
                            }
                            if(jsonResponse.get("status").asInt()==200) break;
                        }
                        return ok(jsonResponse);
                    });

    }
    //Need Refactoring.. Used completableFuture.get() method

    @PostBodyValidator(MultiInsertRequest.class)
    public CompletionStage<Result> insertKey(){

        final CacheRequest cacheRequest;

        try {
            cacheRequest = insertRequestValidator(request());
        } catch (Exception e) {
            ResponseMessage responseMessage = new ResponseMessage(400,e.getMessage());
            return CompletableFuture.completedFuture(status(responseMessage.getStatus(),Json.toJson(responseMessage)));
        }

        return  cacheService
                .getAvailableDbs()
                .thenApply((list)->{
                    for(String database : list){
                        if(database.equalsIgnoreCase("default") || database.equalsIgnoreCase("unused")){continue;}
                        cacheService.getCache(database)
                                .thenCompose(cacheAsyncApi -> FutureConverters.toJava(cacheAsyncApi.set(cacheRequest.getKey(), cacheRequest.getValue(), Duration.create(cacheRequest.getTtl(), TimeUnit.MINUTES))));
                    }
                    ResponseMessage responseMessage;
                    responseMessage = new ResponseMessage(200, "Inserted (" + cacheRequest.getKey() + "," + cacheRequest.getValue() + ") Successfully into all Available databases");
                    return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                });
    }

    @QueryParamsValidator(MultiDbRequest.class)
    public CompletionStage<Result> removeKey(){

        final CacheRequest cacheRequest;

        cacheRequest = (CacheRequest) ctx().args.get(RogersConstants.REQUEST_OBJECT);

        return  cacheService
                .getAvailableDbs()
                .thenApply((list)->{
                    for(String database : list){
                        if(database.equalsIgnoreCase("default") || database.equalsIgnoreCase("unused")){continue;}
                        cacheService.getCache(database)
                                .thenCompose(cacheAsyncApi -> FutureConverters.toJava(cacheAsyncApi.remove(cacheRequest.getKey())));
                    }
                    ResponseMessage responseMessage;
                    responseMessage = new ResponseMessage(200, "Removed " + cacheRequest.getKey() + " Successfully from all databases");
                    return status(responseMessage.getStatus(), Json.toJson(responseMessage));
                });
    }

}

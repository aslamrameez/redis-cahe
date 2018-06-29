package com.rogers.service;

import com.rogers.response.ResponseMessage;
import play.api.cache.redis.CacheAsyncApi;
import play.api.cache.redis.RedisList;
import play.cache.NamedCache;
import scala.Option;
import scala.collection.Seq;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;


@Singleton
public class CacheService {

    private CacheAsyncApi cache1;
    private CacheAsyncApi cache2;
    private CacheAsyncApi cache3;
    private CacheAsyncApi cache4;
    private CacheAsyncApi cache5;
    private CacheAsyncApi cache6;
    private CacheAsyncApi cache7;
    private CacheAsyncApi cache8;
    private CacheAsyncApi cache9;
    private CacheAsyncApi cache10;    // Making 10 databases available to the user

    private Map<Integer,CacheAsyncApi> cacheMap = new HashMap<>();
    private CacheAsyncApi dbKeyCache;

    private RedisList redisList;
    @Inject
    public CacheService(@NamedCache("cache1") CacheAsyncApi cache1,
                        @NamedCache("cache2") CacheAsyncApi cache2,
                        @NamedCache("cache3") CacheAsyncApi cache3,
                        @NamedCache("cache4") CacheAsyncApi cache4,
                        @NamedCache("cache5") CacheAsyncApi cache5,
                        @NamedCache("cache6") CacheAsyncApi cache6,
                        @NamedCache("cache7") CacheAsyncApi cache7,
                        @NamedCache("cache8") CacheAsyncApi cache8,
                        @NamedCache("cache9") CacheAsyncApi cache9,
                        @NamedCache("cache10") CacheAsyncApi cache10,
                        CacheAsyncApi dbKeyCache
                          ) {
        this.cache1 = cache1;
        this.cache2 = cache2;
        this.cache3 = cache3;
        this.cache4 = cache4;
        this.cache5 = cache5;
        this.cache6 = cache6;
        this.cache7 = cache7;
        this.cache8 = cache8;
        this.cache9 = cache9;
        this.cache10 = cache10;

        this.cacheMap.put(1,cache1);
        this.cacheMap.put(2,cache2);
        this.cacheMap.put(3,cache3);
        this.cacheMap.put(4,cache4);
        this.cacheMap.put(5,cache5);
        this.cacheMap.put(6,cache6);
        this.cacheMap.put(7,cache7);
        this.cacheMap.put(8,cache8);
        this.cacheMap.put(9,cache9);
        this.cacheMap.put(10,cache10);

        this.dbKeyCache = dbKeyCache;
        this.dbKeyCache.setIfNotExists("default","0",Duration.apply(30, TimeUnit.DAYS));
        this.redisList = dbKeyCache.list("unused",scala.reflect.ClassTag$.MODULE$.apply(Integer.class));
    }

    private final int size = 10;

    public CompletionStage<Boolean> checkDbExistence(String database){

             return FutureConverters.toJava(dbKeyCache.exists(database))
                    .thenApply(value-> (boolean)value);

    }

    public CompletionStage<Integer> countAvailableDbs(){

        return FutureConverters.toJava(dbKeyCache.get("default",scala.reflect.ClassTag$.MODULE$.apply(Integer.class)))
                    .thenApply((option)->{
                        int counter = (int)((Option)option).get();
                        return counter;
                    })
                    .exceptionally(throwable -> 0);
    }

    public CompletionStage<Map> getCacheConfigruration(String database){

        return FutureConverters.toJava(dbKeyCache.get(database,scala.reflect.ClassTag$.MODULE$.apply(HashMap.class)))
                .thenApply(value->(Map)((Option)value).get())
                .exceptionally((throwable) -> null );
    }

    public CompletionStage<CacheAsyncApi> getCache(String database){

        return getCacheConfigruration(database)
                .thenApply((value)->{
                    Integer count = (Integer)(value.get("cache"));
                    CacheAsyncApi cacheAsyncApi = cacheMap.get(count);
                    return cacheAsyncApi;
                });
    }

    private Boolean checkPattern(String actual, String given, String spec){
        if(spec.equals("db")) {
            if (actual.equals(given)) return true;
            else return false;
        } else{
            if(given.matches(actual + ".+")) return true;
            else return false;
        }
    }

    public CompletionStage<List<String>> getAvailableDbs(){

        return FutureConverters.toJava(dbKeyCache.matching("*"))
                .thenApply(seq ->
                        scala.collection.JavaConversions.seqAsJavaList((Seq<String>)seq)
                );
    }
    //Counter needs to be removed from list


    public CompletionStage<ResponseMessage> addNewDatabase(String database, String keyPattern){

        return checkDbExistence(database)
            .thenCombineAsync(countAvailableDbs(),(exists,count)->{
                if(exists)
                    return new ResponseMessage(400,"Cache with same database name exists.");

                if(count==size){
                    return new ResponseMessage(400,"All Databases are Occupied");
                }

                FutureConverters.toJava((Future<Boolean>) redisList.isEmpty())
                        .thenAccept(aBoolean->{
                            if((Boolean) aBoolean){
                                Map<String,Object> map = new HashMap<>();
                                map.put("pattern",keyPattern);
                                map.put("cache", count + 1);
                                dbKeyCache.set(database,map, Duration.apply(30, TimeUnit.DAYS));
                                dbKeyCache.increment("default",1);
                            }
                            else{
                                FutureConverters.toJava((Future<Integer>) redisList.head())
                                        .thenAccept(value->{
                                            Map<String,Object> map = new HashMap<>();
                                            map.put("pattern",keyPattern);
                                            map.put("cache",value);
                                            dbKeyCache.set(database,map, Duration.apply(30, TimeUnit.DAYS));
                                            dbKeyCache.increment("default",1);
                                            redisList.headPop();
                            });
                            }
                        });

                return new ResponseMessage(200,"Successfully added database");
            });

    }

    public CompletionStage<Object> getKeyDatabase(String database, String keyPattern) {

        return countAvailableDbs()
                .thenCombineAsync((checkDbExistence(database)),(count,exists)->{
                    if (count == 0)
                        return new ResponseMessage(200,"No databases Available");

                    if(!exists)
                        return new ResponseMessage(400,"No database with given name");

                    return new ResponseMessage(200);
                })
                .thenCombineAsync(getCacheConfigruration(database),(responseMessage,map)->{
                    if(responseMessage.getStatus()==400) return responseMessage;
                    else {
                        if(map==null)
                            return new ResponseMessage(400,"No database with given name");

                        if(!checkPattern((String)map.get("pattern"),keyPattern,"key")){
                            return new ResponseMessage(400,"Database and key format is not proper");

                        };
                        Integer count = (Integer)(map.get("cache"));
                        return cacheMap.get(count);
                    }
                });

    }

    public void flushAll(){

            //list.clear();
            redisList.modify().clear();
            dbKeyCache.invalidate();
            dbKeyCache.set("default","0",Duration.apply(30, TimeUnit.DAYS));

    }

    public CompletionStage<Object> getPatternDatabase(String database, String keyPattern, String action ) {

        return checkDbExistence(database)
                .thenCombineAsync(getCacheConfigruration(database),((exists, map) -> {
                    if(!exists)
                        return new ResponseMessage(400,"No database with given name");

                    if(!checkPattern((String)map.get("pattern"),keyPattern,"db"))
                        return new ResponseMessage(400,"Database and Key format is not proper");

                    if(action.matches("flush")) {
                        redisList.append(map.get("cache"));
                       // list.add((Integer) map1.get("cache"))
                        dbKeyCache.remove(database);
                        dbKeyCache.decrement("default",1);
                    }

                    Integer count = (Integer)(map.get("cache"));
                    return cacheMap.get(count);
                }));

    } //Free the cache from db-key and make it possible for re-use

}


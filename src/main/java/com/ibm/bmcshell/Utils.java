package com.ibm.bmcshell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {




    public interface Callable<R>{
        R apply() throws JsonProcessingException;
    }
    public static class EndPoints implements Comparable
    {
        public String url;
        public String action;
        EndPoints(String u,String a){
            url =u;
            action=a;
        }

        @Override
        public String toString() {
            return url + "("+action+")";
        }

        @Override
        public int compareTo(Object o) {
            var other=(EndPoints)o;
            return url.compareTo(other.url);
        }
    };
    public static <R> R tryUntil(int count, Callable<R> f)  {
        while (count>0){
            try{
                return f.apply();
            }catch (Exception  ex){
                if(--count ==0){
                    try {
                        throw ex;
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return null;
    }
    static String currentEventFilters="*";
    public static WebClient createWebClient() throws SSLException {
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        final int size = 1024 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(30000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5000))
                                .addHandlerLast(new WriteTimeoutHandler(5000)));

        return WebClient.builder().exchangeStrategies(strategies).clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

    public static String eventFilters()
    {
        return currentEventFilters;
    }
    public static void setEventFilter(String filter) {
        currentEventFilters=filter;
    }
    public static String base(String m){
         final  String baseUrl="https://%s.aus.stglabs.ibm.com";
//        final  String baseUrl="https://%s.aus.stglabs.ibm.com:8081";
         return String.format(baseUrl,m);

    }
    public static String normalise(String url)
    {
        if(url.isEmpty()){
            return "/redfish/v1/";
        }
        if(url.startsWith("/")){
            return url;
        }
        if(url.startsWith("/redfish/v1/")){
            return url;
        }
        return "/redfish/v1/"+url;

    }
    public static void addToMachineList(String machine) throws IOException {
        var v=listOfMachines();

        if(v.stream().filter(a->a.url.equals(machine)).count()==0){
            var stream=new FileOutputStream(new File("machines"),true);
            stream.write("\n".getBytes(StandardCharsets.UTF_8));
            stream.write(machine.getBytes(StandardCharsets.UTF_8));
        }
    }
    public static List<EndPoints> listOfMachines() throws IOException {
        var file= new File("machines");
        if(!file.exists()){
            var stream=new FileOutputStream(file);
            stream.write("rain104bmc".getBytes(StandardCharsets.UTF_8));
            stream.write("\n".getBytes(StandardCharsets.UTF_8));
            stream.write("rain111bmc".getBytes(StandardCharsets.UTF_8));
            stream.write("\n".getBytes(StandardCharsets.UTF_8));
            stream.write("rain127bmc".getBytes(StandardCharsets.UTF_8));
            stream.write("\n".getBytes(StandardCharsets.UTF_8));
            stream.write("rain135bmc".getBytes(StandardCharsets.UTF_8));
            stream.write("\n".getBytes(StandardCharsets.UTF_8));
            stream.write("rain136bmc".getBytes(StandardCharsets.UTF_8));
            stream.close();

        }
        var ret=Arrays.stream(new String(new FileInputStream(new File("machines")).readAllBytes()).split("\n")).map(a->new EndPoints(a,"Get")).collect(Collectors.toList());
        return ret;
    }
    static Stream<String> getFieldNames(JsonNode node){
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.fieldNames(), Spliterator.ORDERED),true);
    }
    static void collectActions(List<EndPoints> next, JsonNode node)
    {
        getFieldNames(node).forEach(a->System.out.println(a+"\n"));
        getFieldNames(node)
                .map(a->node.get(a))
                .map(a->a.get("target"))
                .filter(a->a!=null)
                .forEach(a->{
                    next.add(new EndPoints(a.asText(),"Post"));
                });
    }
    public static List<EndPoints> sorted(List<EndPoints> endPoints)
    {
        return endPoints.stream().sorted().collect(Collectors.toList());
    }
    static public List<EndPoints> buildLinksAndTargets(JsonNode ret)  {
        List<EndPoints> next=new ArrayList<>();
        try {
            getFieldNames(ret)
                    .filter(a->!a.startsWith("@"))
                    .map(a->ret.get(a))
                    .forEach(a->{
                        if(a.has("@odata.id")){
                            next.add(new EndPoints(a.get("@odata.id").asText(),"Get"));
                        }
                        next.addAll(buildLinksAndTargets(a));
                    });
            getFieldNames(ret)
                    .filter(a->a.equals("Actions"))
                    .map(a->ret.get(a))
                    .forEach(a->{
                        collectActions(next,a);
                    });
            getFieldNames(ret)
                    .map(a->ret.get(a))
                    .filter(a->a.isArray())
                    .forEach(a->{
                        StreamSupport.stream(a.spliterator(),false)
                                .map(b->b.get("@odata.id"))
                                .filter(b->b!=null)
                                .forEach(b->next.add(new EndPoints(b.asText(),"Get")));
                        StreamSupport.stream(a.spliterator(),false)
                                .forEach(b->{
                                    next.addAll(buildLinksAndTargets(b));
                                });

                    });
        }
        catch (Exception e){

        }

        return next;
    }
}
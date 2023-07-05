package com.ibm.bmcshell;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {
    public interface Callable<R>{
        R apply();
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
    public static <R> R tryUntil(int count, Callable<R> f){
        while (count>0){
            try{
                return f.apply();
            }catch (Exception ex){
                if(--count ==0){
                    throw ex;
                }
            }
        }
        return null;
    }
    public static WebClient createWebClient() throws SSLException {
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5000))
                                .addHandlerLast(new WriteTimeoutHandler(5000)));

        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
    public static String base(String m){
         final  String baseUrl="https://%s.aus.stglabs.ibm.com/redfish/v1/";
//        final  String baseUrl="https://%s.aus.stglabs.ibm.com:8081/redfish/v1/";
         return String.format(baseUrl,m);


    }
    public static List<EndPoints> listOfMachines(){
        return List.of(new EndPoints("back","Get"),new EndPoints("rain104bmc","Get"),new EndPoints("rain111bmc","Get"),new EndPoints("rain127bmc","Get"),new EndPoints("rain135bmc","Get"),new EndPoints("rain136bmc","Get"));
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
package com.ibm.bmcshell.redfish;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class SchemaFetcher {

    public static List<String> fetchSchemaFiles() {
        try {
            // Fetch the HTML page
            Document doc = Jsoup.connect("https://redfish.dmtf.org/redfish/schema_index").get();

            // Extract and categorize schema links
            List<String> csdlLinks = new ArrayList<>();
            List<String> jsonLinks = new ArrayList<>();
            List<String> yamlLinks = new ArrayList<>();

            Elements rows = doc.select("table tbody tr");
            for (Element row : rows) {
                Elements links = row.select("a[href]");
                for (Element link : links) {
                    String href = link.attr("href");
                    if (href.endsWith(".xml")) {
                        csdlLinks.add(href);
                    } else if (href.endsWith(".json")) {
                        jsonLinks.add(href);
                    } else if (href.endsWith(".yaml")) {
                        yamlLinks.add(href);
                    }
                }
            }


            System.out.println("\nJSON Links:");
            jsonLinks.forEach(System.out::println);


            return jsonLinks;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void fetchSchemaFiles(String path) {
        List<String> jsonLinks = fetchSchemaFiles();
        path = path.endsWith("/") ? path : path + "/";
        String finalPath = path;
        jsonLinks.stream().map(link -> "https://redfish.dmtf.org" + link).forEach(l->{

            try {
                WebClient webClient = WebClient.create();
                String jsonContent = webClient.get()
                        .uri(l)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                ObjectMapper objectMapper = new ObjectMapper();
                Object json = objectMapper.readValue(jsonContent, Object.class);
                ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
                String prettyJson = writer.writeValueAsString(json);

                String fileName = l.substring(l.lastIndexOf('/') + 1);

                java.nio.file.Path fPath = java.nio.file.Paths.get(finalPath + fileName);
                java.nio.file.Files.createDirectories(fPath.getParent());
                java.nio.file.Files.write(fPath, prettyJson.getBytes());

            } catch (IOException e) {
                System.out.println("Failed to fetch schema file: " + l);
            }

        });
    }

    public static void main(String[] args) {

        fetchSchemaFiles("schemas/");


    }
}

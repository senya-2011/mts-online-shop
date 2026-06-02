package com.mts.online_shop.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/camunda")
public class CamundaDashboardController {

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> dashboard() {
        String html = "<!doctype html>\n" +
                "<html><head><meta charset=\"utf-8\"><title>Camunda Dashboard</title></head><body>\n" +
                "<h1>Camunda & Application Dashboard</h1>\n" +
                "<p>Preconfigured test user: <strong>admin</strong> / <strong>admin</strong></p>\n" +
                "<h2>Camunda Engine REST</h2>\n" +
                "<ul>\n" +
                "<li><a href=\"/engine-rest/engine\">/engine-rest/engine</a></li>\n" +
                "<li><a href=\"/engine-rest/process-definition\">/engine-rest/process-definition</a></li>\n" +
                "<li><a href=\"/engine-rest/process-instance\">/engine-rest/process-instance</a></li>\n" +
                "<li><a href=\"/engine-rest/task\">/engine-rest/task</a></li>\n" +
                "<li><a href=\"/engine-rest/history/process-instance?processDefinitionKey=order_process\">History for order_process</a></li>\n" +
                "</ul>\n" +
                "<h2>Application API (examples)</h2>\n" +
                "<ul>\n" +
                "<li><a href=\"/api/products\">/api/products</a> (products list)</li>\n" +
                "<li><a href=\"/api/orders\">/api/orders</a> (orders)</li>\n" +
                "<li><a href=\"/api/auth/login\">/api/auth/login</a> (auth)</li>\n" +
                "</ul>\n" +
                "<h2>Useful curl examples</h2>\n" +
                "<pre>curl -u admin:admin http://localhost:8080/engine-rest/process-definition</pre>\n" +
                "<pre>curl -u admin:admin -H 'Content-Type: application/json' -d '{\"variables\":{\"userId\":{\"value\":2,\"type\":\"Long\"}}}' http://localhost:8080/engine-rest/process-definition/key/order_process/start</pre>\n" +
                "<p>Note: this dashboard is for local testing only.</p>\n" +
                "</body></html>";
        return ResponseEntity.ok(html);
    }
}

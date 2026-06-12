package com.mts.online_shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiDocsRedirectController {

    @GetMapping({"/", "/api", "/api/swagger-ui", "/api/swagger-ui/"})
    public String redirectToSwagger() {
        return "redirect:/api/swagger-ui/index.html";
    }
}

package com.mts.online_shop.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CamundaForwardController {

    @GetMapping({"/camunda-welcome", "/camunda-welcome/"})
    public String redirectWelcome() {
        // Redirect to Camunda webapp welcome page
        return "redirect:/camunda/app/welcome/default/";
    }

    @RequestMapping("/camunda-welcome/**")
    public String redirectWelcomeAssets() {
        // Forward any subpath to the webapp entry so Camunda serves assets
        return "redirect:/camunda/app/welcome/default/";
    }
}

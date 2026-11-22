package com.jobradar.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("prod")
class SpaController {

    @GetMapping({
        "/jobs", "/walk-in-drives/**", "/opportunities/**", "/login", "/register",
        "/submit-walk-in-drive", "/dashboard/**"
    })
    String forwardToAngular() {
        return "forward:/index.html";
    }
}

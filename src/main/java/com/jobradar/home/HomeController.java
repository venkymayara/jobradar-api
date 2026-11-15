package com.jobradar.home;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {
    private final HomeService home;

    public HomeController(HomeService home) { this.home = home; }

    @GetMapping
    public HomePageDto getHomepage() { return home.getHomepage(); }
}

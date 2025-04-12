package com.manhattan.reconciliation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for welcome page and redirects.
 */
@Controller
public class WelcomeController {
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    /**
     * Redirects the root URL to the Swagger UI.
     *
     * @return Redirect view to Swagger UI
     */
    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView("/swagger-ui/index.html");
    }
    
    /**
     * Redirects /api to the Swagger UI.
     *
     * @return Redirect view to Swagger UI
     */
    @GetMapping("/api")
    public RedirectView api() {
        return new RedirectView("/swagger-ui/index.html");
    }
    
    /**
     * Handles 404 errors.
     *
     * @return Error page view
     */
    @GetMapping("/error")
    public ModelAndView handleError() {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("applicationName", applicationName);
        modelAndView.addObject("message", "The requested page could not be found.");
        return modelAndView;
    }
}
package co.uk.karel.dev.cnicht.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward the root path to index.html so that the SPA is served.
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}

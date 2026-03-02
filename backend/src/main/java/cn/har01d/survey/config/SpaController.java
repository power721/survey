package cn.har01d.survey.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {
            "/",
            "/login",
            "/register",
            "/profile",
            "/surveys",
            "/surveys/**",
            "/votes",
            "/votes/**",
            "/s/**",
            "/v/**",
            "/admin/**",
    })
    public String forward() {
        return "forward:/index.html";
    }
}

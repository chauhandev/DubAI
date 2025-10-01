package com.dAdK.dubAI.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SayHelloController {

    @GetMapping("abc")
    public String sayHello(){
        return "Hello World";
    }
}

package com.varun.tcp.clockout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import com.varun.tcp.clockout.ui.TimeInUi;

@SpringBootApplication
public class DemoApplication {
    //@Autowired
    //TimeInUi tu;
    public static void main(String[] args) {

        ApplicationContext context = SpringApplication.run(DemoApplication.class, args);
        System.out.println("Hello World! from spring boot");
        context.getBean(TimeInUi.class);
    }

}

package org.nepe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NepeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NepeApplication.class, args);
    }

}

/*
* Development order:
* 1. Junit test on domains
* 2. port.in & port.out
* 3. adapter.out
* 4. adapter.in
* */

// agy --conversation=cd4e3f8f-4fa6-4104-9869-a9f88448c8d9
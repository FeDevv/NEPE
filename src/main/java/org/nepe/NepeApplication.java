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

/*
* ports development order:
* 1. settings ❎
* 2. competition ❎
* 3. inference ❎
* 4. match
* */

// analisi e studio della test suite
// effettuare mvn compile per verificare che tutto compili (da fare per l'inference e poi per match)
// agy --conversation=e6a812d6-cd19-4b9f-9f50-3694b9506fa2
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
* 3. inference
* 4. match
* partire dai moduli a dipendenza foglia (senza prerequisiti) per poi risalire verso i moduli più articolati.
* */

// analisi e studio della test suite

// far fare un sanity check e un mvnw compile per verificare che tutte le classi create compilino senza problemi. (sia per modulo settings che per modulo competition).
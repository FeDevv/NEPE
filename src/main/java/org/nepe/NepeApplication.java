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
* 1. domain core ✅
* 2. porte in & out ✅
* 3. outbound adapters ✅
* 4. application services
* 4.5 test suite
* 5. inbound adapters
* */

/*
*  study the whole project, do not believe blindly what's being written.
* */

// study LiveInferenceService

// agy --conversation=078e2eba-0428-4e1d-9d8e-d12a5cb9d49d
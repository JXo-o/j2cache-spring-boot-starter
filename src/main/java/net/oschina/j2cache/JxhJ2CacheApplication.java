package net.oschina.j2cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class JxhJ2CacheApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(JxhJ2CacheApplication.class, args);
        J2CacheCmd.main(args);
    }

}

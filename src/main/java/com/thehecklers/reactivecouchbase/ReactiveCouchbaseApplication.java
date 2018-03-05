package com.thehecklers.reactivecouchbase;

import com.couchbase.client.java.repository.annotation.Field;
import com.couchbase.client.java.repository.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.query.View;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.UUID;

@SpringBootApplication
public class ReactiveCouchbaseApplication {
    @Bean
    CommandLineRunner demoData(CoffeeRepository repo) {
        return args -> {
            repo.findAllById().flatMap(repo::delete).thenMany(
                    Flux.just("Jet Black Couchbase", "Darth Couchbase", "Black Alert Couchbase")
                        .map(name -> new Coffee(UUID.randomUUID().toString(), name))
                        .flatMap(repo::save))
                    .thenMany(repo.findAllById())
                    .subscribe(System.out::println);
        };
    }

    @Bean
    WebClient client() {
        return WebClient.create("http://localhost:8080/coffees");
    }

    public static void main(String[] args) {
        SpringApplication.run(ReactiveCouchbaseApplication.class, args);
    }
}

@RestController
class CouchController {
    private final CoffeeRepository repo;
    private final WebClient client;

    CouchController(CoffeeRepository repo, WebClient client) {
        this.repo = repo;
        this.client = client;
    }

    @GetMapping("/coffees")
    public Flux<Coffee> getAllCoffees() {
        return repo.findAllById();
    }

    @GetMapping(value = "/originalcoffees", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Coffee> getOriginalCoffees() {
        return client.get()
                .retrieve()
                .bodyToFlux(Coffee.class);
    }

}

interface CoffeeRepository extends ReactiveCrudRepository<Coffee, String> {
    @View(viewName = "all")
    Flux<Coffee> findAllById();
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class Coffee {
    @Id
    private String id;
    @Field
    private String name;
}
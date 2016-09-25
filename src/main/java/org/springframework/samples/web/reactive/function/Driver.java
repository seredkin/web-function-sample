/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.web.reactive.function;

import org.springframework.core.ResolvableType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.Response;
import org.springframework.web.reactive.function.RouterFunction;
import org.springframework.web.reactive.function.RouterFunctions;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpServer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.RequestPredicates.*;
import static org.springframework.web.reactive.function.RouterFunctions.toHttpHandler;

public class Driver {

    public static final String HOST = "localhost";

    public static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Driver driver = new Driver();
        driver.startReactorServer();

        System.out.println("Press ENTER to exit.");
        System.in.read();
    }

    public RouterFunction<?> routingFunction() {
        PersonHandler handler = new PersonHandler(new DummyPersonRepository());
        BlogPostService service = new BlogPostService();

        return RouterFunctions.route(GET("/person/{id}"), handler::getPerson)
                .and(RouterFunctions.route(GET("/person").and(accept(APPLICATION_JSON)),
                        handler::listPeople))
                .and(RouterFunctions.route(POST("/person").and(contentType(APPLICATION_JSON)),
                        handler::createPerson))
                .and(RouterFunctions.route(GET("/user/{id}"), service::getBlogPostsForUser));
    }


    RouterFunction<String> helloWorldRoute =
            request -> {
                if (request.path().equals("/hello-world")) {
                    return Optional.of(r -> Response.ok().body(BodyInserters.fromObject("Hello World"))
                    );
                } else {
                    return Optional.empty();
                }
            };

    public void startReactorServer() throws InterruptedException {
        RouterFunction<?> route = routingFunction();
        HttpHandler httpHandler = toHttpHandler(route);

        ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);
        HttpServer server = HttpServer.create(HOST, PORT);
        server.startAndAwait(adapter);
    }

    private class BlogPostService {
        Response<Mono> getBlogPostsForUser(Long id) {
            Map<String, Serializable> map = new LinkedHashMap<>();
            ArrayList<String> posts = new ArrayList<>();

            posts.add("Post1");
            posts.add("Post2");

            Mono myMono = Mono.when(
                    mono1 -> map.put("user", "Stephan"),
                    mono2 -> map.put("posts", posts)).awaitOnSubscribe();

            return Response.ok().build(BodyInserters.fromPublisher(myMono, Map.class));
        }
    }
}

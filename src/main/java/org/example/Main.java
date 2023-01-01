package org.example;

import io.undertow.Undertow;
import jakarta.servlet.Servlet;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.*;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServer;

import java.io.File;

import static org.springframework.http.HttpStatus.OK;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpHandler httpHandler = (request, response) -> {
            response.setStatusCode(OK);
            return response.writeWith(Flux.just(new DefaultDataBufferFactory().wrap("Manty".getBytes())));
        };

        // Netty
        ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);
        HttpServer.create().port(8080).handle(adapter).bindNow();

        // Undertow
        UndertowHttpHandlerAdapter undertowAdapter = new UndertowHttpHandlerAdapter(httpHandler);
        Undertow undertow = Undertow.builder().addHttpListener(8888, "localhost").setHandler(undertowAdapter).build();
        undertow.start();

        // Jetty
        Servlet servlet = new JettyHttpHandlerAdapter(httpHandler);

        Server server = new Server();
        ServletContextHandler contextHandler = new ServletContextHandler(server, "");
        contextHandler.addServlet(new ServletHolder(servlet), "/");
        contextHandler.start();

        ServerConnector connector = new ServerConnector(server);
        connector.setHost("localhost");
        connector.setPort(9090);
        server.addConnector(connector);
        server.start();

        // Tomcat
        TomcatHttpHandlerAdapter tomcatAdapter = new TomcatHttpHandlerAdapter(httpHandler);

        Tomcat tomcat = new Tomcat();
        File base = new File(System.getProperty("java.io.tmpdir"));
        Context rootContext = tomcat.addContext("", base.getAbsolutePath());

        Wrapper adaptorWrapper = Tomcat.addServlet(rootContext, "main", tomcatAdapter);
        adaptorWrapper.setAsyncSupported(true);
        rootContext.addServletMappingDecoded("/", "main");

        Connector tomcatConnector = new Connector();
        tomcatConnector.setPort(9999);
        tomcat.getService().addConnector(tomcatConnector);
        tomcat.start();

        tomcat.getServer().await();

    }
}
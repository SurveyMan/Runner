package edu.umass.cs.runner.system.backend.known.localhost.server;

import edu.umass.cs.runner.Runner;
import edu.umass.cs.runner.system.backend.known.localhost.LocalResponseManager;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.BindException;
import java.net.UnknownHostException;

public class WebServer {
    private final Server server;

    public WebServer(Server server) {
        this.server = server;
    }

    public void stop() throws WebServerException {
        try {
            this.server.stop();
        } catch (Exception ex) {
            throw new WebServerException(ex);
        }
    }

    public void join() throws WebServerException {
        try {
            this.server.join();
        } catch (InterruptedException e) {
            throw new WebServerException(e);
        }
    }

    public int getPort() {
        return server.getConnectors()[0].getPort();
    }

    public static String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "localhost";
        }
    }

    public String getURL() {
        return String.format("http://%s:%d", getHostName(), getPort());
    }

    public static WebServer start(int port, WebHandler handler) throws WebServerException {
        int retries = 5;
        int currentPort = port;
        Server server;
        BindException e;

        do {
            server = new Server(currentPort);
            server.setHandler(new JettyHandler(handler));
            try {
                server.start();
                e = null;
            } catch (BindException exception) {
                LocalResponseManager.chill(5);
                retries--;
                currentPort++;
                e = exception;
                Runner.LOGGER.warn(e.getMessage());
            } catch (Exception ex) {
                throw new WebServerException(ex);
            }
        } while (retries > 0 && e != null);

        return new WebServer(server);
    }

    public static final class JettyHandler extends AbstractHandler {
        public final WebHandler handler;

        public JettyHandler(WebHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(String s, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            jettyRequest.setHandled(true);
            try {
                handler.handle(request, response);
            } catch (Exception e) {
                // 501 -- internal error
                response.sendError(501, e.getMessage());
            }
        }
    }
}


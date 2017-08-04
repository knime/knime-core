package org.knime.core.jsonrpc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.knime.core.gateway.ServiceDefUtil;
import org.knime.core.gateway.server.KnimeGatewayServer;
import org.knime.core.gateway.v0.workflow.service.GatewayService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.googlecode.jsonrpc4j.JsonRpcMultiServer;

/**
 * Simple jetty json rpc service server. Obsolete as soon as the communication is established via the message queue.
 *
 * @author Martin Horn
 *
 */
public class JettyJsonRpcServer implements KnimeGatewayServer {

    private static final String DEFAULT_SERVICE_PACKAGE = "org.knime.core.gateway.serverproxy.service";

    private static final String DEFAULT_SERVICE_PREFIX = "Default";

    private Server m_server;

    @Override
    public void start(final int port) throws Exception {
        //create all default services and wrap them with the rest wrapper services
        Collection<Pair<String, String>> serviceDefs = ServiceDefUtil.getServices();
        Map<String, GatewayService> wrappedServices = new HashMap<String, GatewayService>();
        for (Pair<String, String> p : serviceDefs) {
            Class<?> defaultServiceClass;
            String defaultServiceFullClassName = DEFAULT_SERVICE_PACKAGE + "." + DEFAULT_SERVICE_PREFIX + p.getLeft();
            try {
                defaultServiceClass = Class.forName(defaultServiceFullClassName);
            } catch (ClassNotFoundException ex1) {
                throw new RuntimeException(
                    "No default service implementation not found (" + defaultServiceFullClassName + ")", ex1);
            }
            try {
                Class<GatewayService> wrapperServiceClass =
                    (Class<GatewayService>)org.knime.core.jsonrpc.serverproxy.ObjectSpecUtil
                        .getClassForFullyQualifiedName(p.getRight(), p.getLeft(), "jsonrpc-wrapper");
                Class<?> serviceInterface =
                    org.knime.core.gateway.ObjectSpecUtil.getClassForFullyQualifiedName(p.getRight(), p.getLeft(), "api");
                wrappedServices.put(p.getLeft(), wrapperServiceClass.getConstructor(serviceInterface)
                    .newInstance(defaultServiceClass.newInstance()));
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException
                    | ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
        start(port, wrappedServices);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        m_server.stop();
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public void startForTesting(final int port, final GatewayService... services) throws Exception {
        //wrap the passed services with the rest wrapper services
        Map<String, GatewayService> wrappedServices = new HashMap<String, GatewayService>();
        for (GatewayService s : services) {
            try {
                //TODO
                String namespace = ObjectSpecUtil.extractNamespaceFromClass(s.getClass(), "api");
                String name = ObjectSpecUtil.extractNameFromClass(s.getClass(), "api");
                Class<GatewayService> wrapperServiceClass =
                    (Class<GatewayService>)org.knime.core.jsonrpc.serverproxy.ObjectSpecUtil
                        .getClassForFullyQualifiedName(namespace, name, "jsonrpc-wrapper");
                Class<?> serviceInterface = ObjectSpecUtil.getClassForFullyQualifiedName(namespace, name, "api");
                wrappedServices.put(name, wrapperServiceClass.getConstructor(serviceInterface).newInstance(s));
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException
                    | ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
        start(port, wrappedServices);
    }

    private void start(final int port, final Map<String, GatewayService> services) throws Exception {
        m_server = new Server(port);
        m_server.setHandler(new MyHandler(services));
        m_server.start();
    }

    private static class MyHandler extends AbstractHandler {

        private JsonRpcMultiServer m_jsonRpcMultiServer;

        /**
         *
         */
        public MyHandler(final Map<String, GatewayService> services) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());

            JsonRpcUtil.addMixIns(mapper);
            m_jsonRpcMultiServer = new JsonRpcMultiServer(mapper);

            for (Entry<String, GatewayService> entry : services.entrySet()) {
                m_jsonRpcMultiServer.addService(entry.getKey(), entry.getValue());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, ServletException {
            m_jsonRpcMultiServer.handle(request, response);

            //inform jetty that this request has now been handled
            baseRequest.setHandled(true);

        }

    }

}

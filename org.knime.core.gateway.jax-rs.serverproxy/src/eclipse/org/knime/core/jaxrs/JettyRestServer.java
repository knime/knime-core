package org.knime.core.jaxrs;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.knime.core.gateway.server.KnimeGatewayServer;
import org.knime.core.gateway.serverproxy.util.DefaultServiceMap;
import org.knime.core.gateway.services.ServiceMap;
import org.knime.core.gateway.v0.workflow.service.GatewayService;
import org.knime.core.jaxrs.providers.json.EntityCollectionJSONDeserializer;
import org.knime.core.jaxrs.providers.json.EntityCollectionJSONSerializer;
import org.knime.core.jaxrs.providers.json.EntityJSONDeserializer;
import org.knime.core.jaxrs.providers.json.EntityJSONSerializer;
import org.knime.core.jaxrs.providers.json.MapJSONDeserializer;

/**
 * Simple jetty restful service server.
 *
 * TODO: synchronize
 *
 * @author Martin Horn
 *
 */
public class JettyRestServer implements KnimeGatewayServer {

    private Server m_server;

    @Override
    public void start(final int port) throws Exception {
        //create all default services and wrap them with the rest wrapper services
        List<GatewayService> services = ServiceMap.getAllServices().stream().map(s -> {
            Class<GatewayService> defaultServiceClass = DefaultServiceMap.getInstance().get(s);
            try {
                return RSWrapperServiceMap.getInstance().get(s).getConstructor(ServiceMap.getServiceInterface(s))
                    .newInstance(defaultServiceClass.newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
        start(port, services);
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
        //create all default services and wrap them with the rest wrapper services
        List<GatewayService> wrappedServices = Arrays.stream(services).map(s -> {
            try {
                String serviceName = ServiceMap.getServiceName(s.getClass());
                return RSWrapperServiceMap.getInstance().get(serviceName)
                    .getConstructor(ServiceMap.getServiceInterface(serviceName)).newInstance(s);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
        start(port, wrappedServices);
    }

    private void start(final int port, final Collection<GatewayService> services) throws Exception {
        Set<Object> resourceSingletons = new HashSet<>();
        resourceSingletons.addAll(services);
        resourceSingletons.add(new EntityJSONSerializer());
        resourceSingletons.add(new EntityJSONDeserializer());
        resourceSingletons.add(new EntityCollectionJSONSerializer());
        resourceSingletons.add(new EntityCollectionJSONDeserializer());
        resourceSingletons.add(new MapJSONDeserializer());

        CXFNonSpringJaxrsServlet context = new CXFNonSpringJaxrsServlet(resourceSingletons);

        ServletHolder servlet = new ServletHolder(context);
        ServletContextHandler handler = new ServletContextHandler();
        handler.addServlet(servlet, "/*");
        //        handler.setContextPath("/snapshot");

        m_server = new Server(port);
        m_server.setHandler(handler);
        m_server.start();
    }

}

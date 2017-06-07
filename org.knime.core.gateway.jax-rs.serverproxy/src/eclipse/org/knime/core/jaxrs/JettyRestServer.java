package org.knime.core.jaxrs;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.knime.core.gateway.ObjectSpecUtil;
import org.knime.core.gateway.ServiceDefUtil;
import org.knime.core.gateway.server.KnimeGatewayServer;
import org.knime.core.gateway.v0.workflow.service.GatewayService;
import org.knime.core.jaxrs.providers.OptionalParamConverter;
import org.knime.core.jaxrs.providers.exception.NoSuchElementExceptionMapper;
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

    private static final String DEFAULT_SERVICE_PACKAGE = "org.knime.core.gateway.serverproxy.service";

    private static final String DEFAULT_SERVICE_PREFIX = "Default";

    private Server m_server;

    @Override
    public void start(final int port) throws Exception {
        //create all default services and wrap them with the rest wrapper services
        Collection<Pair<String, String>> serviceDefs = ServiceDefUtil.getServices();
        List<GatewayService> services = serviceDefs.stream().map(sd -> {
            Class<?> defaultServiceClass;
            String defaultServiceFullClassName = DEFAULT_SERVICE_PACKAGE + "." + DEFAULT_SERVICE_PREFIX + sd.getLeft();
            try {
                defaultServiceClass = Class.forName(defaultServiceFullClassName);
            } catch (ClassNotFoundException ex1) {
                throw new RuntimeException(
                    "No default service implementation not found (" + defaultServiceFullClassName + ")", ex1);
            }
            try {
                Class<GatewayService> rsWrapperServiceClass =
                    (Class<GatewayService>)org.knime.core.jaxrs.serverproxy.ObjectSpecUtil
                        .getClassForFullyQualifiedName(sd.getRight(), sd.getLeft(), "restwrapper");
                Class<?> serviceInterface =
                    ObjectSpecUtil.getClassForFullyQualifiedName(sd.getRight(), sd.getLeft(), "api");
                return rsWrapperServiceClass.getConstructor(serviceInterface)
                    .newInstance(defaultServiceClass.newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException
                    | ClassNotFoundException ex) {
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
        //wrap the passed services with the rest wrapper services
        List<GatewayService> wrappedServices = Arrays.stream(services).map(s -> {
            try {
                //TODO
                String namespace = ObjectSpecUtil.extractNamespaceFromClass(s.getClass(), "api");
                String name = ObjectSpecUtil.extractNameFromClass(s.getClass(), "api");
                Class<GatewayService> rsWrapperServiceClass =
                    (Class<GatewayService>)org.knime.core.jaxrs.serverproxy.ObjectSpecUtil
                        .getClassForFullyQualifiedName(namespace, name, "restwrapper");
                Class<?> serviceInterface = ObjectSpecUtil.getClassForFullyQualifiedName(namespace, name, "api");
                return rsWrapperServiceClass.getConstructor(serviceInterface).newInstance(s);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException
                    | ClassNotFoundException ex) {
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
        resourceSingletons.add(new OptionalParamConverter());
        resourceSingletons.add(new NoSuchElementExceptionMapper());

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

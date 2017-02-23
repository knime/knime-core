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
import org.knime.core.gateway.codegen.spec.ObjectSpec;
import org.knime.core.gateway.codegen.spec.ServiceSpecs;
import org.knime.core.gateway.codegen.types.ObjectDef;
import org.knime.core.gateway.codegen.types.ServiceDef;
import org.knime.core.gateway.server.KnimeGatewayServer;
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

    public static final ObjectSpec RestWrapperServiceSpec =
        new ObjectSpec("restwrapper", "RSWrapper##name##", "org.knime.core.jaxrs", "");

    public final static ObjectSpec DefaultServiceSpec =
        new ObjectSpec("default", "Default##name##", "org.knime.core.gateway.serverproxy", "");

    private Server m_server;

    @Override
    public void start(final int port) throws Exception {
        //create all default services and wrap them with the rest wrapper services
        List<ServiceDef> serviceDefs = ObjectDef.readAll(ServiceDef.class);
        List<GatewayService> services = serviceDefs.stream().map(sd -> {
            try {
                Class<?> defaultServiceClass =
                    DefaultServiceSpec.getClassForFullyQualifiedName(sd.getNamespace(), sd.getName());
                Class<GatewayService> rsWrapperServiceClass = (Class<GatewayService>)RestWrapperServiceSpec
                    .getClassForFullyQualifiedName(sd.getNamespace(), sd.getName());
                Class<?> serviceInterface =
                    ServiceSpecs.Api.getClassForFullyQualifiedName(sd.getNamespace(), sd.getName());
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
                String namespace = ServiceSpecs.Api.extractNamespaceFromClass(s.getClass());
                String name = ServiceSpecs.Api.extractNameFromClass(s.getClass());
                Class<GatewayService> rsWrapperServiceClass =
                    (Class<GatewayService>)RestWrapperServiceSpec.getClassForFullyQualifiedName(namespace, name);
                Class<?> serviceInterface = ServiceSpecs.Api.getClassForFullyQualifiedName(namespace, name);
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

package org.knime.core.jaxrs;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.knime.core.jaxrs.codegen.RSWorkflowService;

import com.knime.enterprise.server.rest.providers.json.GenericJSONSerializer;

/**
 * Singleton tile server (and possibly more, e.g. providing the underlying data tables etc.)
 *
 * TODO: synchronize
 *
 * @author Martin Horn
 *
 */
public class JettyRestServer {

    private static JettyRestServer INSTANCE = null;

    private JettyRestServer() throws Exception {
        ServletHolder sh = new ServletHolder(ServletContainer.class);
        //        sh.setInitParameter("jersey.config.property.resourceConfigClass",
        //                            "jersey.api.core.PackagesResourceConfig");
        //        sh.setInitParameter("jersey.config.property.packages", "org.knime.knip.js.imgtableserver.rest");//Set the package where the services reside
        sh.setInitParameter("jersey.config.server.provider.classnames",
                            String.join(",", RSWorkflowService.class.getCanonicalName(), GenericJSONSerializer.class.getCanonicalName()));
        //        sh.setInitParameter("jersey.api.json.POJOMappingFeature", "true");

        Server server = new Server(3000);
        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        server.setHandler(context);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server.start();

    }

    /**
     * Gets the tile server instance.
     *
     * @return
     * @throws Exception an exception if something got wrong on starting the server (if accessed for the first time)
     */
    public static void start() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new JettyRestServer();
        }
    }
}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ----------------------------------------------------------------------------
 */
package org.knime.workbench.helpview;

import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.log.Log;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class HelpviewPlugin extends AbstractUIPlugin {
    private static final NodeLogger logger = NodeLogger
            .getLogger(HelpviewPlugin.class);

    // The shared instance.
    private static HelpviewPlugin plugin;

    private Server m_server;

    private static final int PORT = 51176; // if you change this value be sure
                                           // to also change it in
                                           // FullNodeDescription.xslt!

    /**
     * The constructor.
     */
    public HelpviewPlugin() {
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     *
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        // initialize jetty's logging system
        if (System.getProperty("org.mortbay.log.class") == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(JettyLogger.class.getClassLoader());
                System.setProperty("org.mortbay.log.class",JettyLogger.class.getName());
                Log.getLog();
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        // start an embedded webserver for serving bundle-local or node-local
        // files in the node descriptions
        m_server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(PORT);
        try {
            connector.setHost(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException ex) {
            // sometimes the above returns this exception; yeah, strange
            try {
                connector.setHost(InetAddress.getByName("localhost").getHostAddress());
            } catch (UnknownHostException ex1) {
                logger.error("Cannot start embedded webserver: " + ex.getMessage(),
                        ex);
            }
        }
        m_server.addConnector(connector);
        m_server.addHandler(FileHandler.instance);
        try {
            m_server.start();
        } catch (BindException ex) {
            logger.error("Cannot start embedded webserver: " + ex.getMessage(),
                    ex);
            // ignore exception otherwise the plugin is not initialized
        }
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        m_server.stop();
        plugin = null;
    }

    /**
     * Returns the shared instance.
     *
     * @return the shared instance.
     */
    public static HelpviewPlugin getDefault() {
        return plugin;
    }
}

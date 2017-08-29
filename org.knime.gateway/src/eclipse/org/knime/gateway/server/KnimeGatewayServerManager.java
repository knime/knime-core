/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 25, 2017 (hornm): created
 */
package org.knime.gateway.server;

import java.util.List;

import org.knime.core.util.ExtPointUtils;
import org.knime.gateway.workflow.service.GatewayService;

/**
 * Manager for {@link KnimeGatewayServer} provided by the respective extension point (see {@link KnimeGatewayServer}).
 *
 * @author Martin Horn, University of Konstanz
 */
public class KnimeGatewayServerManager {

    private static List<KnimeGatewayServer> SERVERS = null;

    private KnimeGatewayServerManager() {
        //utility class
    }

    /**
     * Starts all registered server.
     *
     * @param port
     * @throws Exception
     */
    public static void startAll(final int port) throws Exception {
        collectServers();
        for (KnimeGatewayServer s : SERVERS) {
            s.start(port);
        }
    }

    /**
     * Stops all registered server.
     *
     * @throws Exception
     */
    public static void stopAll() throws Exception {
        if (SERVERS == null) {
            throw new IllegalStateException("No servers have been initialized and started.");
        }
        for (KnimeGatewayServer s : SERVERS) {
            s.stop();
        }
    }

    /**
     * Starts all registered server for testing. The passed services (that are very likely mocked services) are going to
     * be wrapped and used.
     *
     * @param port
     * @param services
     * @throws Exception
     */
    public static void startAllForTesting(final int port, final GatewayService... services) throws Exception {
        collectServers();
        for (KnimeGatewayServer s : SERVERS) {
            s.startForTesting(port, services);
        }
    }

    private static void collectServers() {
        if (SERVERS == null) {
            SERVERS = ExtPointUtils.collectExecutableExtensions(KnimeGatewayServer.EXT_POINT_ID,
                KnimeGatewayServer.EXT_POINT_ATTR);
        }
    }

}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Aug 6, 2020 (hornm): created
 */
package org.knime.core.node.rpc.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.knime.core.node.rpc.RpcClient;
import org.knime.core.node.rpc.RpcServer;
import org.knime.core.node.rpc.RpcSingleClient;
import org.knime.core.node.rpc.RpcTransport;
import org.knime.core.util.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility methods for de-/serialization testing of data service calls and their results for json-rpc server and client
 * implementations.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class JsonRpcTestUtil {

    private JsonRpcTestUtil() {
        // utility class
    }

    /**
     * Creates an {@link RpcSingleClient} for testing purposes. Call {@link RpcSingleClient#getService()} to test the
     * de-/serialization of your service.
     *
     * @param <S>
     * @param serviceInterface
     * @param handler
     * @param mapper if <code>null</code> the default mapper will be used
     * @return the rpc client for testing purposes
     */
    public static <S> RpcSingleClient<S> createRpcSingleClientInstanceForTesting(final Class<S> serviceInterface,
        final S handler, final ObjectMapper mapper) {
        JsonRpcSingleServer<S> server =
            mapper == null ? new JsonRpcSingleServer<>(handler) : new JsonRpcSingleServer<>(handler, mapper);
        return createRpcSingleClientInstanceForTesting(serviceInterface, mapper, new TestRpcTransport(server));
    }

    /**
     * Creates a {@link RpcSingleClient} for testing purposes.
     *
     * @param serviceInterface
     * @param mapper an optional custom mapper or <code>null</code>
     * @param rpcTransport a custom transport implementation
     * @param <S>
     * @return the rpc client for testing purposes
     */
    public static <S> RpcSingleClient<S> createRpcSingleClientInstanceForTesting(final Class<S> serviceInterface,
        final ObjectMapper mapper, final RpcTransport rpcTransport) {
        return new JsonRpcSingleClient<>(serviceInterface, mapper, rpcTransport);
    }

    /**
     * Creates a {@link RpcClient} for testing purposes. Call {@link RpcClient#getService(Class)} to test the
     * de-/serialization of your service.
     *
     * @param mapper if <code>null</code> the default mapper will be used
     * @param serviceInterfaceAndHandler the service interface and handler to register with the test server
     * @return the rpc client for testing purposes
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static RpcClient createRpcClientInstanceForTesting(final ObjectMapper mapper,
        final Pair<Class, Object>... serviceInterfaceAndHandler) {
        JsonRpcServer server = mapper == null ? new JsonRpcServer() : new JsonRpcServer(mapper);
        for (Pair<Class, Object> p : serviceInterfaceAndHandler) {
            server.addService(p.getFirst(), p.getSecond());
        }
        return createRpcClientInstanceForTesting(mapper, new TestRpcTransport(server));
    }

    /**
     * Creates a {@link RpcClient} for testing purposes.
     *
     * @param mapper an optional custom mapper or <code>null</code>
     * @param rpcTransport a custom transport implementation
     * @return the rpc client for testing purposes
     */
    public static RpcClient createRpcClientInstanceForTesting(final ObjectMapper mapper,
        final RpcTransport rpcTransport) {
        return new JsonRpcClient(mapper, rpcTransport);
    }

    private static final class TestRpcTransport implements RpcTransport {

        private RpcServer m_server;

        private TestRpcTransport(final RpcServer server) {
            m_server = server;
        }

        @Override
        public String sendAndReceive(final String rpc) {
            try (ByteArrayInputStream request = new ByteArrayInputStream(rpc.getBytes(StandardCharsets.UTF_8));
                    ByteArrayOutputStream response = new ByteArrayOutputStream()) {
                m_server.handleRequest(request, response);
                return new String(response.toByteArray(), StandardCharsets.UTF_8.name());
            } catch (IOException ex) {
                throw new IllegalStateException("I/O exception during node data service rpc request handling", ex);
            }
        }

    }
}

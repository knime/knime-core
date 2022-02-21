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
 *   Jul 14, 2020 (carlwitt): created
 */
package org.knime.core.webui.data.rpc.json.impl;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.CUSTOM_SERVER_ERROR_UPPER;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.data.rpc.RpcServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcMultiServer;

/**
 * A wrapper for the {@link JsonRpcMultiServer}; a simple delegate in case we want to exchange the JSON-RPC
 * implementation.
 *
 * The service interface and their handler registered with this server need to either follow a certain convention or a
 * custom {@link ObjectMapper} (for de-/serialization) needs to be provided. For more details see
 * {@link JsonRpcSingleClient}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 4.3
 */
public class JsonRpcServer implements RpcServer {

    /**
     * Node data service implementations. Can be local (directly execute using node model in the same JVM) or remote
     * (execute using remote node model). Simply records what is added to {@link #m_jsonRpcServer}, since the API
     * doesn't have a getService() method.
     */
    private final Map<String, Object> m_handlers = new HashMap<>();

    private final JsonRpcMultiServer m_jsonRpcServer;

    /**
     * JSON-RPC server with default object mapper.
     */
    public JsonRpcServer() {
        this(ObjectMapperUtil.getInstance().getObjectMapper());
    }

    /**
     * @param mapper allows customized serialization of java objects into JSON
     */
    public JsonRpcServer(final ObjectMapper mapper) {
        CheckUtils.checkNotNull(mapper, "Object mapper passed to JSON-RPC server must not be null.");
        m_jsonRpcServer = new JsonRpcMultiServer(mapper);
        m_jsonRpcServer.setErrorResolver(
            (t, method, arguments) -> new JsonError(CUSTOM_SERVER_ERROR_UPPER, t.getMessage(), new ErrorData(t)));
    }

    /**
     * Adds a new service handler to the server. The simple class name of the service interface is used as service name.
     * The service name is used in the JSON-RPC request method property to specify the service to use (e.g.
     * <code>MyService.theMethodToCall</code>)
     *
     * @param <S> the type of the handler
     * @param serviceInterface the interface implemented by the handler (i.e. node data service)
     * @param handler the handler implementation
     */
    public <S> void addService(final Class<S> serviceInterface, final S handler) {
        addService(serviceInterface.getSimpleName(), handler);
    }

    /**
     * Adds a new service handler to the server.
     *
     * @param <S> the type of the handler
     * @param serviceName the unique name for the service (the name must be stated in the method field of the JSON-RPC
     *            request, e.g., <code>"method": "ServiceName.someMethod"</code>)
     * @param handler the handler implementation
     */
    public <S> void addService(final String serviceName, final S handler) {
        CheckUtils.checkNotNull(handler, "Service implementation passed to JSON-RPC server must not be null.");
        m_jsonRpcServer.addService(serviceName, handler);
        m_handlers.put(serviceName, handler);
    }

    /**
     * Handles a single request from the given InputStream, that is, a single JsonNode is read from the stream and
     * treated as a JSON-RPC request. All responses are written to the given OutputStream. The method to call needs to
     * include the simple name of the service interface, see {@link #addService(Class, Object)}.
     */
    @Override
    public void handleRequest(final InputStream in, final OutputStream out) throws IOException {
        m_jsonRpcServer.handleRequest(in, out);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> S getHandler(final String serviceName) {
        return (S)m_handlers.get(serviceName);
    }

}

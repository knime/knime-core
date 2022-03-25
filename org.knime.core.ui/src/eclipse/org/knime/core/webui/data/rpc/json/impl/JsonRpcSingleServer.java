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

import static org.knime.core.webui.data.rpc.json.impl.JsonRpcServer.INTERNAL_ERROR_CODE;
import static org.knime.core.webui.data.rpc.json.impl.JsonRpcServer.USER_ERROR_CODE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.data.DataServiceException;
import org.knime.core.webui.data.rpc.RpcSingleServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;

/**
 * A convenience specialization of {@link JsonRpcServer} that supports only one service interface and thus spares the
 * user from specifying the service interface name every time.
 *
 * If this single json rpc server is used, only the method name is required to address the node data service in the
 * JSON-RPC request from the client.
 *
 * @param <S> the node data service interface type; defines which methods are offered by the node model to retrieve
 *            data. See also {@link JsonRpcClient} for requirements on this interface.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 4.3
 */
public class JsonRpcSingleServer<S> implements RpcSingleServer<S> {

    /**
     * Node data service implementor. Can be local (node model lives in the same JVM as client) or remote.
     */
    private S m_handler;

    private com.googlecode.jsonrpc4j.JsonRpcServer m_jsonRpcServer;

    /**
     * @param handler implementation of the node data service interface
     */
    public JsonRpcSingleServer(final S handler) {
        this(handler, ObjectMapperUtil.getInstance().getObjectMapper());
    }

    /**
     * @param handler implementation of the node data service interface
     * @param mapper allows customized serialization of java objects into JSON
     */
    public JsonRpcSingleServer(final S handler, final ObjectMapper mapper) {
        CheckUtils.checkNotNull(mapper, "Object mapper passed to JSON-RPC server must not be null.");
        m_handler = CheckUtils.checkNotNull(handler, "The node data service implementation must not be null.");
        m_jsonRpcServer = new com.googlecode.jsonrpc4j.JsonRpcServer(mapper, handler);
        m_jsonRpcServer.setErrorResolver((t, method, arguments) -> t instanceof DataServiceException
            ? new JsonError(USER_ERROR_CODE, t.getMessage(), new JsonRpcUserErrorData((DataServiceException)t))
            : new JsonError(INTERNAL_ERROR_CODE, t.getMessage(), new JsonRpcInternalErrorData(t)));
    }

    @Override
    public void handleRequest(final InputStream in, final OutputStream out) throws IOException {
        m_jsonRpcServer.handleRequest(in, out);
    }

    @Override
    public S getHandler() {
        return m_handler;
    }
}

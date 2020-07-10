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
 *   Jul 13, 2020 (hornm): created
 */
package org.knime.core.node.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.rpc.json.JsonRpcClient;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;

/**
 * Base class for node data service client implementations, such as {@link JsonRpcClient}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @param <T> the node model's service interface; see {@link #getServiceInterface()}
 */
public abstract class AbstractRpcClient<T> implements RpcClient<T> {

    /**
     * The data retrieval interface offered to the node dialog/view by the node model. This interface is defined and
     * implemented by the node developer.
     */
    private final Class<T> m_serviceInterface;

    /**
     * An instance of the {@link #getServiceInterface()} that executes each method remotely. Is null if the execution is
     * local.
     */
    private T m_remoteProxyServiceInterface;

    /**
     * Used to transport the serialized remote procedure call to a remote node model.
     */
    private final RpcTransport m_rpcTransport;

    /**
     * If the node model lives in the same JVM, the optional contains the service implementation. If the node model is
     * on a different machine or in a different language, the optional is empty, in which case
     * {@link #callServiceWithRes(Function)} will forward requests of the node dialog/view to a remote server.
     */
    private Optional<RpcServer<T>> m_rpcServer;

    /**
     * @param serviceInterface the data retrieval interface offered to the node dialog/view by the node model. This
     *            interface is defined and implemented by the node developer.
     */
    public AbstractRpcClient(final Class<T> serviceInterface) {
        m_serviceInterface = serviceInterface;
        m_rpcTransport = RpcTransportRegistry.getRpcMessageTransportFactory().createRpcTransport();
    }

    /**
     * Converts a call to the node model's data service interface to String.<br/>
     * For instance, method countChars("abc", 'b') on a ServiceInterface could be represented in JSON-RPC as {"jsonrpc":
     * "2.0", "method": "countChars", "params": ["abc", "b"], "id": 3}
     * @param method name of the method to call, as provided by the interface {@link #getServiceInterface()}
     * @param args method parameters for the invocation
     * @return the representation of the method call
     */
    protected abstract String convertCall(Method method, Object[] args);

    /**
     * Counterpart for {@link #convertCall(Method, Object[])}, parses response and converts back to the method return
     * type declared by the service interface {@link #getServiceInterface()}.
     * @param response the result of the remote procedure call
     * @param valueType the return type of the method invoked via remote procedure call
     * @param <R> the return type of the method invoked via remote procedure call
     * @return the result as a java object of the specified type
     */
    protected abstract <R> R convertResult(String response, final Object valueType);

    /**
     * @see #m_rpcServer
     */
    private Optional<RpcServer<T>> getRpcServer() {
        if (m_rpcServer == null) {
            m_rpcServer = Optional.ofNullable(createRpcServer());
        }
        return m_rpcServer;
    }

    /**
     * Determine whether we're working local or remote.
     *
     * @return the node model's server if working local or null if working remotely.
     */
    private <N extends NodeModel> RpcServer<T> createRpcServer() {
        NodeContainer nc = NodeContext.getContext().getNodeContainer();
        if (nc instanceof NativeNodeContainer) {
            N nodeModel = (N)((NativeNodeContainer)nc).getNodeModel();

            NodeFactory<NodeModel> factory = ((NativeNodeContainer)nc).getNode().getFactory();
            if (factory instanceof RpcServerFactory) {
                RpcServer<T> server = ((RpcServerFactory<N, T>)factory).createRpcServer(nodeModel);
                return server;
            }
        }
        return null;
    }

    @Override
    public Future<Void> callService(final Consumer<T> serviceEvaluator) {
        return callServiceWithRes(s -> {
            serviceEvaluator.accept(s);
            return null;
        });
    }

    @Override
    public <R> Future<R> callServiceWithRes(final Function<T, R> serviceEvaluator) {

        T service = getRpcServer()
            .map(RpcServer<T>::getHandler)
            .orElseGet(this::getRemoteProxy);

        CompletableFuture<R> result = new CompletableFuture<>();
        result.complete(serviceEvaluator.apply(service));
        return result;
    }

    /**
     * @return an instance of the node data service interface that forwards each call to a remote node model.
     */
    T getRemoteProxy() {
        if (m_remoteProxyServiceInterface == null) {
            m_remoteProxyServiceInterface = (T)Proxy.newProxyInstance(
                m_serviceInterface.getClassLoader(),
                new Class[]{m_serviceInterface},
                this::handleInvocation);
        }
        return m_remoteProxyServiceInterface;
    }

    /**
     * An {@link InvocationHandler} that forwards a call to the {@link #getServiceInterface()} to a remote node model.
     * @param proxy only for signature compatibility with {@link InvocationHandler} functional interface
     * @param method the method invoked on the {@link #getServiceInterface()}
     * @param params the parameters passed to the method invocation
     * @return the response of the node model, after deserialization
     */
    private Object handleInvocation(final Object proxy, final Method method, final Object[] params) {
        String request = convertCall(method, params);
        String response = m_rpcTransport.sendAndReceive(request);
        // TODO save some memory via streaming? e.g., new PipedOutputStream();
        return convertResult(response, method.getGenericReturnType());
    }

}

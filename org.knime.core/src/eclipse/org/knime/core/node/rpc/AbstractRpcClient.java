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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
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
 */
public abstract class AbstractRpcClient implements RpcClient {

    /**
     * Used to transport the serialized remote procedure call to a remote node model.
     */
    private final RpcTransport m_rpcTransport;

    /**
     * If the node model lives in the same JVM, the optional contains the service implementation. If the node model is
     * on a different machine or in a different language, the optional is empty, in which case
     * {@link #callServiceWithRes(Function)} will forward requests of the node dialog/view to a remote server. The field
     * is null until the first access triggers an invocation of {@link #createRpcServer()} which returns an Optional.
     */
    private Optional<RpcServer> m_rpcServer;

    /**
     * A map of service interfaces to a proxy-implementation of them for remote service calls. Serves as a cache of the
     * proxy instances.
     */
    private final Map<Class<?>, Object> m_remoteProxyServiceHandler;

    /**
     * A debug flag for node developers to simplify testing serialization.
     */
    private boolean m_alwaysSerializeForTesting = false;

    /**
     *
     */
    public AbstractRpcClient() {
        m_rpcTransport = RpcTransportRegistry.getRpcMessageTransportFactory().createRpcTransport();
        m_remoteProxyServiceHandler = new HashMap<>();
    }

    /**
     * Constructor to initialize a rpc client for testing purposes only.
     *
     * @param rpcTransport a custom rpc transport for testing
     */
    protected AbstractRpcClient(final RpcTransport rpcTransport) {
        m_rpcTransport = rpcTransport;
        m_rpcServer = Optional.empty();
        m_alwaysSerializeForTesting = true;
        m_remoteProxyServiceHandler = new HashMap<>();
    }

    /**
     * Converts a call to the node model's data service interface to String.<br/>
     * For instance, method countChars("abc", 'b') on a ServiceInterface could be represented in JSON-RPC as {"jsonrpc":
     * "2.0", "method": "countChars", "params": ["abc", "b"], "id": 3}
     * @param serviceName the name of the service to be called
     * @param method name of the method to call, as provided by the interface this client is generically typed to
     * @param args method parameters for the invocation
     * @return the representation of the method call
     */
    protected abstract String convertCall(String serviceName, Method method, Object[] args);

    /**
     * Counterpart for {@link #convertCall(String, Method, Object[])}, parses response and converts back to the method return
     * type declared by the service interface this client is generically typed to.
     *
     * @param response the result of the remote procedure call
     * @param valueType the return type of the method invoked via remote procedure call, e.g., as returned by
     *            {@code SomeInterface.class.getMethod("someMethodName").getReturnType()}
     * @param <R> the return type of the method invoked via remote procedure call
     * @return the result as a java object of the specified type
     * @throws Exception an exception thrown by the rpc client implementation when processing the request
     */
    protected abstract <R> R convertResult(String response, final Type valueType) throws Exception; //NOSONAR

    /**
     * @see #m_rpcServer
     */
    private Optional<RpcServer> getRpcServer() {
        // if the field is null, we haven't tried to create a server before
        // if the field is an empty Optional, we called it before and didn't get a local handler (working remotely)
        if (m_rpcServer == null) { //NOSONAR -- to avoid repeatedly calling createRpcServer
            m_rpcServer = createRpcServer();
        }
        return m_rpcServer;
    }

    /**
     * @return the node model's server if working local or an empty optional if working remotely.
     */
    @SuppressWarnings("unchecked")
    private <N extends NodeModel> Optional<RpcServer> createRpcServer() {
        NodeContainer nc = NodeContext.getContext().getNodeContainer();
        if (nc instanceof NativeNodeContainer) {
            N nodeModel = (N)((NativeNodeContainer)nc).getNodeModel();

            NodeFactory<NodeModel> factory = ((NativeNodeContainer)nc).getNode().getFactory();
            return Optional.of(getRpcServerFromNodeFactory(nodeModel, factory));
        }
        return Optional.empty();
    }

    /**
     * Creates an rpc server from the provided node factory.
     *
     * @param nodeModel
     * @param factory
     * @param <N>
     * @return the server, never <code>null</code>
     */
    @SuppressWarnings("unchecked")
    protected <N extends NodeModel> RpcServer getRpcServerFromNodeFactory(final N nodeModel,
        final NodeFactory<NodeModel> factory) {
        if (factory instanceof RpcServerFactory) {
            return ((RpcServerFactory<N>)factory).createRpcServer(nodeModel);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> S getService(final Class<S> serviceInterface, final String serviceName) {
        return (S)getRpcServer().map(s -> s.getHandler(serviceName))
            .orElseGet(() -> getRemoteProxy(serviceInterface, serviceName));
    }

    @Override
    public <S> Future<Void> callService(final Class<S> serviceInterface, final String serviceName,
        final Consumer<S> serviceEvaluator) {
        return callServiceWithRes(serviceInterface, serviceName, s -> {
            serviceEvaluator.accept(s);
            return null;
        });
    }

    @Override
    public <S, R> Future<R> callServiceWithRes(final Class<S> serviceInterface, final String serviceName,
        final Function<S, R> serviceEvaluator) {
        S service = getService(serviceInterface);

        CompletableFuture<R> result = new CompletableFuture<>();
        result.complete(serviceEvaluator.apply(service));
        return result;
    }

    /**
     * @param serviceInterface
     * @param serviceName
     * @return an instance of the node data service interface that forwards each call to a remote node model.
     */
    @SuppressWarnings("unchecked")
    private <S> S getRemoteProxy(final Class<S> serviceInterface, final String serviceName) {
        Object proxy = m_remoteProxyServiceHandler.computeIfAbsent(serviceInterface,
            k -> createRemoteProxyHandler(k, serviceName));
        return (S)proxy;
    }

    @SuppressWarnings("unchecked")
    private <S> S createRemoteProxyHandler(final Class<S> serviceInterface, final String serviceName) {
        return (S)Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[]{serviceInterface},
            (proxy, method, params) -> {
                String request = convertCall(serviceName, method, params);
                String response = m_rpcTransport.sendAndReceive(request);
                // TODO save some memory via streaming? e.g., new PipedOutputStream() //NOSONAR
                return convertResult(response, method.getGenericReturnType());
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnectedRemotely() {
        return getRpcServer().isPresent();
    }

}

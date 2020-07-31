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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
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
 * @param <T> the node model's service interface that defines the methods that can be used by the dialog/view to obtain
 *            data from it
 */
public abstract class AbstractRpcClient<T> implements RpcClient<T> {

    /**
     * The data retrieval interface offered to the node dialog/view by the node model. This interface is defined and
     * implemented by the node developer.
     */
    private final Class<T> m_serviceInterface;

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
    private Optional<RpcServer<T>> m_rpcServer;

    /**
     * An instance of the {@link #getServiceInterface()} that executes each method remotely. Is null if the execution is
     * local.
     */
    private T m_remoteProxyServiceInterface;

    /**
     * A debug flag for node developers to simplify testing serialization.
     */
    private boolean m_alwaysSerializeForTesting = false;

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
     * @param method name of the method to call, as provided by the interface this client is generically typed to
     * @param args method parameters for the invocation
     * @return the representation of the method call
     */
    protected abstract String convertCall(Method method, Object[] args);

    /**
     * Counterpart for {@link #convertCall(Method, Object[])}, parses response and converts back to the method return
     * type declared by the service interface this client is generically typed to.
     *
     * @param response the result of the remote procedure call
     * @param valueType the return type of the method invoked via remote procedure call, e.g., as returned by
     *            {@code SomeInterface.class.getMethod("someMethodName").getReturnType()}
     * @param <R> the return type of the method invoked via remote procedure call
     * @return the result as a java object of the specified type
     * @throws Exception an exception thrown by the node model why processing the request.
     */
    protected abstract <R> R convertResult(String response, final Type valueType) throws Exception;

    /**
     * @see #m_rpcServer
     */
    private Optional<RpcServer<T>> getRpcServer() {
        // if the field is null, we haven't called tried to create a server before;
        // if the field is an empty Optional, we called it before and didn't get a local handler (working remotely)
        if (m_rpcServer == null) { //NOSONAR -- to avoid repeatedly calling createRpcServer
            m_rpcServer = Optional.ofNullable(createRpcServer());
        }
        return m_rpcServer;
    }

    /**
     * Determine whether we're working local or remote.
     *
     * @return the node model's server if working local or null if working remotely.
     */
    @SuppressWarnings("unchecked")
    private <N extends NodeModel> RpcServer<T> createRpcServer() {
        NodeContainer nc = NodeContext.getContext().getNodeContainer();
        if (nc instanceof NativeNodeContainer) {
            N nodeModel = (N)((NativeNodeContainer)nc).getNodeModel();

            NodeFactory<NodeModel> factory = ((NativeNodeContainer)nc).getNode().getFactory();
            if (factory instanceof RpcServerFactory) {
                return ((RpcServerFactory<N, T>)factory).createRpcServer(nodeModel);
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

        if(isAlwaysSerializeForTesting()) {
            return handleLocalWithSerialization(serviceEvaluator);
        }

        T service = getRpcServer()
            .map(RpcServer<T>::getHandler)
            .orElseGet(this::getRemoteProxy);

        CompletableFuture<R> result = new CompletableFuture<>();
        result.complete(serviceEvaluator.apply(service));
        return result;
    }

    /**
     * Only to make testing for node developers easier. Used when {@link #isAlwaysSerializeForTesting()} is on for
     * testing. Uses the {@link RpcServer} provided by the node model's factory to handle the request, but uses a simple
     * local InputStream/OutputStream to handle the request instead of the remote {@link RpcTransport}.
     *
     * @param serviceEvaluator
     * @return
     */
    private <R> Future<R> handleLocalWithSerialization(final Function<T, R> serviceEvaluator) {

        if(m_rpcServer == null) { //NOSONAR
            m_rpcServer = getRpcServer();
        }

        @SuppressWarnings("unchecked")
        T debugServiceProxy = (T)Proxy.newProxyInstance(m_serviceInterface.getClassLoader(),
            new Class[]{m_serviceInterface}, this::handleInvocationLocalTesting);

        R result = serviceEvaluator.apply(debugServiceProxy);
        CompletableFuture<R> future = new CompletableFuture<>();
        future.complete(result);
        return future;

    }

    /**
     * @return an instance of the node data service interface that forwards each call to a remote node model.
     */
    @SuppressWarnings("unchecked")
    private T getRemoteProxy() {
        if (m_remoteProxyServiceInterface == null) {
            m_remoteProxyServiceInterface = (T)Proxy.newProxyInstance(
                m_serviceInterface.getClassLoader(),
                new Class[]{m_serviceInterface},
                this::handleInvocationRemote);
        }
        return m_remoteProxyServiceInterface;
    }

    /**
     * An {@link InvocationHandler} that forwards a call to the {@link #getServiceInterface()} to a remote node model.
     *
     * @param proxy only for signature compatibility with {@link InvocationHandler} functional interface
     * @param method the method invoked on the node data service interface
     * @param params the parameters passed to the method invocation
     * @return the response of the node model, after deserialization
     * @throws Exception an exception thrown by the node model while handling the request (the exception instance is
     *             unmarshalled after transport)
     */
    private Object handleInvocationRemote(final Object proxy, final Method method, final Object[] params)
        throws Exception {
        String request = convertCall(method, params);
        String response = m_rpcTransport.sendAndReceive(request);
        // TODO save some memory via streaming? e.g., new PipedOutputStream();
        return convertResult(response, method.getGenericReturnType());
    }

    /**
     * A debug service invocation handler for {@link #handleLocalWithSerialization(Function)}.
     *
     * @param proxy only for signature compatibility with {@link InvocationHandler} functional interface
     */
    private Object handleInvocationLocalTesting(final Object proxy, final Method method, final Object[] params)
        throws Exception {

        NodeLogger debugLogger = NodeLogger.getLogger(AbstractRpcClient.class);

        final RpcServer<T> localServer = m_rpcServer
            .orElseThrow(() -> new IllegalStateException("Can not test serialization locally when working remotely."));

        String requestString = convertCall(method, params);
        debugLogger.debug(
            String.format("[Node data service local testing] Remote procedure call to node model : %s", requestString));
        Charset defaultCharset = Charset.defaultCharset();
        try (ByteArrayInputStream request = new ByteArrayInputStream(requestString.getBytes(defaultCharset));
                ByteArrayOutputStream response = new ByteArrayOutputStream()) {
            localServer.handleRequest(request, response);
            debugLogger
                .debug(String.format("[Node data service local testing] Node model response: %s", requestString));
            Object result = convertResult(response.toString(defaultCharset.name()), method.getGenericReturnType());
            debugLogger.debug(String.format("[Node data service local testing] Object unmarshalled from response: %s",
                requestString));
            return result;
        }
    }

    /**
     * For testing. Enable serialization when working locally in order to check whether it works correctly without a server and executor in place to
     * actually perform a remote procedure call.
     * @param alwaysSerializeForTesting
     */
    public void setAlwaysSerializeForTesting(final boolean alwaysSerializeForTesting) {
        m_alwaysSerializeForTesting = alwaysSerializeForTesting;
    }

    /**
     * @return whether the {@link #setAlwaysSerializeForTesting(boolean)} debug flag is set
     */
    public boolean isAlwaysSerializeForTesting() {
        return m_alwaysSerializeForTesting;
    }

}

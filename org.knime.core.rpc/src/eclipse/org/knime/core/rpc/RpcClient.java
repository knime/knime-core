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
package org.knime.core.rpc;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An {@link RpcClient} implementation is used by a node dialog or node view to retrieve data from its (possibly remote)
 * node model.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 4.3
 */
public interface RpcClient {

    /**
     * Calls a method on the node model's data service implementation of the interface and returns the result. Assumes
     * that the service is registered under the name <code>serviceInterface.getSimpleName()</code>. Example usage:
     *
     * <pre>
     * RpcClient m_rpcClient = ...
     * Future&lt;List&lt;SomeType>> future =
     *      m_rpcClient.callServiceWithRes(MyService.class, service -> service.getSomeData(someParameter));
     * try {
     *     List&lt;SomeType> results = future.get(3, TimeUnit.SECONDS);
     * } catch (TimeoutException timeoutException) {
     *     ...
     * </pre>
     *
     * @param <S> the interface type of the service to use
     * @param <R> the result type of the invoked method
     * @param serviceEvaluator the service evaluator is given an implementation of the node model's data service. It
     *            then calls one of the methods on the node model's service interface and returns the result.
     * @param serviceInterface the interface of the service to call to
     * @return a {@link Future} containing the result of the invoked method.
     */
    default <S, R> Future<R> callServiceWithRes(final Class<S> serviceInterface,
        final Function<S, R> serviceEvaluator) {
        return callServiceWithRes(serviceInterface, serviceInterface.getSimpleName(), serviceEvaluator);
    }

    /**
     * Calls a method on the node model's data service implementation of the given interface registered under the given
     * name and returns the result. Example usage:
     *
     * <pre>
     * RpcClient m_rpcClient = ...
     * Future&lt;List&lt;SomeType>> future =
     *      m_rpcClient.callServiceWithRes(MyService.class, "MyServiceInstance3", s -> s.getSomeData(someParameter));
     * try {
     *     List&lt;SomeType> results = future.get(3, TimeUnit.SECONDS);
     * } catch (TimeoutException timeoutException) {
     *     ...
     * </pre>
     *
     * @param <S> the interface type of the service to use
     * @param <R> the result type of the invoked method
     * @param serviceInterface the interface of the service to call to
     * @param serviceName the name under which the service interface is registered at the server, see
     *            {@link RpcServer#getHandler(String)}
     * @param serviceEvaluator the service evaluator is given an implementation of the node model's data service. It
     *            then calls one of the methods on the node model's service interface and returns the result.
     * @return the result
     */
    <S, R> Future<R> callServiceWithRes(Class<S> serviceInterface, String serviceName, Function<S, R> serviceEvaluator);

    /**
     * Similar to {@link #callServiceWithRes(Class, Function)} but for void methods. For instance:
     * {@code Future<Void> future = m_rpcClient.callService(nodeDataService -> nodeDataService.sendSomeData(someData));}
     *
     * @param <S> the interface type of the service to use
     * @param serviceInterface the interface of the service to call to
     * @param serviceConsumer used to invoke the method on the service interface
     * @return an empty future
     */
    default <S> Future<Void> callService(final Class<S> serviceInterface, final Consumer<S> serviceConsumer) {
        return callService(serviceInterface, serviceInterface.getSimpleName(), serviceConsumer);
    }

    /**
     * See {@link #callService(Class, Consumer)}.
     *
     * @param <S> the interface type of the service to use
     * @param serviceInterface the interface of the service to call
     * @param serviceName the name under which the service interface is registered at the server, see
     *            {@link RpcServer#getHandler(String)}
     * @param serviceConsumer used to invoke the method on the service interface
     * @return an empty future
     */
    <S> Future<Void> callService(Class<S> serviceInterface, String serviceName, Consumer<S> serviceConsumer);

    /**
     * Gives direct access to the service implementation. Please note that a call to the service might block for a while
     * (in case the rpc request is send to the server). It is always advisable to run service calls in an extra thread
     * to not block the ui!
     *
     * @param serviceInterface the interface to get the service implementation for
     * @param <S>
     * @return the service implementation
     */
    default <S> S getService(final Class<S> serviceInterface) {
        return getService(serviceInterface, serviceInterface.getSimpleName());
    }

    /**
     * See {@link #getService(Class)}.
     * @param <S>
     * @param serviceInterface
     * @param serviceName
     * @return the service implementation
     */
    <S> S getService(Class<S> serviceInterface, String serviceName);

    /**
     * @return <code>true</code> if this rpc client is connected to a server, or <code>false</code> if the call are just
     *         forwarded to a local implementation.
     */
    boolean isConnectedRemotely();

}

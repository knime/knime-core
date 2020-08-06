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
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.rpc.json.JsonRpcClient;

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
 * @param <S> the node model's service interface that defines the methods that can be used by the dialog/view to obtain
 *            data from it
 */
public abstract class AbstractRpcSingleClient<S> extends AbstractRpcClient implements RpcSingleClient<S> {

    /**
     * The data retrieval interface offered to the node dialog/view by the node model. This interface is defined and
     * implemented by the node developer.
     */
    private final Class<S> m_serviceInterface;

    /**
     * @param serviceInterface the data retrieval interface offered to the node dialog/view by the node model. This
     *            interface is defined and implemented by the node developer.
     */
    public AbstractRpcSingleClient(final Class<S> serviceInterface) {
        super();
        m_serviceInterface = serviceInterface;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <N extends NodeModel> RpcServer getRpcServerFromNodeFactory(final N nodeModel, final NodeFactory<NodeModel> factory) {
        if (factory instanceof RpcSingleServerFactory) {
            return ((RpcSingleServerFactory<N, S>)factory).createRpcServer(nodeModel);
        } else {
            return null;
        }
    }

    @Override
    protected String convertCall(final String serviceName, final Method method, final Object[] args) {
        return convertCall(method, args);
    }

    /**
     * See {@link AbstractRpcClient#convertCall(String, Method, Object[])}.
     * @param method
     * @param args
     * @return the call as string
     */
    protected abstract String convertCall(Method method, Object[] args);

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> Future<R> callServiceWithRes(final Function<S, R> serviceEvaluator) {
        return super.callServiceWithRes(m_serviceInterface, serviceEvaluator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<Void> callService(final Consumer<S> serviceConsumer) {
        return super.callService(m_serviceInterface, serviceConsumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public S getService() {
        return super.getService(m_serviceInterface);
    }

}

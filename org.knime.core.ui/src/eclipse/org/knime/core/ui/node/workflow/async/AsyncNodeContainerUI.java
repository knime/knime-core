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
 */
package org.knime.core.ui.node.workflow.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.ui.node.workflow.NodeContainerUI;

/**
 * UI-interface that provides asynchronous versions of some methods of {@link NodeContainerUI}.
 *
 * The methods that are overridden and provided with a asynchronous counterpart here are expected to (potentially)
 * return their result with a delay (e.g. because it is requested from a server). If there is a asynchronous counterpart
 * it is advised to use it instead of the synchronous method!
 *
 * All methods not overridden here are expected to return almost immediately.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface AsyncNodeContainerUI extends NodeContainerUI {

    /**
     * {@inheritDoc}
     */
    @Override
    default NodeDialogPane getDialogPaneWithSettings() throws NotConfigurableException {
        throw new UnsupportedOperationException("Please use async method instead.");
    }

    /**
     * Async version of {@link #getDialogPaneWithSettings()}.
     *
     * @return result as {@link Future}
     * @throws NotConfigurableException
     */
    public CompletableFuture<NodeDialogPane> getDialogPaneWithSettingsAsync() throws NotConfigurableException;

    /**
     * Creates a new {@link CompletableFuture}.
     *
     * @param sup the actual stuff to run
     * @return a new future
     */
    public static <U> CompletableFuture<U> future(final Supplier<U> sup) {
        return CompletableFuture.supplyAsync(sup);
    }
}
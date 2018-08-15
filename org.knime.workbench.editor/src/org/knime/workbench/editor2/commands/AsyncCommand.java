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
 *   Aug 13, 2018 (hornm): created
 */
package org.knime.workbench.editor2.commands;

import static org.knime.workbench.ui.async.AsyncUtil.waitForTermination;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.knime.core.ui.node.workflow.async.AsyncWorkflowManagerUI;

/**
 * A command that also provides asynchronous versions of the execute- and undo-methods.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface AsyncCommand {

    /**
     * If the command shall be executed in asynchronous mode.
     *
     * @return <code>true</code> if to be executed asynchronously, otherwise <code>false</code>
     */
    boolean shallExecuteAsync();

    /**
     * Asynchronous execution of the command. Only allowed to be called if {@link #shallExecuteAsync()} returns
     * <code>true</code>.
     *
     * @return the future for async execution
     */
    CompletableFuture<Void> executeAsync();

    /**
     * Asynchronous undo of the command. Only allowed to be called if {@link #shallExecuteAsync()} returns
     * <code>true</code>.
     *
     * @return the future for async undo
     */
    CompletableFuture<Void> undoAsync();

    /**
     * @return he commands underlying {@link AsyncWorkflowManagerUI}. Might be <code>null</code> if
     *         {@link #shallExecuteAsync()} is <code>false</code>.
     */
    AsyncWorkflowManagerUI getAsyncHostWFM();

    /**
     * Combines a list of {@link AsyncCommand}s into one {@link AbstractKNIMECommand} that also does a workflow refresh
     * after all commands have been executed.
     *
     * Note: All provided commands need to have the same host-wfm!
     *
     * @param asyncCommands the {@link AsyncCommand}s to be combined
     * @param waitingMessage the waiting message to be displayed when the command is executed and the executing takes a
     *            bit longer
     * @return the new command or <code>null</code> of no {@link AsyncCommand}s are provided
     */
    public static AbstractKNIMECommand combineWithRefresh(final List<AsyncCommand> asyncCommands,
        final String waitingMessage) {
        if (asyncCommands == null || asyncCommands.isEmpty()) {
            return null;
        }
        final AsyncWorkflowManagerUI asyncWFM = asyncCommands.get(0).getAsyncHostWFM();
        return new AbstractKNIMECommand(asyncWFM) {
            /**
             * {@inheritDoc}
             */
            @Override
            public void execute() {
                waitForTermination(CompletableFuture
                    .allOf(
                        asyncCommands.stream().map(c -> c.executeAsync()).toArray(size -> new CompletableFuture[size]))
                    .thenCompose(f -> asyncWFM.refresh(false)), waitingMessage);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void undo() {
                waitForTermination(CompletableFuture
                    .allOf(asyncCommands.stream().map(c -> c.undoAsync()).toArray(size -> new CompletableFuture[size]))
                    .thenCompose(f -> asyncWFM.refresh(false)), waitingMessage);
            }
        };
    }

}

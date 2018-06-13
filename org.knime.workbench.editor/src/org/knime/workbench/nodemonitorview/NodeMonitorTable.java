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
 *   Jun 12, 2018 (hornm): created
 */
package org.knime.workbench.nodemonitorview;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.ui.node.workflow.NodeContainerUI;

/**
 * A table displayed in the node monitor. It's contents are loaded from a node container the node monitor is currently
 * registered on.
 *
 * All methods are called from within the SWT-UI-thread, except
 * {@link #loadTableData(NodeContainerUI, NodeContainer, int)}!
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface NodeMonitorTable {
    /**
     * Called to load the required data from the provided node container. Can be called multiple times (e.g., when the
     * load data button is pressed multiple times). Disable load button if not desired. This method is not called in the
     * UI thread!
     *
     * @param ncUI the node container, never <code>null</code>
     * @param nc the ordinary node container instance, if available, otherwise <code>null</code>
     * @param count how many times this method has been called already
     * @throws LoadingFailedException if something went wrong during load
     */
    void loadTableData(final NodeContainerUI ncUI, NodeContainer nc, final int count) throws LoadingFailedException;

    /**
     * Called to configure the monitor table, i.e. setting header, adding columns etc.
     *
     * @param table the swt table
     */
    void setupTable(Table table);

    /**
     * Called to update the controls, e.g. the label of the load-button, enabling/disabling those etc. Might be called
     * multiple times. Can be stopped by disabling the load button.
     *
     * @param loadButton the button to manually load data
     * @param portCombo the port index selection
     * @param count how many times this method has been called already
     */
    void updateControls(Button loadButton, Combo portCombo, int count);

    /**
     * Let one change the info label.
     *
     * @param info the label to be updated
     */
    void updateInfoLabel(Label info);

    /**
     * Clean up when this monitor table isn't used anymore. E.g. de-registering listeneres etc.
     *
     * @param table the table to be disposed
     */
    void dispose(Table table);

    /**
     * Exception thrown when something went wrong with loading the data.
     */
    public static class LoadingFailedException extends Exception {
        private static final long serialVersionUID = 5320288247631681951L;
        LoadingFailedException(final String cause) {
            super(cause);
        }
    }
}

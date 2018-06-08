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

import java.util.Collection;
import java.util.Collections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SingleNodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;

/**
 * Puts info about workflow or node variables into the table.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class MonitorVariableTable implements NodeMonitorTable {
    private Collection<FlowVariable> m_variables = Collections.emptyList();

    private String m_infoLabel;

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadTableData(final NodeContainerUI ncUI, final NodeContainer nc, final int count)
        throws LoadingFailedException {
        // retrieve variables
        if ((ncUI instanceof SingleNodeContainerUI) || ncUI.getNrOutPorts() > 0) {
            // for normal nodes port 0 is available (hidden variable OutPort!)
            FlowObjectStack fos = ncUI.getOutPort(0).getFlowObjectStack();
            if (fos != null) {
                m_variables = fos.getAvailableFlowVariables(Type.values()).values();
                m_infoLabel = "Node Variables";
            } else {
                m_variables = Collections.emptyList();
            }
        } else {
            // no output port on metanode - display workflow variables
            m_variables = ((WorkflowManagerUI)ncUI).getWorkflowVariables();
            m_infoLabel = "Metanode Variables";
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupTable(final Table table) {
        String[] titles = {"Variable", "Value"};
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
        }
        // update content
        for (FlowVariable fv : m_variables) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, fv.getName());
            item.setText(1, fv.getValueAsString());
        }
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumn(i).pack();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateControls(final Button loadButton, final Combo portCombo, final int count) {
        portCombo.setEnabled(false);
        if (count == 0) {
            loadButton.setEnabled(true);
            loadButton.setText("Load Variables");
        } else {
            loadButton.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInfoLabel(final Label info) {
        if (m_infoLabel == null) {
            info.setText("Flow Variables");
        } else {
            info.setText(m_infoLabel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose(final Table table) {
        //nothing to do here
    }
}

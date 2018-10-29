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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.util.Pair;

/**
 * Puts info about the node settings into the table.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class MonitorSettingsTable implements NodeMonitorTable {
    private final boolean m_showAll;

    private ConfigBaseRO m_settings;

    private NodeAndBundleInformationPersistor m_nodeAndBundleInfo;

    /**
     * Creates a new monitor table to display node settings.
     *
     * @param showAll whether the entire settings should be displayed
     */
    public MonitorSettingsTable(final boolean showAll) {
        m_showAll = showAll;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadTableData(final NodeContainerUI ncUI, final NodeContainer nc, final int count)
        throws LoadingFailedException {
        // retrieve settings
        m_settings = ncUI.getNodeSettings();
        if (nc instanceof NativeNodeContainer) {
            m_nodeAndBundleInfo = ((NativeNodeContainer)nc).getNodeAndBundleInformation();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupTable(final Table table) {
       // and put them into the table
       String[] titles = {"Key", "Value"};
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
        }
        // add information about plugin and version to list (in show all/expert mode only)
        if (m_nodeAndBundleInfo != null && m_showAll) {
            TableItem item4 = new TableItem(table, SWT.NONE);
            item4.setText(0, "Node's feature name");
            item4.setText(1, m_nodeAndBundleInfo.getFeatureName().orElse("?"));

            TableItem item5 = new TableItem(table, SWT.NONE);
            item5.setText(0, "Node's feature symbolic name");
            item5.setText(1, m_nodeAndBundleInfo.getFeatureSymbolicName().orElse("?"));

            TableItem item6 = new TableItem(table, SWT.NONE);
            item6.setText(0, "Node's feature version (last saved with)");
            item6.setText(1, m_nodeAndBundleInfo.getFeatureVersion().map(v -> v.toString()).orElse("?"));

            TableItem item1 = new TableItem(table, SWT.NONE);
            item1.setText(0, "Node's plug-in name");
            item1.setText(1, m_nodeAndBundleInfo.getBundleName().orElse("?"));

            TableItem item2 = new TableItem(table, SWT.NONE);
            item2.setText(0, "Node's plug-in symbolic name");
            item2.setText(1, m_nodeAndBundleInfo.getBundleSymbolicName().orElse("?"));

            TableItem item3 = new TableItem(table, SWT.NONE);
            item3.setText(0, "Node's plug-in version (last saved with)");
            item3.setText(1, m_nodeAndBundleInfo.getBundleVersion().map(v -> v.toString()).orElse("?"));
        }
        // add settings to table
        Stack<Pair<Iterator<String>, ConfigBaseRO>> stack = new Stack<Pair<Iterator<String>, ConfigBaseRO>>();
        Iterator<String> it = m_settings.keySet().iterator();
        if (it.hasNext()) {
            stack.push(new Pair<Iterator<String>, ConfigBaseRO>(it, m_settings));
        }
        while (!stack.isEmpty()) {
            String key = stack.peek().getFirst().next();
            int depth = stack.size();
            boolean noexpertskip = (depth <= 1);
            ConfigBaseRO second = stack.peek().getSecond();
            ConfigBaseRO confBase = null;
            try {
                confBase = second.getConfigBase(key);
            } catch (InvalidSettingsException e) {
                //nothing to do here - then it's just null as handled below
            }
            if (!stack.peek().getFirst().hasNext()) {
                stack.pop();
            }
            if (confBase != null) {
                // it's another Config entry, push on stack!
                String val = confBase.toString();
                if ((!val.endsWith("_Internals")) || m_showAll) {
                    Iterator<String> it2 = confBase.iterator();
                    if (it2.hasNext()) {
                        stack.push(new Pair<Iterator<String>, ConfigBaseRO>(it2, confBase));
                    }
                } else {
                    noexpertskip = true;
                }
            }
            // in both cases, we report its value
            if ((!noexpertskip) || m_showAll) {
                //TODO little problem here: there is no way to turn the entry for a specific key in the config base into a string!
                //Hence, we check for a implementation that allows to do that as workaround.
                //Yet, it would be better to add, e.g., a #toStringValue(String key) method to the ConfigRO interface ...
                //However, so far there isn't any other implementation than ConfigBase anyway ...
                String value;
                if (second instanceof ConfigBase) {
                    value = ((ConfigBase)second).getEntry(key).toStringValue();
                } else {
                    throw new IllegalStateException(
                        "Sub type \"" + second.getClass() + "\" of ConfigBaseRO not supported.");
                }
                TableItem item = new TableItem(table, SWT.NONE);
                char[] indent = new char[depth - 1];
                Arrays.fill(indent, '_');
                item.setText(0, new String(indent) + key);
                item.setText(1, value != null ? value : "null");
            }
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
            loadButton.setText("Show settings");
            loadButton.setEnabled(true);
        } else {
            loadButton.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInfoLabel(final Label info) {
        info.setText("Node Configuration");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose(final Table table) {
        //nothing to do here
    }
}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.base.node.viz.hilite;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ViewUtils;

/**
 * Node that automatically hilites all incoming rows.
 * @author Martin Horn, University of Konstanz, Germany
 * @author Christian Dietz, University of Konstanz, Germany
 * @since 2.10
 */
public class AutoHiLiteNodeFactory extends NodeFactory<NodeModel> {

    private static final int NUMBER_OF_ROWS_HILITED_AT_ONCE = 1000;

    private static final SettingsModelBoolean createClearHilitesModel() {
        return new SettingsModelBoolean("clearhilites", false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new NodeModel(1, 1) {

            private SettingsModelBoolean m_smClearHiLites = createClearHilitesModel();

            /**
             * {@inheritDoc}
             */
            @Override
            protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
                return inSpecs;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected BufferedDataTable[] execute(
                    final BufferedDataTable[] inData,
                    final ExecutionContext exec) throws Exception {
                if (m_smClearHiLites.getBooleanValue()) {
                    getInHiLiteHandler(0).fireClearHiLiteEvent();
                }

                final Set<RowKey> keys = new HashSet<RowKey>();
                final HiLiteHandler hlh = getInHiLiteHandler(0);
                long counter = 0;
                long numOfRows = inData[0].size();
                for (final DataRow row : inData[0]) {
                    keys.add(row.getKey());
                    if (keys.size() == NUMBER_OF_ROWS_HILITED_AT_ONCE) {
                        exec.setProgress(++counter * NUMBER_OF_ROWS_HILITED_AT_ONCE / (double)numOfRows, "HiLiting all rows...");
                        hlh.fireHiLiteEvent(keys);
                        keys.clear();
                    }
                }
                hlh.fireHiLiteEvent(keys);
                ViewUtils.invokeAndWaitInEDT(() -> {}); // wait for hilite to propagate
                return inData;
            }

            @Override
            protected void loadInternals(final File nodeInternDir,
                    final ExecutionMonitor exec) throws IOException,
                    CanceledExecutionException {

            }

            @Override
            protected void saveInternals(final File nodeInternDir,
                    final ExecutionMonitor exec) throws IOException,
                    CanceledExecutionException {

            }

            @Override
            protected void saveSettingsTo(final NodeSettingsWO settings) {
                m_smClearHiLites.saveSettingsTo(settings);

            }

            @Override
            protected void validateSettings(final NodeSettingsRO settings)
                    throws InvalidSettingsException {
                m_smClearHiLites.validateSettings(settings);
            }

            @Override
            protected void loadValidatedSettingsFrom(
                    final NodeSettingsRO settings)
                    throws InvalidSettingsException {
                m_smClearHiLites.loadSettingsFrom(settings);
            }

            @Override
            protected void reset() {
                // no op
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new DefaultNodeSettingsPane() {
            {
                addDialogComponent(new DialogComponentBoolean(createClearHilitesModel(), "Clear HiLiting"));
            }
        };
    }

}

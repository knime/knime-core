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
 * History
 *   21.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.proximity;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class RandomForestProximityNodeModel extends NodeModel {

    private static final String CFG_PROXIMITYMEASURE = "proximityMeasure";

    public enum ProximityMeasure {

        Proximity("Proximity"),
        PathProximity("PathProximity");

        private final String m_name;
        private ProximityMeasure(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    static SettingsModelString createProximityMeasureModel() {
        return new SettingsModelString(CFG_PROXIMITYMEASURE, ProximityMeasure.Proximity.toString());
    }

    private SettingsModelString m_proximityMeasure = createProximityMeasureModel();


    /**
     * @param inPortTypes
     * @param outPortTypes
     */
    protected RandomForestProximityNodeModel() {
        super(new PortType[]{TreeEnsembleModelPortObject.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL},
            new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        TreeEnsembleModelPortObjectSpec modelSpec = (TreeEnsembleModelPortObjectSpec)inSpecs[0];
        DataTableSpec table1Spec = (DataTableSpec)inSpecs[1];
        DataTableSpec table2Spec = (DataTableSpec)inSpecs[2];
        // this method checks whether the input tables contain the necessary columns to use the model
        modelSpec.calculateFilterIndices(table1Spec);
        if (table2Spec != null) {
            modelSpec.calculateFilterIndices(table2Spec);
        }

        // the output table depends on the rows of the input tables
        return null;

    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        TreeEnsembleModelPortObject model = (TreeEnsembleModelPortObject)inObjects[0];
        BufferedDataTable table1 = (BufferedDataTable)inObjects[1];
        BufferedDataTable table2 = (BufferedDataTable)inObjects[2];
        BufferedDataTable[] tables;
        if (table2 != null) {
         tables = new BufferedDataTable[]{table1, table2};
        } else {
            tables = new BufferedDataTable[] {table1};
        }
        ExecutionContext calcExec = exec.createSubExecutionContext(0.7);
        ExecutionContext writeExec = exec.createSubExecutionContext(0.3);
        exec.setMessage("Calculating Proximity");
        ProximityMatrix pm = null;
        ProximityMeasure proximityMeasure = ProximityMeasure.valueOf(m_proximityMeasure.getStringValue());
        switch (proximityMeasure) {
            case PathProximity:
                pm = new PathProximity(tables, model).calculatePathProximities(calcExec);
                break;
            case Proximity:
                pm = Proximity.calcProximities(tables, model, calcExec);
                break;
            default:
                throw new IllegalStateException("Illegal proximity measure encountered.");
        }
        exec.setMessage("Writing");
        return new BufferedDataTable[]{pm.createTable(writeExec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_proximityMeasure.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_proximityMeasure.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_proximityMeasure.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

}

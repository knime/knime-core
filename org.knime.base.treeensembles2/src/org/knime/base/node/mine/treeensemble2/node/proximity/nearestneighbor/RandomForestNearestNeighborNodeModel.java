/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   07.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.proximity.nearestneighbor;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.node.proximity.PathProximity;
import org.knime.base.node.mine.treeensemble2.node.proximity.Proximity;
import org.knime.base.node.mine.treeensemble2.node.proximity.ProximityMatrix;
import org.knime.base.node.mine.treeensemble2.node.proximity.RandomForestProximityNodeModel;
import org.knime.base.node.mine.treeensemble2.node.proximity.RandomForestProximityNodeModel.ProximityMeasure;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 *
 * @author Adrian Nembach
 */
public class RandomForestNearestNeighborNodeModel extends NodeModel {

    static final int DEFAULT_NUMNEARESTNEIGHBORS = 10;

    private static final String CFG_NUMNEARESTNEIGHBORS = "numNearestNeighbors";
    private static final String CFG_PROXIMTYMEASURE = "proximityMeasure";

    private SettingsModelIntegerBounded m_numNearestNeighbors = createNumNearestNeighborsSettingsModel();
    private SettingsModelString m_proximityMeasure = createProximityMeasureModel();

    static SettingsModelIntegerBounded createNumNearestNeighborsSettingsModel() {
        return new SettingsModelIntegerBounded(CFG_NUMNEARESTNEIGHBORS, DEFAULT_NUMNEARESTNEIGHBORS, 0,
            Integer.MAX_VALUE);
    }

    static SettingsModelString createProximityMeasureModel() {
        return new SettingsModelString(CFG_PROXIMTYMEASURE, RandomForestProximityNodeModel.ProximityMeasure.Proximity.toString());
    }

    /**
     *
     */
    protected RandomForestNearestNeighborNodeModel() {
        super(new PortType[]{TreeEnsembleModelPortObject.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL},
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        TreeEnsembleModelPortObjectSpec modelSpec = (TreeEnsembleModelPortObjectSpec)inSpecs[0];
        DataTableSpec tableSpec1 = (DataTableSpec)inSpecs[1];
        // this method throws an InvalidSettingsException if the tableSpecs are not compatible to the model
        modelSpec.calculateFilterIndices(tableSpec1);
        if (inSpecs[2] != null) {
            DataTableSpec tableSpec2 = (DataTableSpec)inSpecs[2];
            modelSpec.calculateFilterIndices(tableSpec2);
        }

        DataTableSpec[] nearestNeighborSpecs =
            ProximityMatrix.createNearestNeighborOutSpecs(m_numNearestNeighbors.getIntValue());

        return nearestNeighborSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        TreeEnsembleModelPortObject ensembleModel = (TreeEnsembleModelPortObject)inObjects[0];
        boolean optionalTable = inObjects[2] != null;
        BufferedDataTable[] tables = new BufferedDataTable[optionalTable ? 2 : 1];
        tables[0] = (BufferedDataTable)inObjects[1];
        if (optionalTable) {
            tables[1] = (BufferedDataTable)inObjects[2];
        }

        ExecutionContext proxExec = exec.createSubExecutionContext(0.6);
        ExecutionContext nnExec = exec.createSubExecutionContext(0.4);
        exec.setMessage("Calculating");

        ProximityMatrix proximityMatrix = Proximity.calcProximities(tables, ensembleModel, proxExec);
        ProximityMeasure proximityMeasure = ProximityMeasure.valueOf(m_proximityMeasure.getStringValue());
        switch (proximityMeasure) {
            case Proximity :
                proximityMatrix = Proximity.calcProximities(tables, ensembleModel, proxExec);
                break;
            case PathProximity :
                proximityMatrix = new PathProximity(tables,ensembleModel).calculatePathProximities(proxExec);
                break;
            default :
                throw new IllegalStateException("Encountered unknown proximity measure.");
        }

        exec.setMessage("Calculating nearest neighbors");

        int k = m_numNearestNeighbors.getIntValue();
        return proximityMatrix.getNearestNeighbors(nnExec, k);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_numNearestNeighbors.saveSettingsTo(settings);
        m_proximityMeasure.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_numNearestNeighbors.validateSettings(settings);
        m_proximityMeasure.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_numNearestNeighbors.loadSettingsFrom(settings);
        m_proximityMeasure.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

}

/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 14, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.node.modelextractor;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble.model.TreeEnsembleModelPortObject;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.xml.PMMLCell;
import org.knime.core.data.xml.PMMLCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;

/**
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class TreeEnsembleModelExtractorNodeModel extends NodeModel {

    /**
     *  */
    public TreeEnsembleModelExtractorNodeModel() {
        super(new PortType[]{TreeEnsembleModelPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createOutSpec()};
    }

    private DataTableSpec createOutSpec() {
        DataColumnSpecCreator colCreator = new DataColumnSpecCreator("Models", PMMLCell.TYPE);
        return new DataTableSpec("Tree Ensemble Models", colCreator.createSpec());
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        TreeEnsembleModelPortObject treeEnsembleModel = (TreeEnsembleModelPortObject)inObjects[0];
        DataTableSpec outSpec = createOutSpec();
        BufferedDataContainer container = exec.createDataContainer(outSpec, false, 0);
        int nrModels = treeEnsembleModel.getEnsembleModel().getNrModels();
        for (int i = 0; i < nrModels; i++) {
            PMMLPortObject pmmlObject = treeEnsembleModel.createDecisionTreePMMLPortObject(i);
            DataCell cell = PMMLCellFactory.create(pmmlObject.getPMMLValue().toString());
            RowKey key = RowKey.createRowKey(i);
            container.addRowToTable(new DefaultRow(key, cell));
            exec.checkCanceled();
            exec.setProgress(i / (double)nrModels, "Exported model " + (i + 1) + "/" + nrModels);
        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no internals
    }

}

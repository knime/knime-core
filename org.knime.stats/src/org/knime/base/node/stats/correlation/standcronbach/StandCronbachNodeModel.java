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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.stats.correlation.standcronbach;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.correlation.pmcc.PMCCPortObjectAndSpec;
import org.knime.base.util.HalfDoubleMatrix;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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

/**
 * Node for calculating cronbachs alpha.
 *
 * @author adae, University of Konstanz
 */
public class StandCronbachNodeModel extends NodeModel {

    /** Empty constructor, 1 ins, 1 out. */
    public StandCronbachNodeModel() {
        super(new PortType[]{PMCCPortObjectAndSpec.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        PMCCPortObjectAndSpec model = (PMCCPortObjectAndSpec)inData[0];
        HalfDoubleMatrix mat = model.getCorrelationMatrix();
        double sum = 0;
        double count = 0;
        for (int i = 0; i < mat.getRowCount(); i++) {
            for (int j = i + 1; j < mat.getRowCount(); j++) {
                if (Double.isNaN(mat.get(i, j))) {
                    throw new IOException("No NAN values supported for the calculation, "
                + "try using an alternative correlation meassure");
                }
                sum += mat.get(i, j);
                count++;
            }
        }

        double mean = sum / count;
        double cronbach = (mat.getRowCount() * mean) / (1 + (mat.getRowCount() - 1) * mean);
        BufferedDataContainer out = exec.createDataContainer(getDataTableSpec());
        RowKey k = new RowKey("Cronbach");
        DataRow r = new DefaultRow(k, new DoubleCell(cronbach));
        out.addRowToTable(r);
        out.close();
        return new BufferedDataTable[]{out.getTable()};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{getDataTableSpec()};
    }

    /**
     * @return the data table spec of this node.
     */
    private DataTableSpec getDataTableSpec() {
        DataColumnSpecCreator dcs = new DataColumnSpecCreator("Cronbach", DoubleCell.TYPE);
        return new DataTableSpec(dcs.createSpec());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}

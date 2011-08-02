/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *
 * History
 *   Oct 17, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.normalize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.normalize.AffineTransConfiguration;
import org.knime.base.data.normalize.AffineTransTable;
import org.knime.base.data.normalize.Normalizer;
import org.knime.base.data.normalize.NormalizerPortObject;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
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
 *
 * @author wiswedel, University of Konstanz
 */
public class NormalizerApplyNodeModel extends NodeModel {

    /**
     * Constructor.
     */
    public NormalizerApplyNodeModel() {
        super(new PortType[]{NormalizerPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * @param modelPortType the port type.
     */
    protected NormalizerApplyNodeModel(final PortType modelPortType) {
        this(modelPortType, false);
    }

    /**
     * @param modelPortType the input port type.
     * @param passThrough if set to true, the incoming model is passed through
     *          as model outport
     */
    protected NormalizerApplyNodeModel(final PortType modelPortType,
            final boolean passThrough) {
        super(new PortType[]{modelPortType, BufferedDataTable.TYPE},
                passThrough 
                ? new PortType[]{modelPortType, BufferedDataTable.TYPE}
                        : new PortType[]{BufferedDataTable.TYPE});
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec modelSpec = (DataTableSpec)inSpecs[0];
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        List<String> unknownCols = new ArrayList<String>();
        List<String> knownCols = new ArrayList<String>();
        for (DataColumnSpec c : modelSpec) {
            DataColumnSpec inDataCol = dataSpec.getColumnSpec(c.getName());
            if (inDataCol == null) {
                unknownCols.add(c.getName());
            } else if (!inDataCol.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column \"" + c.getName()
                        + "\" is to be normalized, but is not numeric");
            } else {
                knownCols.add(c.getName());
            }
        }
        if (!unknownCols.isEmpty()) {
            setWarningMessage("Some column(s) as specified by the model is not "
                    + "present in the data: " + unknownCols);
        }
        String[] ar = knownCols.toArray(new String[knownCols.size()]);
        DataTableSpec s = Normalizer.generateNewSpec(dataSpec, ar);
        return new DataTableSpec[]{s};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        NormalizerPortObject model = (NormalizerPortObject)inData[0];
        BufferedDataTable table = (BufferedDataTable)inData[1];
        AffineTransTable t = new AffineTransTable(
                table, getAffineTrans(model.getConfiguration()));
        BufferedDataTable bdt = exec.createBufferedDataTable(t, exec);
        if (t.getErrorMessage() != null) {
            setWarningMessage(t.getErrorMessage());
        }
        return new BufferedDataTable[]{bdt};
    }
    
    /**
     * Return the configuration with possible additional transformations made.
     * 
     * @param affineTransConfig the original affine transformation 
     * configuration.
     * @return the (possible modified) configuration.
     */
     protected AffineTransConfiguration getAffineTrans(
                     final AffineTransConfiguration affineTransConfig) {
               return affineTransConfig;
     }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
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
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
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

}

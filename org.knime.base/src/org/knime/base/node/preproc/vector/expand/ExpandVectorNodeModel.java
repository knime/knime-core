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
 * ---------------------------------------------------------------------
 *
 * Created on 2014.03.20. by gabor
 */
package org.knime.base.node.preproc.vector.expand;


import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.data.vector.stringvector.StringVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;

/**
 * This is the model implementation for a node which samples and expands a double vector
 * to individual double columns.
 *
 * @author M. Berthold
 * @since 3.2
 */
public class ExpandVectorNodeModel extends NodeModel {

    private int[] m_indices = null;
    private enum VType { String, Double };
    private VType m_vectorType = null;

    /* static factory methods for the SettingsModels used here and in the NodeDialog. */
    /**
     * @return the settings model used to store the source column name.
     */
    static SettingsModelString createVectorColSelectSettingsModel() {
        return new SettingsModelString("VectorColumn", null);
    }
    private final SettingsModelString m_vectorColumn = createVectorColSelectSettingsModel();

    static SettingsModelString createIndexColSelectSettingsModel() {
        return new SettingsModelString("IndexColumn", null);
    }
    private final SettingsModelString m_indexColumn = createIndexColSelectSettingsModel();

    /**
     * Initialize model. One Data Inport, two Data Outports.
     */
    protected ExpandVectorNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE,
            InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // user settings must be set and valid
        assert m_vectorColumn.getStringValue() != null;
        assert m_indexColumn.getStringValue() != null;
        // selected columns should exist in input table
        if (!inSpecs[0].containsName(m_vectorColumn.getStringValue())) {
            throw new InvalidSettingsException("Selected column '"
                    + m_vectorColumn.getStringValue() + "' does not exist in input table!");
        }
        if (!inSpecs[1].containsName(m_indexColumn.getStringValue())) {
            throw new InvalidSettingsException("Selected column '"
                    + m_indexColumn.getStringValue() + "' does not exist in second input table!");
        }
        // and it should be of type double or string vector
        if (inSpecs[0].getColumnSpec(m_vectorColumn.getStringValue()).getType().isCompatible(DoubleVectorValue.class)) {
            m_vectorType = VType.Double;
        } else if (inSpecs[0].getColumnSpec(m_vectorColumn.getStringValue()).getType().isCompatible(DoubleVectorValue.class)) {
            m_vectorType = VType.String;
        } else {
            throw new InvalidSettingsException("Selected column '"
                    + m_vectorColumn.getStringValue() + "' does not contain double or string vectors!");
        }
        if (!inSpecs[1].getColumnSpec(m_indexColumn.getStringValue()).getType().isCompatible(IntValue.class)) {
            throw new InvalidSettingsException("Selected column '"
                    + m_indexColumn.getStringValue() + "' does not contain indices!");
        }
        // checks passed - we still don't what our output table will look like...
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        // retrieve indices from second table
        BufferedDataTable dt = inData[1];
        if (dt.size() > 100000) {
            throw new IllegalArgumentException("Refusing to generate output table with >100k columns!");
        }
        m_indices = new int[(int)dt.size()];
        int indexCol = dt.getSpec().findColumnIndex(m_indexColumn.getStringValue());
        int i = 0;
        for (DataRow row : dt) {
            DataCell cell = row.getCell(indexCol);
            if (!(cell instanceof IntValue)) {
                throw new IllegalArgumentException("Not an index in row " + i + "!");
            }
            m_indices[i] = ((IntValue)cell).getIntValue();
            i++;
        }
        BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0],
            createColumnRearranger(inData[0].getDataTableSpec(), m_vectorType), exec);
        return new BufferedDataTable[]{outTable};
    }

    /**
     * Creates the ColumnRearranger for the re-arranger table. Also used to compute the output table spec.
     *
     * @param inTableSpec the spec of the source table
     * @return the column rearranger
     */
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inTableSpec, final VType vtype)
            throws InvalidSettingsException {
        int sourceColumnIndex = inTableSpec.findColumnIndex(m_vectorColumn.getStringValue());
        DataColumnSpec sourceSpec = inTableSpec.getColumnSpec(sourceColumnIndex);
        ColumnRearranger c = new ColumnRearranger(inTableSpec);
        DataColumnSpec[] newSpecs;
        newSpecs = IntStream.range(0, m_indices.length).mapToObj(
            i -> new DataColumnSpecCreator(sourceSpec.getElementNames().get(m_indices[i]),
                                           m_vectorType.equals(VType.Double) ? DoubleCell.TYPE : StringCell.TYPE
                                               ).createSpec()).toArray(DataColumnSpec[]::new);
        c.append(new AbstractCellFactory(true, newSpecs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                DataCell dc = row.getCell(sourceColumnIndex);
                if ((m_vectorType.equals(VType.Double) && dc instanceof DoubleVectorValue)
                        || (m_vectorType.equals(VType.String) && dc instanceof StringVectorValue)) {
                    DataCell[] res = new DataCell[m_indices.length];
                    for (int i = 0; i < m_indices.length; i++) {
                        res[i] = m_vectorType.equals(VType.Double) ?
                              new DoubleCell(((DoubleVectorValue)dc).getValue(m_indices[i]))
                            : new StringCell(((StringVectorValue)dc).getValue(m_indices[i]));
                    }
                    return res;
                }
                return new MissingCell[m_indices.length];
            }
        });
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_vectorColumn.saveSettingsTo(settings);
        m_indexColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_vectorColumn.validateSettings(settings);
        m_indexColumn.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_vectorColumn.loadSettingsFrom(settings);
        m_indexColumn.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }
}

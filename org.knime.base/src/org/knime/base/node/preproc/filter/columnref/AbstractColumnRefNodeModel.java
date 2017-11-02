/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.columnref;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * @author Thomas Gabriel, University of Konstanz
 * @author Christian Dietz, University of Konstanz
 * @since 3.1
 */
public class AbstractColumnRefNodeModel extends NodeModel {

    /**
     * @return settings model for column type compatibility
     */
    static SettingsModelBoolean createTypeModel() {
        return new SettingsModelBoolean("type_compatibility", false);
    }

    /** Settings model to check column type compatibility. */
    private final SettingsModelBoolean m_typeComp = createTypeModel();

    /* Indicates whether the node is used as a ref splitter or ref filter*/
    private boolean m_isSplitter;

    /**
     * Creates a new node model of the Reference Column Filter node with two inputs and one output.
     *
     * @param isSplitter indicator whether a splitter is used or not.
     */
    public AbstractColumnRefNodeModel(final boolean isSplitter) {
        super(2, isSplitter ? 2 : 1);
        m_isSplitter = isSplitter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final ColumnRearranger[] arr = createRearranger(inSpecs[0], inSpecs[1]);
        final DataTableSpec spec1 = arr[0].createSpec();
        if (m_isSplitter) {
            return new DataTableSpec[]{spec1, arr[1].createSpec()};
        } else {
            return new DataTableSpec[]{spec1};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        ColumnRearranger[] cr = createRearranger(inData[0].getSpec(), inData[1].getSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], cr[0], exec);
        if (m_isSplitter) {
            return new BufferedDataTable[]{out, exec.createColumnRearrangeTable(inData[0], cr[1], exec)};
        } else {
            return new BufferedDataTable[]{out};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                ColumnRearranger[] cr = createRearranger((DataTableSpec)inSpecs[0], (DataTableSpec)inSpecs[1]);
                StreamableFunction func1 = cr[0].createStreamableFunction(0, 0);
                if (m_isSplitter) {
                    StreamableFunction func2 = cr[1].createStreamableFunction(0, 1);
                    RowInput rowInput = ((RowInput)inputs[0]);
                    RowOutput rowOutput1 = ((RowOutput)outputs[0]);
                    RowOutput rowOutput2 = ((RowOutput)outputs[1]);
                    StreamableFunction.runFinalInterwoven(rowInput, func1, rowOutput1, func2, rowOutput2, exec);
                } else {
                    func1.runFinal(inputs, outputs, exec);
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        OutputPortRole[] roles = new OutputPortRole[getNrOutPorts()];
        Arrays.fill(roles, OutputPortRole.DISTRIBUTED);
        return roles;
    }

    /**
     * Creates a <code>ColumnRearranger</code> that is a filter on the input table spec.
     *
     * @param oSpec original table spec to filter
     * @param filterSpec the reference table spec
     * @return a rearranger object that filters the original spec
     */
    private ColumnRearranger[] createRearranger(final DataTableSpec oSpec, final DataTableSpec filterSpec) {

        final ColumnRearranger[] cr;

        if (m_isSplitter) {
            cr = new ColumnRearranger[]{new ColumnRearranger(oSpec), new ColumnRearranger(oSpec)};
        } else {
            cr = new ColumnRearranger[]{new ColumnRearranger(oSpec)};
        }

        boolean exclude = isInvertInclusion();

        for (DataColumnSpec cspec : oSpec) {
            String name = cspec.getName();
            if (exclude) {
                // only true if m_splitter==false
                if (filterSpec.containsName(name)) {
                    DataType fType = filterSpec.getColumnSpec(name).getType();
                    if (!m_typeComp.getBooleanValue() || cspec.getType().isASuperTypeOf(fType)) {
                        cr[0].remove(name);
                    }
                }
            } else {
                if (!filterSpec.containsName(name)) {
                    cr[0].remove(name);
                } else {
                    DataType fType = filterSpec.getColumnSpec(name).getType();
                    if (m_typeComp.getBooleanValue() && !cspec.getType().isASuperTypeOf(fType)) {
                        cr[0].remove(name);
                    } else if (m_isSplitter) {
                        cr[1].remove(name);
                    }
                }
            }
        }
        return cr;

    }

    /**
     * Can be override by sub-classes. Default is false.
     *
     * @return true, is inclusion should be inverted (i.e. exclude)
     */
    protected boolean isInvertInclusion() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_typeComp.loadSettingsFrom(settings);
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
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_typeComp.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_typeComp.validateSettings(settings);
    }

}

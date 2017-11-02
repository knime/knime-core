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
 */
package org.knime.ext.sun.nodes.script.node.joiner;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;
import org.knime.ext.sun.nodes.script.calculator.MultiTableColumnCalculator;
import org.knime.ext.sun.nodes.script.compile.CompilationFailedException;
import org.knime.ext.sun.nodes.script.multitable.MultiSpecHandler;
import org.knime.ext.sun.nodes.script.multitable.MultiTableRowInput;
import org.knime.ext.sun.nodes.script.multitable.VirtualJointRow;
import org.knime.ext.sun.nodes.script.settings.MultiTableJavaScriptingCustomizer;
import org.knime.ext.sun.nodes.script.settings.MultiTableJavaScriptingSettings;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Stefano Woerner
 */
public class JavaJoinerNodeModel extends NodeModel implements FlowVariableProvider {

    private final MultiTableJavaScriptingCustomizer m_customizer;

    private MultiTableJavaScriptingSettings m_settings;

    /** The current row count or -1 if not in execute(). */
    private long m_rowCount = -1;

    /**
     * Creates a new instance of <code>JavaJoinerNodeModel</code>.
     * @param customizer the customizer to be used by this node
     *
     */
    public JavaJoinerNodeModel(final MultiTableJavaScriptingCustomizer customizer) {
        super(2, 1);
        if (customizer == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_customizer = customizer;
        // must create empty settings as otherwise, the wrong expression
        // version is guess in the dialog (non-existence --> v1)
        m_settings = m_customizer.createSettings();
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings == null || m_settings.getExpression() == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        try {
            m_settings.setInputAndCompile(MultiSpecHandler.createJointSpec(inSpecs[0], inSpecs[1]));
        } catch (CompilationFailedException e) {
            throw new InvalidSettingsException("The given expression could not be compiled.", e);
        }
        return new DataTableSpec[] {MultiSpecHandler.createJointOutputSpec(inSpecs[0], inSpecs[1])};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        MultiTableRowInput input = new MultiTableRowInput(inData[0], inData[1]);
        // Row count for the flow variable provider
        m_rowCount = input.getRowCount();
        // Container for the joined table
        BufferedDataContainer joinedTables = exec.createDataContainer(
            MultiSpecHandler.createJointOutputSpec(inData[0].getDataTableSpec(), inData[1].getDataTableSpec()));
        BufferedDataTableRowOutput output = new BufferedDataTableRowOutput(joinedTables);
        execute(input, output, exec);
        return new BufferedDataTable[]{joinedTables.getTable()};
    }

    private void execute(final MultiTableRowInput inData, final RowOutput output,
                            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData.getDataTableSpec();
        m_settings.setInputAndCompile(inSpec);
        long leftRowCount = inData.getLeftRowCount();
        long rightRowCount = inData.getRightRowCount();
        MultiTableColumnCalculator cc = new MultiTableColumnCalculator(m_settings, this, leftRowCount, rightRowCount);
        int rowIndex = 0;
        DataRow row;
        while ((row = inData.poll()) != null) {
            cc.setProgress(rowIndex, m_rowCount, row.getKey(), exec);
            DataCell result = cc.calculate((VirtualJointRow)row);
            boolean b;
            if (result.isMissing()) {
                b = false;
                setWarningMessage("Expression returned missing value for some combinations (interpreted as no match)");
            } else {
                b = ((BooleanValue)result).getBooleanValue();
            }
            if (b) {
                output.push(row);
            }
            exec.checkCanceled();
            rowIndex++;
        }
        output.close();
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_rowCount = -1;
    }


    /** {@inheritDoc} */
    @Override
    public Object readVariable(final String name, final Class<?> type) {
        if (Integer.class.equals(type)) {
            return peekFlowVariableInt(name);
        } else if (Double.class.equals(type)) {
            return peekFlowVariableDouble(name);
        } else if (String.class.equals(type)) {
            return peekFlowVariableString(name);
        } else {
            throw new RuntimeException("Invalid variable class: " + type);
        }
    }

    /** {@inheritDoc}
     * @deprecated*/
    @Deprecated
    @Override
    public int getRowCount() {
        return KnowsRowCountTable.checkRowCount(m_rowCount);
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.2
     */
    @Override
    public long getRowCountLong() {
        return m_rowCount;
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_customizer.createSettings().loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        MultiTableJavaScriptingSettings jsSettings = m_customizer.createSettings();
        jsSettings.loadSettingsInModel(settings);
        m_settings = jsSettings;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_settings != null) {
            m_settings.saveSettingsTo(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }
}

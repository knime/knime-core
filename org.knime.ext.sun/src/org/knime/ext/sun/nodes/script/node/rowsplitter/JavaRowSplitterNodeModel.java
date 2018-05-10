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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.ext.sun.nodes.script.node.rowsplitter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.ext.sun.nodes.script.calculator.ColumnCalculator;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;
import org.knime.ext.sun.nodes.script.expression.Expression;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingCustomizer;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class JavaRowSplitterNodeModel extends NodeModel
    implements FlowVariableProvider {

    private static final String SIMPLE_STREAMABLE_ROWCOUNT_KEY = "rowCount-int";

    private final JavaScriptingCustomizer m_customizer;
    private JavaScriptingSettings m_settings;

    /** The current row count or -1 if not in execute(). */
    private long m_rowCount = -1;

    /**
     *
     */
    public JavaRowSplitterNodeModel(final JavaScriptingCustomizer customizer,
            final boolean hasFalsePort) {
        super(1, hasFalsePort ? 2 : 1);
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings == null || m_settings.getExpression() == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        try {
            m_settings.setInputAndCompile(inSpecs[0]);
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
        DataTableSpec[] outs = new DataTableSpec[getNrOutPorts()];
        Arrays.fill(outs, inSpecs[0]);
        return outs;
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
        final ExecutionContext exec) throws Exception {
        final int rowCount = inData[0].getRowCount();
        m_rowCount = rowCount;
        DataTableRowInput input = new DataTableRowInput(inData[0]);
        DataTableSpec spec = inData[0].getDataTableSpec();
        BufferedDataContainer trueMatch = exec.createDataContainer(spec);
        BufferedDataTableRowOutput[] outputs;
        BufferedDataContainer falseMatch = null;
        if (getNrOutPorts() == 2) {
            falseMatch = exec.createDataContainer(spec);
        }
        outputs = Stream.of(trueMatch, falseMatch).filter(f -> f != null).map(f -> new BufferedDataTableRowOutput(f))
            .toArray(BufferedDataTableRowOutput[]::new);
        execute(input, outputs, exec);
        BufferedDataTable[] outTables = Stream.of(trueMatch, falseMatch).filter(f -> f != null)
                .map(f -> f.getTable()).toArray(BufferedDataTable[]::new);
        return outTables;
    }


    private void execute(final RowInput inData, final RowOutput[] outputs, final ExecutionContext exec) throws Exception {
        DataTableSpec spec = inData.getDataTableSpec();
        m_settings.setInputAndCompile(spec);
        ColumnCalculator cc = new ColumnCalculator(m_settings, this);
        int rowIndex = 0;
        DataRow r;
        RowOutput trueMatch = outputs[0];
        RowOutput falseMatch = outputs.length > 1 ? outputs[1] : null;
        while ((r = inData.poll()) != null) {
            cc.setProgress(rowIndex, m_rowCount, r.getKey(), exec);
            DataCell result = cc.calculate(r);
            boolean b;
            if (result.isMissing()) {
                b = false;
                setWarningMessage("Expression returned missing value for some rows (interpreted as no match)");
            } else {
                b = ((BooleanValue)result).getBooleanValue();
            }
            if (b) {
                trueMatch.push(r);
            } else if (falseMatch != null) {
                falseMatch.push(r);
            }
            exec.checkCanceled();
            rowIndex++;
        }
        trueMatch.close();
        if (falseMatch != null) {
            falseMatch.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_rowCount = -1;
    }

    @Override
    protected void onDispose() {
        if (m_settings != null) {
            m_settings.discard();
        }
        super.onDispose();
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        boolean isStreamable, isDistributable;
        Expression exp = m_settings == null ? null : m_settings.getCompiledExpression();
        if (exp == null) {
            isStreamable = false;
            isDistributable = false;
        } else {
            isStreamable = true;
            isDistributable = !exp.usesRowIndex();
        }
        return new InputPortRole[] {InputPortRole.get(isDistributable, isStreamable)};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return IntStream.range(0, getNrOutPorts())
                .mapToObj(i -> OutputPortRole.DISTRIBUTED).toArray(OutputPortRole[]::new);
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        return saveLong(-1);
    }

    /** {@inheritDoc} */
    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        if(m_settings != null) {
            Expression exp = m_settings.getCompiledExpression();
            if(exp != null && exp.usesRowCount() && readLong(internals) < 0) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public MergeOperator createMergeOperator() {
        return new MyMergeOperator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        return new MyStreamableOperator();
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

    /** {@inheritDoc} */
    @Override
    public int getRowCount() {
        return KnowsRowCountTable.checkRowCount(m_rowCount);
    }

    /**
     * {@inheritDoc}
     * @since 3.2
     */
    @Override
    public long getRowCountLong() {
        return m_rowCount;
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_customizer.createSettings().loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (m_settings != null) {
            m_settings.discard();
        }
        JavaScriptingSettings jsSettings = m_customizer.createSettings();
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
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

    private class MyMergeOperator extends MergeOperator {

        /** {@inheritDoc} */
        @Override
        public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
            return saveLong(Stream.of(operators).mapToLong(o -> readLong(o)).sum());
        }

        /** {@inheritDoc} */
        @Override
        public StreamableOperatorInternals mergeIntermediate(final StreamableOperatorInternals[] operators) {
            return mergeFinal(operators);
        }

    }

    private final class MyStreamableOperator extends StreamableOperator {

        /** {@inheritDoc} */
        @Override
        public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
            RowOutput[] rowOutputs = Stream.of(outputs).map(o -> (RowOutput)o).toArray(RowOutput[]::new);
            execute((RowInput)inputs[0], rowOutputs, exec);
        }

        /** {@inheritDoc} */
        @Override
        public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
            int count = 0;
            RowInput rowInput = (RowInput)inputs[0];
            while (rowInput.poll() != null) {
                count += 1;
            }
            rowInput.close();
            m_rowCount = count;
        }

        /** {@inheritDoc} */
        @Override
        public StreamableOperatorInternals saveInternals() {
            return saveLong(m_rowCount);
        }

        /** {@inheritDoc} */
        @Override
        public void loadInternals(final StreamableOperatorInternals internals) {
            m_rowCount = readLong(internals);
        }

    }

    private static long readLong(final StreamableOperatorInternals internals) {
        CheckUtils.checkArgument(internals instanceof SimpleStreamableOperatorInternals, "Not of expected class,"
            + "expected \"%s\", got \"%s\"", SimpleStreamableOperatorInternals.class.getSimpleName(),
            internals == null ? "<null" : internals.getClass().getSimpleName());
        try {
            return ((SimpleStreamableOperatorInternals)internals).getConfig().getLong(SIMPLE_STREAMABLE_ROWCOUNT_KEY);
        } catch (InvalidSettingsException e) {
            throw new RuntimeException(e);
        }
    }

    private static SimpleStreamableOperatorInternals saveLong(final long rowCount) {
        SimpleStreamableOperatorInternals internals = new SimpleStreamableOperatorInternals();
        internals.getConfig().addLong(SIMPLE_STREAMABLE_ROWCOUNT_KEY, rowCount);
        return internals;
    }

}

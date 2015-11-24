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
 *   24.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import java.io.File;
import java.io.IOException;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.jsnippet.guarded.JavaSnippetDocument;
import org.knime.base.node.jsnippet.util.FlowVariableRepository;
import org.knime.base.node.jsnippet.util.JavaSnippetSettings;
import org.knime.base.node.jsnippet.util.ValidationReport;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;


/**
 * The node model of the java snippet node.
 *
 * @author Heiko Hofer
 */
public class JavaSnippetNodeModel extends NodeModel {

    /* config key to store the row count in a streamable operator internals */
    private static final String CFG_ROW_COUNT = "row_count";

    private JavaSnippetSettings m_settings;
    private JavaSnippet m_snippet;
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
        "Java Snippet");
    /**
     * Create a new instance.
     */
    public JavaSnippetNodeModel() {
        super(1, 1);
        m_settings = new JavaSnippetSettings();
        m_snippet = new JavaSnippet();
        m_snippet.attachLogger(LOGGER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        m_snippet.setSettings(m_settings);
        FlowVariableRepository flowVarRepository =
            new FlowVariableRepository(getAvailableInputFlowVariables());
        ValidationReport report = m_snippet.validateSettings(inSpecs[0],
                flowVarRepository);
        if (report.hasWarnings()) {
            setWarningMessage(StringUtils.join(report.getWarnings(), "\n"));
        }
        if (report.hasErrors()) {
            throw new InvalidSettingsException(
                    StringUtils.join(report.getErrors(), "\n"));
        }


        DataTableSpec outSpec = m_snippet.configure(inSpecs[0],
                flowVarRepository);
        for (FlowVariable flowVar : flowVarRepository.getModified()) {
            if (flowVar.getType().equals(Type.INTEGER)) {
                pushFlowVariableInt(flowVar.getName(), flowVar.getIntValue());
            } else if (flowVar.getType().equals(Type.DOUBLE)) {
                pushFlowVariableDouble(flowVar.getName(),
                        flowVar.getDoubleValue());
            } else {
                pushFlowVariableString(flowVar.getName(),
                        flowVar.getStringValue());
            }
        }
        return new DataTableSpec[] {outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_snippet.setSettings(m_settings);
        FlowVariableRepository flowVarRepo =
            new FlowVariableRepository(getAvailableInputFlowVariables());
        BufferedDataTable output = m_snippet.execute(inData[0],
                flowVarRepo, exec);
        for (FlowVariable var : flowVarRepo.getModified()) {
            Type type = var.getType();
            if (type.equals(Type.INTEGER)) {
                pushFlowVariableInt(var.getName(), var.getIntValue());
            } else if (type.equals(Type.DOUBLE)) {
                pushFlowVariableDouble(var.getName(), var.getDoubleValue());
            } else { // case: type.equals(Type.STRING)
                pushFlowVariableString(var.getName(), var.getStringValue());
            }
        }
        return new BufferedDataTable[] {output};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        return new SimpleStreamableOperatorInternals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        if (usesRowCount()) {
            SimpleStreamableOperatorInternals simpleInternals = (SimpleStreamableOperatorInternals) internals;
            if (simpleInternals.getConfig().containsKey(CFG_ROW_COUNT)) {
                //already iterated
                return false;
            } else {
                //needs one iteration to determine the row count
                return true;
            }
        } else {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {

        m_snippet.setSettings(m_settings);
        final FlowVariableRepository flowVarRepo =
                new FlowVariableRepository(getAvailableInputFlowVariables());

        return new StreamableOperator() {

            private SimpleStreamableOperatorInternals m_internals;

            /**
             * {@inheritDoc}
             */
            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                m_internals = (SimpleStreamableOperatorInternals) internals;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                //count number of rows
                long count = 0;
                RowInput rowInput = (RowInput) inputs[0];
                while(rowInput.poll()!=null) {
                    count++;
                }
                m_internals.getConfig().addLong(CFG_ROW_COUNT, count);
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                long rowCount = -1;
                if (m_internals.getConfig().containsKey(CFG_ROW_COUNT)) {
                    rowCount = m_internals.getConfig().getLong(CFG_ROW_COUNT);
                }
                StreamableFunction func = m_snippet.createRearranger((DataTableSpec)inSpecs[0], flowVarRepo, (int) rowCount)
                    .createStreamableFunction();
                func.runFinal(inputs, outputs, exec);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public StreamableOperatorInternals saveInternals() {
                return m_internals;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {

            /**
             * {@inheritDoc}
             */
            @Override
            public StreamableOperatorInternals mergeIntermediate(final StreamableOperatorInternals[] operators) {
                //sum up the row counts if necessary
                long count = 0;
                for (int i = 0; i < operators.length; i++) {
                    SimpleStreamableOperatorInternals simpleInternals = (SimpleStreamableOperatorInternals)operators[i];
                    CheckUtils.checkState(simpleInternals.getConfig().containsKey(CFG_ROW_COUNT),
                        "Config for key " + CFG_ROW_COUNT + " isn't set.");
                    try {
                        count += simpleInternals.getConfig().getLong(CFG_ROW_COUNT);
                    } catch (InvalidSettingsException e) {
                        // should not happen since we checked already
                        throw new RuntimeException(e);
                    }
                }

                SimpleStreamableOperatorInternals res = new SimpleStreamableOperatorInternals();
                if(count > 0) {
                    res.getConfig().addLong(CFG_ROW_COUNT, count);
                }
                return res;
            }

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                //nothing to do here
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        InputPortRole inputPortRole = InputPortRole.DISTRIBUTED_STREAMABLE;
        if (usesRowIndex()) {
            //rowindex field is used, cannot be distributed
            inputPortRole = InputPortRole.NONDISTRIBUTED_STREAMABLE;
            LOGGER
                .warn("The ROWINDEX field is used in the snippet. Calculations cannot be done in distributed manner!");
        }
        if (usesRowCount()) {
            //rowcount field is used -> streaming not possible but distributed execution
            inputPortRole = InputPortRole.DISTRIBUTED_NONSTREAMABLE;
            LOGGER.warn("The ROWCOUNT field is used in the snippet. Calculations cannot be done in streamed manner!");
        }
        return new InputPortRole[]{inputPortRole};
    }

    /* tells if the snippet code uses the row index constant */
    private boolean usesRowIndex() {
        //is there a better test?
        try {
            String snippetCode = m_snippet.getDocument().getTextBetween(JavaSnippetDocument.GUARDED_BODY_START,
                JavaSnippetDocument.GUARDED_BODY_END);
            return snippetCode.contains(JavaSnippet.ROWINDEX);
        } catch (BadLocationException e) {
            //should not happen -> implementation error
            throw new RuntimeException("Most likely an implementation error.", e);
        }

    }

    /* tells if the snippet code uses the row count constant */
    private boolean usesRowCount() {
        //is there a better test?
        try {
            String snippetCode = m_snippet.getDocument().getTextBetween(JavaSnippetDocument.GUARDED_BODY_START,
                JavaSnippetDocument.GUARDED_BODY_END);
            return snippetCode.contains(JavaSnippet.ROWCOUNT);
        } catch (BadLocationException e) {
            //should not happen -> implementation error
            throw new RuntimeException("Most likely an implementation error.", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        JavaSnippetSettings s = new JavaSnippetSettings();
        s.loadSettings(settings);
        // TODO: Check settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals, nothing to reset.
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals.
    }
}

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
 * ---------------------------------------------------------------------
 *
 * History
 *   20.09.2016 (Simon): created
 */
package org.knime.base.node.preproc.stringmanipulation.variable;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

import org.knime.base.node.preproc.stringmanipulation.StringManipulationSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.ext.sun.nodes.script.calculator.ColumnCalculator;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;
import org.knime.ext.sun.nodes.script.compile.CompilationFailedException;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;

/**
 * The node model of the string manipulation (variable) node.
 *
 * @author Simon Schmid
 */
public class StringManipulationVariableNodeModel extends NodeModel implements FlowVariableProvider {

    private final StringManipulationSettings m_settings;

    /**
     * flow variable in, flow variable out.
     */
    StringManipulationVariableNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{FlowVariablePortObject.TYPE});
        m_settings = new StringManipulationSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.getExpression() == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        try {
            calculate();
        } catch (InstantiationException | CompilationFailedException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }

        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    /**
     * @throws CompilationFailedException
     * @throws InstantiationException
     * @throws Exception
     */
    private void calculate() throws InvalidSettingsException, CompilationFailedException, InstantiationException {
        if (m_settings.getExpression() == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        JavaScriptingSettings settings = m_settings.getJavaScriptingSettings();
        settings.setInputAndCompile(new DataTableSpec());

        // calculate the result
        ColumnCalculator cc = new ColumnCalculator(settings, this);
        DataCell calculate = null;
        try {
            calculate = cc.calculate(new DefaultRow(new RowKey(""), new DataCell[]{}));
        } catch (NoSuchElementException e){
            throw new InvalidSettingsException(e.getMessage());
        }
        String newVariableName;
        Map<String, FlowVariable> inputFlowVariables = getAvailableInputFlowVariables();
        if (m_settings.isReplace()) {
            newVariableName = m_settings.getColName();
            CheckUtils.checkSettingNotNull(inputFlowVariables.get(newVariableName),
                "Can't replace input variable '%s' -- it does not exist in the input", newVariableName);
        } else {
            newVariableName = new UniqueNameGenerator(inputFlowVariables.keySet()).newName(m_settings.getColName());
        }

        // convert and push result as flow variable
        CheckUtils.checkSetting(!calculate.isMissing(), "Calculation returned missing value");
        Class<? extends DataCell> cellType = calculate.getClass();
        if (cellType.equals(IntCell.class)) {
            pushFlowVariableInt(newVariableName, ((IntCell)calculate).getIntValue());
        } else if (cellType.equals(DoubleCell.class)) {
            pushFlowVariableDouble(newVariableName, ((DoubleCell)calculate).getDoubleValue());
        } else if (cellType.equals(StringCell.class)) {
            pushFlowVariableString(newVariableName, ((StringCell)calculate).getStringValue());
        } else {
            throw new RuntimeException("Invalid variable class: " + cellType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // must not call calculate() as it has been done in configure()
        // (and there would otherwise be the chance that we do a calculation
        // on the already updated values!)
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new StringManipulationSettings().loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    @Override
    protected void onDispose() {
        m_settings.discard();
        super.onDispose();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    @Deprecated
    @Override
    public int getRowCount() {
        return 0;
    }
}

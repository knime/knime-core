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
 * History
 *   Oct 3, 2010 (wiswedel): created
 */
package org.knime.ext.sun.nodes.script.node.editvar;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.ext.sun.nodes.script.calculator.ColumnCalculator;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingCustomizer;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType.JavaSnippetDoubleType;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType.JavaSnippetIntType;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType.JavaSnippetStringType;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class JavaEditVariableNodeModel extends NodeModel
    implements FlowVariableProvider {

    /** A no column spec. */
    public static final DataTableSpec EMPTY_SPEC = new DataTableSpec("empty");
    private static final DataRow EMPTY_ROW =
        new DefaultRow("empty", new String[0]);

    private JavaScriptingSettings m_settings;
    private final JavaScriptingCustomizer m_customizer;

    /** flow var in, flow var out.
     * @param customizer Customizer for settings. */
    JavaEditVariableNodeModel(final JavaScriptingCustomizer customizer) {
        super(new PortType[] {FlowVariablePortObject.TYPE_OPTIONAL},
              new PortType[] {FlowVariablePortObject.TYPE});
        m_customizer = customizer;
        // must create empty settings as otherwise, the wrong expression
        // version is guess in the dialog (non-existence --> v1)
        m_settings = m_customizer.createSettings();
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        calculate();
        return new PortObjectSpec[] {FlowVariablePortObjectSpec.INSTANCE};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        // must not call calculate() as it has been done in configure()
        // (and there would otherwise be the chance that we do a calculation
        // on the already updated values!)
        return new PortObject[] {FlowVariablePortObject.INSTANCE};
    }

    /**
     *
     */
    private void calculate() throws InvalidSettingsException {
        if (m_settings == null || m_settings.getExpression() == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        try {
            m_settings.setInputAndCompile(EMPTY_SPEC);
            ColumnCalculator cc = new ColumnCalculator(m_settings, this);
            DataCell result = cc.calculate(EMPTY_ROW);
            if (result.isMissing()) {
                throw new InvalidSettingsException(
                        "Calculation returned missing value");
            }
            Class<?> ret = m_settings.getReturnType();
            if (ret.equals(JavaSnippetIntType.INSTANCE.getJavaClass(false))) {
                Integer i = JavaSnippetIntType.INSTANCE.asJavaObject(result);
                pushFlowVariableInt(m_settings.getColName(), i);
            } else if (ret.equals(
                    JavaSnippetDoubleType.INSTANCE.getJavaClass(false))) {
                Double d = JavaSnippetDoubleType.INSTANCE.asJavaObject(result);
                pushFlowVariableDouble(m_settings.getColName(), d);
            } else {
                String s = JavaSnippetStringType.INSTANCE.asJavaObject(result);
                pushFlowVariableString(m_settings.getColName(), s);
            }
        } catch (InvalidSettingsException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no op
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_customizer.createSettings().loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        JavaScriptingSettings jsSettings = m_customizer.createSettings();
        jsSettings.loadSettingsInModel(settings);
        m_settings = jsSettings;
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
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRowCountLong() {
        return -1L;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

}

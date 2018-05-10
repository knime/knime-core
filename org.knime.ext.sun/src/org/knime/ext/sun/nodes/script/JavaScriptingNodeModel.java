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
 * -------------------------------------------------------------------
 *
 */
package org.knime.ext.sun.nodes.script;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.ext.sun.nodes.script.calculator.ColumnCalculator;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingCustomizer;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JavaScriptingNodeModel
    extends NodeModel implements FlowVariableProvider {

    private JavaScriptingSettings m_settings;

    /** The current row count or -1 if not in execute(). */
    private long m_rowCount = -1;

    private final JavaScriptingCustomizer m_customizer;

    /** One input, one output.
     * @param customizer Customizer to disable/enable certain fields. */
    public JavaScriptingNodeModel(final JavaScriptingCustomizer customizer) {
        super(1, 1);
        m_customizer = customizer;
        // must create empty settings as otherwise, the wrong expression
        // version is guess in the dialog (non-existence --> v1)
        m_settings = m_customizer.createSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_settings != null) {
            m_settings.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_customizer.createSettings().loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (m_settings != null) {
            m_settings.discard();
        }
        JavaScriptingSettings temp = m_customizer.createSettings();
        temp.loadSettingsInModel(settings);
        m_settings = temp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        ColumnRearranger c = createColumnRearranger(inSpec);
        m_rowCount = inData[0].size();
        try {
            BufferedDataTable o = exec.createColumnRearrangeTable(
                    inData[0], c, exec);
            return new BufferedDataTable[]{o};
        } finally {
            m_rowCount = -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_rowCount = -1;
    }

    @Override
    protected void onDispose() {
        if (m_settings != null) {
            m_settings.discard();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger c = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec)
        throws InvalidSettingsException {
        if (m_settings == null || m_settings.getExpression() == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        boolean isReplace = m_settings.isReplace();
        String colName = m_settings.getColName();
        try {
            m_settings.setInputAndCompile(spec);
            ColumnCalculator cc = new ColumnCalculator(m_settings,
                    this);
            ColumnRearranger result = new ColumnRearranger(spec);
            if (isReplace) {
                result.replace(cc, colName);
            } else {
                if (spec.containsName(colName)) {
                    throw new InvalidSettingsException(
                            "Can't create new column \"" + colName
                            + "\" as input spec already contains such column");
                }
                result.append(cc);
            }
            return result;
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
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
     * @since 3.2
     */
    @Override
    public long getRowCountLong() {
        return m_rowCount;
    }

}

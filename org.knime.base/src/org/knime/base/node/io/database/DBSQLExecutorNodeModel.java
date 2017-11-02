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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database;

import java.util.NoSuchElementException;

import org.knime.base.util.flowvariable.FlowVariableProvider;
import org.knime.base.util.flowvariable.FlowVariableResolver;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.reader.DBReader;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
final class DBSQLExecutorNodeModel extends DBNodeModel implements FlowVariableProvider {

    /**
     * Settings key for sort columns.
     */
    static final String CFG_STATEMENT = "statement";

    private String m_statement = "";

    /**
     * Creates a new database reader.
     */
    DBSQLExecutorNodeModel() {
        super(new PortType[]{DatabaseConnectionPortObject.TYPE}, new PortType[]{DatabaseConnectionPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(CFG_STATEMENT, m_statement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        settings.getString(CFG_STATEMENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        String statement = settings.getString(CFG_STATEMENT);
        m_statement = statement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {
        DatabaseConnectionPortObject dbObj = (DatabaseConnectionPortObject)inData[0];
        DatabaseConnectionSettings conn = dbObj.getConnectionSettings(getCredentialsProvider());
        String[] statements = parseStatementAndReplaceVariables().split(DBReader.SQL_QUERY_SEPARATOR);

        final double max = statements.length;
        int i = 0;
        for (String statement : statements) {
            exec.checkCanceled();
            statement = statement.trim();
            exec.setProgress(i++ / max, "Executing '" + statement + "'");
            if (!statement.isEmpty()) {
                conn.execute(statement, getCredentialsProvider());
            }
        }
        return inData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return inSpecs;
    }

    private String parseStatementAndReplaceVariables() throws InvalidSettingsException {
        String flowVarCorrectedText;
        try {
            flowVarCorrectedText = FlowVariableResolver.parse(m_statement, this);
        } catch (NoSuchElementException nse) {
            throw new InvalidSettingsException(nse.getMessage(), nse);
        }
        return flowVarCorrectedText;
    }

}

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
package org.knime.base.node.io.database;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.reader.DBReader;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBColumnFilterNodeModel extends DBNodeModel {

    private final SettingsModelFilterString m_filter
         = DBColumnFilterNodeDialogPane.createColumnFilterModel();

    /**
     * Creates a new database reader.
     */
    DBColumnFilterNodeModel() {
            super(new PortType[]{DatabasePortObject.TYPE},
                    new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_filter.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        // FIX bug 5040: potential problem with clone settings method when in-/exclude list contain same elements
        SettingsModelFilterString filter = DBColumnFilterNodeDialogPane.createColumnFilterModel();
        filter.loadSettingsFrom(settings);
        if (filter.getIncludeList().isEmpty() && !filter.getExcludeList().isEmpty()) {
            throw new InvalidSettingsException("No columns included in output table.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_filter.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec)
            throws CanceledExecutionException, Exception {
        DatabasePortObject dbObj = (DatabasePortObject)inData[0];
        DatabaseQueryConnectionSettings conn = dbObj.getConnectionSettings(getCredentialsProvider());
        ColumnRearranger colre = new ColumnRearranger(dbObj.getSpec().getDataTableSpec());
        colre.keepOnly(m_filter.getIncludeList().toArray(new String[0]));
        final DataTableSpec resultSpec = colre.createSpec();
        final String newQuery = createQuery(conn, resultSpec);
        conn = createDBQueryConnection(dbObj.getSpec(), newQuery);
        DatabasePortObjectSpec outSpec = new DatabasePortObjectSpec(
                resultSpec, conn.createConnectionModel());
        DatabasePortObject outObj = new DatabasePortObject(outSpec);
        return new PortObject[]{outObj};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DatabasePortObjectSpec spec = (DatabasePortObjectSpec) inSpecs[0];
        StringBuilder buf = new StringBuilder();
        for (String column : m_filter.getIncludeList()) {
            if (!spec.getDataTableSpec().containsName(column)) {
                buf.append("\"" + column + "\" ");
            }
        }
        if (buf.length() > 0) {
            throw new InvalidSettingsException("Not all columns available in "
                    + "input spec: " + buf.toString());
        }
        DatabaseQueryConnectionSettings conn = spec.getConnectionSettings(getCredentialsProvider());
        ColumnRearranger colre = new ColumnRearranger(spec.getDataTableSpec());
        colre.keepOnly(m_filter.getIncludeList().toArray(new String[0]));
        final DataTableSpec resultSpec = colre.createSpec();
        final String newQuery = createQuery(conn, resultSpec);
        conn = createDBQueryConnection(spec, newQuery);
        return new PortObjectSpec[]{new DatabasePortObjectSpec(
                resultSpec, conn.createConnectionModel())};
    }

    private String createQuery(final DatabaseQueryConnectionSettings conn, final DataTableSpec resultSpec) {
        final String query = conn.getQuery();
        final StatementManipulator manipulator = conn.getUtility().getStatementManipulator();
        final StringBuilder buf = new StringBuilder();
        final String[] queries = query.split(
                DBReader.SQL_QUERY_SEPARATOR);
        for (int i = 0; i < queries.length - 1; i++) {
            buf.append(queries[i]);
            buf.append(DBReader.SQL_QUERY_SEPARATOR);
        }
        // build SELECT statement
        buf.append("SELECT ");
        if (m_filter.getExcludeList().isEmpty()) {
            super.setWarningMessage("All columns retained.");
            buf.append("*"); // selects all columns
        } else {
            int i = 0;
            for (final DataColumnSpec colSpec : resultSpec) {
                final String colName = colSpec.getName();
                if (i++ > 0) {
                    buf.append(",");
                }
                buf.append(manipulator.quoteColumn(colName));
            }
        }
        final String selectQuery = queries[queries.length - 1];
        buf.append(" FROM (" + selectQuery + ") table_"
                + System.identityHashCode(this));
        return buf.toString();
    }

}

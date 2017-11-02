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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.util.Pair;

/**
 * @author Thomas Gabriel, University of Konstanz
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
final class DBJoinerNodeModel extends DBNodeModel {

    private DBJoinerSettings m_settings = new DBJoinerSettings();

    /**
     * Creates a new database joiner.
     */
    DBJoinerNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE, DatabasePortObject.TYPE},
            new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        new DBJoinerSettings().loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        DBJoinerSettings tempSettings = new DBJoinerSettings();
        tempSettings.loadSettingsInModel(settings);
        m_settings = tempSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {
        exec.setMessage("Retrieving metadata from database");
        DatabasePortObject dbObjectLeft = (DatabasePortObject)inData[0];
        DatabasePortObject dbObjectRight = (DatabasePortObject)inData[1];
        DatabasePortObject outObject =
            new DatabasePortObject(createDbOutSpec(dbObjectLeft.getSpec(), dbObjectRight.getSpec()));
        return new PortObject[]{outObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        checkJoinColumns(((DatabasePortObjectSpec)inSpecs[0]).getDataTableSpec(),
            ((DatabasePortObjectSpec)inSpecs[1]).getDataTableSpec());
        if (m_settings.getLeftJoinOnColumns().length < 1) {
            throw new InvalidSettingsException("At least one pair of columns to join on has to be selected");
        }
        return new PortObjectSpec[]{createDbOutSpec((DatabasePortObjectSpec)inSpecs[0],
            (DatabasePortObjectSpec)inSpecs[1])};
    }

    private void checkJoinColumns(final DataTableSpec leftSpec, final DataTableSpec rightSpec)
        throws InvalidSettingsException {
        List<String> leftColumns = Arrays.asList(leftSpec.getColumnNames());
        for (String joinColumn : m_settings.getLeftJoinOnColumns()) {
            if (!leftColumns.contains(joinColumn)) {
                throw new InvalidSettingsException("Top table is missing join column " + joinColumn);
            }
        }
        List<String> rightColumns = Arrays.asList(rightSpec.getColumnNames());
        for (String joinColumn : m_settings.getRightJoinOnColumns()) {
            if (!rightColumns.contains(joinColumn)) {
                throw new InvalidSettingsException("Bottom table is missing join column " + joinColumn);
            }
        }
    }

    /**
     * @param inSpecLeft Spec of the left database object
     * @param inSpecRight Spec of the right database object
     * @return Spec of the output database object
     * @throws InvalidSettingsException If the current settings are invalid
     */
    private DatabasePortObjectSpec createDbOutSpec(final DatabasePortObjectSpec inSpecLeft,
        final DatabasePortObjectSpec inSpecRight) throws InvalidSettingsException {
        DatabaseQueryConnectionSettings connectionLeft = inSpecLeft.getConnectionSettings(getCredentialsProvider());
        DatabaseQueryConnectionSettings connectionRight = inSpecRight.getConnectionSettings(getCredentialsProvider());
        StatementManipulator statementManipulator = connectionLeft.getUtility().getStatementManipulator();
        // Check first if connections are equal, otherwise abort
        checkIfDBConnectionsAreEqual(connectionLeft, connectionRight);
        // Calculate output columns
        Pair<Map<String, String>, Map<String, String>> columnMaps =
            createOutputColumnMaps(inSpecLeft.getDataTableSpec(), inSpecRight.getDataTableSpec(), statementManipulator);
        if (columnMaps.getFirst().size() + columnMaps.getSecond().size() < 1) {
            throw new InvalidSettingsException("No column selected");
        }
        // Create output query
        String newQuery =
            createQuery(connectionLeft.getQuery(), connectionRight.getQuery(), statementManipulator, columnMaps);
        // Create output spec
        DataTableSpec tableSpec =
            createOutSpec(inSpecLeft.getDataTableSpec(), inSpecRight.getDataTableSpec(), columnMaps,
                statementManipulator);
        DatabaseQueryConnectionSettings connection = createDBQueryConnection(inSpecLeft, newQuery);
        return new DatabasePortObjectSpec(tableSpec, connection.createConnectionModel());
    }

    private String getLastQuery(final String queryList) {
        final String[] queries = queryList.split(DBReader.SQL_QUERY_SEPARATOR);
        return queries[queries.length - 1];
    }

    /**
     * @param queryLeft Query that returns the left table
     * @param queryRight Query that returns the right table
     * @param manipulator Statement manipulator for the current database
     * @param columnMaps The output columns
     * @return SQL query that joins the two tables selected by the left query and right query
     */
    private String createQuery(final String queryLeft, final String queryRight, final StatementManipulator manipulator,
        final Pair<Map<String, String>, Map<String, String>> columnMaps) {
        final StringBuilder buf = new StringBuilder();
        String leftQuery = getLastQuery(queryLeft);
        String rightQuery = getLastQuery(queryRight);
        // Build identifiers for both tables
        String tableNameLeft = "table_" + System.identityHashCode(this) + "_1";
        String tableNameRight = "table_" + System.identityHashCode(this) + "_2";
        Map<String, String> leftColumns = columnMaps.getFirst();
        Map<String, String> rightColumns = columnMaps.getSecond();
        StringBuilder columnBuf = new StringBuilder();
        // Add columns from left table
        int index = 0;
        for (String oldName : leftColumns.keySet()) {
            columnBuf.append(tableNameLeft + "." + manipulator.quoteColumn(oldName) + " AS "
                + manipulator.quoteColumn(leftColumns.get(oldName)));
            if (++index < leftColumns.size() || !rightColumns.isEmpty()) {
                columnBuf.append(", ");
            }
        }
        // Add columns from right table
        index = 0;
        for (String oldName : rightColumns.keySet()) {
            columnBuf.append(tableNameRight + "." + manipulator.quoteColumn(oldName) + " AS "
                + manipulator.quoteColumn(rightColumns.get(oldName)));
            if (++index < rightColumns.size()) {
                columnBuf.append(", ");
            }
        }
        buf.append("SELECT " + columnBuf.toString() + " FROM (" + leftQuery + ") " + tableNameLeft);
        // build JOIN clause
        buf.append(" " + m_settings.getJoinMode().getKeyword() + " (" + rightQuery + ") " + tableNameRight);
        if (m_settings.getLeftJoinOnColumns().length > 0) {
            buf.append(" ON ");
        }
        for (int i = 0; i < m_settings.getLeftJoinOnColumns().length; i++) {
            buf.append(tableNameLeft + "." + manipulator.quoteColumn(m_settings.getLeftJoinOnColumns()[i]) + "="
                + tableNameRight + "." + manipulator.quoteColumn(m_settings.getRightJoinOnColumns()[i]));
            if (i + 1 < m_settings.getLeftJoinOnColumns().length) {
                buf.append(" " + (m_settings.getAndComposition() ? "AND" : "OR") + " ");
            }
        }
        return buf.toString();
    }

    /**
     * @param inSpecLeft Spec of the left table
     * @param inSpecRight Spec of the right table
     * @param columnMaps The output columns
     * @return Spec for the data table that will be created when executing the query
     */
    private DataTableSpec createOutSpec(final DataTableSpec inSpecLeft, final DataTableSpec inSpecRight,
        final Pair<Map<String, String>, Map<String, String>> columnMaps, final StatementManipulator manipulator) {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
        // Add columns from left table
        Map<String, String> leftColumns = columnMaps.getFirst();
        for (String oldName : leftColumns.keySet()) {
            colSpecs.add(new DataColumnSpecCreator(manipulator.getValidColumnName(leftColumns.get(oldName)), inSpecLeft
                .getColumnSpec(oldName).getType()).createSpec());
        }
        // Add columns from right table
        Map<String, String> rightColumns = columnMaps.getSecond();
        for (String oldName : rightColumns.keySet()) {
            colSpecs.add(new DataColumnSpecCreator(manipulator.getValidColumnName(rightColumns.get(oldName)),
                inSpecRight.getColumnSpec(oldName).getType()).createSpec());
        }
        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
    }

    /**
     * Checks if the given connections are equal by comparing the JDBC URL and the user.
     *
     * @param connectionLeft Connection of left table
     * @param connectionRight Connection of right table
     * @throws InvalidSettingsException If the connections are not equal
     */
    private void checkIfDBConnectionsAreEqual(final DatabaseQueryConnectionSettings connectionLeft,
        final DatabaseQueryConnectionSettings connectionRight) throws InvalidSettingsException {
        if (!connectionLeft.getJDBCUrl().equals(connectionRight.getJDBCUrl())) {
            throw new InvalidSettingsException("Database URL is different.");
        }
        if (connectionLeft.getUserName(getCredentialsProvider()) != null
            && !connectionLeft.getUserName(getCredentialsProvider()).equals(
                connectionRight.getUserName(getCredentialsProvider()))) {
            throw new InvalidSettingsException("User is different.");
        }
    }

    /**
     * @param leftSpec Spec of the left table
     * @param rightSpec Spec of the right table
     * @return Pair of maps for left and right table that contain the source name and the target name
     * @throws InvalidSettingsException If a duplicate was found and the option do abort if duplicate is found is
     *             selected
     */
    private Pair<Map<String, String>, Map<String, String>> createOutputColumnMaps(final DataTableSpec leftSpec,
        final DataTableSpec rightSpec, final StatementManipulator statementManipulator)
        throws InvalidSettingsException {
        // Use maps that keep insert order
        Map<String, String> leftMap = new LinkedHashMap<String, String>();
        Map<String, String> rightMap = new LinkedHashMap<String, String>();
        List<String> leftColumnsToFilter;
        if (m_settings.getFilterLeftJoinOnColumns()) {
            // Add join on columns if they should be filtered out
            leftColumnsToFilter = Arrays.asList(m_settings.getLeftJoinOnColumns());
        } else {
            leftColumnsToFilter = new ArrayList<String>(0);
        }
        // Add selected columns
        String[] selectedLeftColumns =
            m_settings.getAllLeftColumns() ? leftSpec.getColumnNames() : getAvailableColumns(
                m_settings.getLeftColumns(), leftSpec);
        for (String column : selectedLeftColumns) {
            if (!leftColumnsToFilter.contains(column)) {
                String newName = uniqueColumnName(column, leftMap.values(), rightMap.values(), statementManipulator);
                if (newName != null) {
                    leftMap.put(column, newName);
                }
            }
        }
        List<String> rightColumnsToFilter;
        if (m_settings.getFilterRightJoinOnColumns()) {
            // Add join on columns if they should be filtered out
            rightColumnsToFilter = Arrays.asList(m_settings.getRightJoinOnColumns());
        } else {
            rightColumnsToFilter = new ArrayList<String>(0);
        }
        // Add selected columns
        String[] selectedRightColumns =
                m_settings.getAllRightColumns() ? rightSpec.getColumnNames() : getAvailableColumns(
                    m_settings.getRightColumns(), rightSpec);
        for (String column : selectedRightColumns) {
            if (!rightColumnsToFilter.contains(column)) {
                String newName = uniqueColumnName(column, leftMap.values(), rightMap.values(), statementManipulator);
                if (newName != null) {
                    rightMap.put(column, newName);
                }
            }
        }
        return new Pair<Map<String, String>, Map<String, String>>(leftMap, rightMap);
    }

    private String[] getAvailableColumns(final String[] columns, final DataTableSpec spec) {
        List<String> availableColumns = new ArrayList<String>();
        for (String column : columns) {
            if (spec.containsName(column)) {
                availableColumns.add(column);
            }
        }
        return availableColumns.toArray(new String[availableColumns.size()]);
    }

    /**
     * @param name The original name
     * @param leftNames Names that are already taken by the left table
     * @param rightNames Names that are already taken by the right table
     * @return Column name that is not yet contained in the given name collections, null if the column should be skipped
     * @throws InvalidSettingsException If a duplicate was found and the option do abort if duplicate is found is
     *             selected
     */
    private String uniqueColumnName(final String name, final Collection<String> leftNames,
        final Collection<String> rightNames, final StatementManipulator statementManipulator)
        throws InvalidSettingsException {
        // Start with original name
        String newName = name;
        int i = 1;
        // Do this until name is unique
        while (leftNames.contains(newName) || rightNames.contains(newName)) {
            switch (m_settings.getDuplicateHandling()) {
                case Filter:
                    // Skip colunn
                    return null;
                case DontExecute:
                    // Abort execution
                    throw new InvalidSettingsException(
                        "Found duplicate columns, won't execute. Fix it in \"Column Selection\" tab");
                case AppendSuffixAutomatic:
                    // Name with appended number
                    newName = name + " (#" + i + ")";
                    break;
                case AppendSuffix:
                    // Append custom prefix to name
                    newName = name;
                    for (int j = 0; j < i; j++) {
                        newName = newName + m_settings.getCustomSuffix();
                    }
                    break;
            }
            newName = statementManipulator.getValidColumnName(newName);
            i++;
        }
        return newName;
    }

}

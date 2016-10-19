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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.05.2014 (thor): created
 */
package org.knime.core.node.port.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.DBAggregationFunctionFactory;
import org.knime.core.node.port.database.aggregation.InvalidDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.AvgDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.CountDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.FirstDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.LastDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.MaxDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.MinDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.SumDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.custom.CustomDBAggregationFunction;
import org.knime.core.node.port.database.connection.CachedConnectionFactory;
import org.knime.core.node.port.database.connection.DBConnectionFactory;
import org.knime.core.node.port.database.connection.DBDriverFactory;
import org.knime.core.node.port.database.connection.PriorityDriverFactory;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.node.port.database.reader.DBReaderImpl;
import org.knime.core.node.port.database.tablecreator.DBTableCreator;
import org.knime.core.node.port.database.tablecreator.DBTableCreatorImpl;
import org.knime.core.node.port.database.writer.DBWriter;
import org.knime.core.node.port.database.writer.DBWriterImpl;

/**
 * This class is the entry point for database specific routines and information. All implementations must be
 * thread-safe.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DatabaseUtility {
    /**Default database utility identifier.
     * @since 2.11*/
    public static final String DEFAULT_DATABASE_IDENTIFIER = "default";

    private static final StatementManipulator DEFAULT_MANIPULATOR = new StatementManipulator();

    private static final DBAggregationFunctionFactory[] DEFAULT_AGGREGATION_FUNCTIONS =
            new DBAggregationFunctionFactory[] {new AvgDBAggregationFunction.Factory(),
        new CountDBAggregationFunction.Factory(), new FirstDBAggregationFunction.Factory(),
        new LastDBAggregationFunction.Factory(), new MaxDBAggregationFunction.Factory(),
        new MinDBAggregationFunction.Factory(), new SumDBAggregationFunction.Factory()};

    private final Map<String, DBAggregationFunctionFactory> m_aggregationFunctions;

    private final String m_dbIdentifier;

    private final StatementManipulator m_stmtManipulator;

    private boolean m_supportsIsValid = true;

    private DBConnectionFactory m_connFactory;

    /**
     * Returns a utility implementation for the given database type. If no specific implementation is available, a
     * generic manipulator is returned.
     *
     * @param dbIdentifier the database identifier, usually the second part of a JDBC URL
     * ({@link #getDatabaseIdentifier()}; must not be <code>null</code>
     * @return an SQL manipulator
     */
    public static DatabaseUtility getUtility(final String dbIdentifier) {
        return DatabaseUtilityRegistry.getInstance().getUtility(dbIdentifier);
    }

    /**
     * @return {@link Collection} of database identifiers of all registered database utility implementations
     * @since 2.11
     */
    public static Collection<String> getDatabaseIdentifiers() {
        return DatabaseUtilityRegistry.getInstance().getDBIdentifier();
    }

    /**
     * @return {@link Set} with available JDBC drivers
     * @since 3.2
     */
    public static Set<String> getJDBCDriverClasses() {
        return DatabaseUtilityRegistry.getInstance().getJDBCDriverClasses();
    }

    /**
     * Constructor that uses all default aggregation methods.
     * @see #DatabaseUtility(String, StatementManipulator, DBAggregationFunctionFactory...)
     */
    @Deprecated
    public DatabaseUtility() {
        this(null, null, (DBAggregationFunctionFactory[]) null);
    }
    /**
     * @param dbIdentifier the unique database identifier or <code>null</code> to use default
     * @param stmtManipulator  the {@link StatementManipulator} or <code>null</code> to use default
     * @param aggregationFunctions array of all {@link DBAggregationFunction}s or <code>null</code>
     * to use default
     * @since 2.11
     */
    public DatabaseUtility(final String dbIdentifier, final StatementManipulator stmtManipulator,
        final DBAggregationFunctionFactory... aggregationFunctions) {
        this(dbIdentifier, stmtManipulator, null, aggregationFunctions);
    }

    /**
     * @param dbIdentifier the unique database identifier or <code>null</code> to use default
     * @param stmtManipulator  the {@link StatementManipulator} or <code>null</code> to use default
     * @param driverFactory {@link DBDriverFactory}
     * @param aggregationFunctions array of all {@link DBAggregationFunction}s or <code>null</code>
     * to use default
     * @since 3.2
     */
    public DatabaseUtility(final String dbIdentifier, final StatementManipulator stmtManipulator,
        final DBDriverFactory driverFactory, final DBAggregationFunctionFactory... aggregationFunctions) {
        m_dbIdentifier = dbIdentifier != null ? dbIdentifier : DEFAULT_DATABASE_IDENTIFIER;
        m_stmtManipulator = stmtManipulator != null ? stmtManipulator : DEFAULT_MANIPULATOR;
        final DBAggregationFunctionFactory[] f;
        if (aggregationFunctions != null) {
            f = aggregationFunctions;
        } else {
            f = DEFAULT_AGGREGATION_FUNCTIONS;
        }
        m_aggregationFunctions = new HashMap<>(f.length);
        for (DBAggregationFunctionFactory function : f) {
            final DBAggregationFunctionFactory duplicateFunction =
                    m_aggregationFunctions.put(function.getId(), function);
            if (duplicateFunction != null) {
                NodeLogger.getLogger(DatabaseUtility.class).error(
                    "Duplicate aggregation function found for id: " + function.getId()
                    + " class 1: " + function.getClass().getName()
                    + " class 2: " + duplicateFunction.getClass().getName());
            }
        }
        //add the custom function if it does not exists since it is of use to all databases
        if (!m_aggregationFunctions.containsKey(CustomDBAggregationFunction.ID)) {
            final DBAggregationFunctionFactory customFunction = new CustomDBAggregationFunction.Factory();
            m_aggregationFunctions.put(customFunction.getId(), customFunction);
        }
        //ensures that we always consider external drivers first
        final DBDriverFactory df;
        if (driverFactory == null || RegisteredDriversConnectionFactory.getInstance().equals(driverFactory)) {
            df = RegisteredDriversConnectionFactory.getInstance();
        } else {
            df = new PriorityDriverFactory(RegisteredDriversConnectionFactory.getInstance(), driverFactory);
        }
        m_connFactory = createConnectionFactory(df);
    }

    /**
     * @param df {@link DBDriverFactory} to use
     * @return the {@link DBConnectionFactory} to use
     * @since 3.2 the
     */
    protected DBConnectionFactory createConnectionFactory(final DBDriverFactory df) {
        //currently we only support the old single connection factory
         return new CachedConnectionFactory(df);
    }

    /**
     * @return the identifier of the db usually the second part of the jdbc url
     * @since 2.11
     */
    public String getDatabaseIdentifier() {
        return m_dbIdentifier;
    }

    /**
     * Returns a statement manipulator for the database.
     *
     * @return a statement manipulator
     */
    public StatementManipulator getStatementManipulator() {
        return m_stmtManipulator;
    }

    /**
     * Returns a list if all aggregation functions that the current database supports.
     *
     * @return {@link Collection} of all supported {@link DBAggregationFunction}s
     * @since 2.11
     */
    public Collection<DBAggregationFunction> getAggregationFunctions() {
        final List<DBAggregationFunction> clone = new ArrayList<>(m_aggregationFunctions.size());
        for (DBAggregationFunctionFactory function: m_aggregationFunctions.values()) {
            clone.add(function.createInstance());
        }
        return clone;
    }

    /**
     * Returns the aggregation function with the given id, if the current database supports it.
     * Otherwise a {@link InvalidDBAggregationFunction} is returned.
     *
     * @param id the id as returned by {@link DBAggregationFunction#getId()}
     * @return the {@link DBAggregationFunction} for the given name or an instance of the
     * {@link InvalidDBAggregationFunction} that has the given id
     * @since 2.11
     */
    public DBAggregationFunction getAggregationFunction(final String id) {
        final DBAggregationFunctionFactory function = m_aggregationFunctions.get(id);
        if (function != null) {
            return function.createInstance();
        }
        final String dbIdentifier = getDatabaseIdentifier();
        String msg = "The function '" + id + "' is not supported by ";
        if (dbIdentifier != null) {
            msg += dbIdentifier + ".";
        } else {
            msg += "the current database.";
        }
        return new InvalidDBAggregationFunction(id, msg, dbIdentifier);
    }

    /**
     * Returns whether the database supports INSERT operations. The default is <code>true</code>.
     *
     * @return <code>true</code> if INSERT is supported, <code>false</code> otherwise
     */
    public boolean supportsInsert() {
        return true;
    }

    /**
     * Returns whether the database supports UPDATE operations. The default is <code>true</code>.
     *
     * @return <code>true</code> if UPDATE is supported, <code>false</code> otherwise
     */
    public boolean supportsUpdate() {
        return true;
    }

    /**
     * Returns whether the database supports DELETE operations. The default is <code>true</code>.
     *
     * @return <code>true</code> if DELETE is supported, <code>false</code> otherwise
     */
    public boolean supportsDelete() {
        return true;
    }

    /**
     * @return <code>true</code> if the database supports random sampling
     * @since 3.1
     */
    public boolean supportsRandomSampling() {
        return false;
    }

    /**
     * @return <code>true</code> if the database supports CASE statements
     * @since 3.1
     */
    public boolean supportsCase() {
        return false;
    }

    /**
     * Returns whether the given table name exists in the database denoted by the connection.
     *
     * @param conn a database connection
     * @param tableName the table's name
     * @return <code>true</code> if the table exists, <code>false</code> otherwise
     * @throws SQLException if an DB error occurs
     * @since 2.10
     */
    public boolean tableExists(final Connection conn, final String tableName) throws SQLException {
        final NodeLogger logger = NodeLogger.getLogger(getClass());
        logger.debug("Checking if table " + tableName + " exists");
        String sql = getStatementManipulator().forMetadataOnly("SELECT 1 as tmpcol FROM " + tableName);
        logger.debug("Execute query: " + sql);
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            logger.debug("Table " + tableName + " exists");
            return true;
        } catch (SQLException ex) {
            logger.debug(
                "Got exception while checking for existence of table '" + tableName + "': " + ex.getMessage(), ex);
            return false; // we assume this is because the table does not exist; must be fixed!!!
        }
    }

    /**
     * Checks if the given connection is valid and can be re-used. If the database driver does not support
     * {@link Connection#isValid(int)} then a <code>SELECT 1</code> statement is issued instead.
     *
     * @param conn any connection
     * @return <code>true</code> if the connection is valid
     * @throws SQLException if an SQL error occurs
     * @since 2.12
     */
    public synchronized boolean isValid(final Connection conn) throws SQLException {
        if (!m_supportsIsValid) {
            try (Statement st = conn.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT 1")) {
                    rs.next();
                    return true;
                }
            } catch (SQLException e) {
                return false;
            }
        } else {
            try {
                return conn.isValid(5);
            } catch (final Throwable t) {
                NodeLogger.getLogger(getClass()).debug("java.sql.Connection#isValid throws error: " + t.getMessage()
                    + ". Executing simple \"SELECT 1\" statement to check validity from now on.", t);
                m_supportsIsValid = false;
                return isValid(conn);
            }
        }
    }


    /**
     * @param querySettings the {@link DatabaseQueryConnectionSettings}
     * @return the {@link DBReader} to perform read operations in the db
     * @since 3.1
     */
    public DBReader getReader(final DatabaseQueryConnectionSettings querySettings) {
        return new DBReaderImpl(querySettings);
    }

    /**
     * @param connSettings {@link DatabaseConnectionSettings}
     * @return {@link DBWriter} to use to write, update or delete values in this db
     * @since 3.1
     */
    public DBWriter getWriter(final DatabaseConnectionSettings connSettings) {
        return new DBWriterImpl(connSettings);
    }

    /**
     * {@link DBConnectionFactory} to use
     * @return {@link DBConnectionFactory}
     * @since 3.2
     */
    public DBConnectionFactory getConnectionFactory() {
        return m_connFactory;
    }

    /**
     * Class that creates a new table.
     * @param schema the optional schema name (can be <code>null</code>)
     * @param tableName the table name
     * @param isTempTable <code>true</code> if this is a temporary table
     * @return {@link DBTableCreator} to generate create table statement
     * @since 3.3
     */
    public DBTableCreator getTableCreator(final String schema, final String tableName, final boolean isTempTable) {
        return new DBTableCreatorImpl(getStatementManipulator(), schema, tableName, isTempTable);
    }
}

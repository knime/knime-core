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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.aggregation.AverageDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.CountDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.FirstDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.LastDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.MaxDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.MinDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.SumDBAggregationFunction;

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

    private static final String EXT_POINT_ID = "org.knime.core.DatabaseUtility";

    private static final Map<String, DatabaseUtility> UTILITY_MAP = new HashMap<>();

    private static final StatementManipulator DEFAULT_MANIPULATOR = new StatementManipulator();

    private static final DBAggregationFunction[] DEFAULT_AGGREGATION_FUNCTIONS = new DBAggregationFunction[] {
        AverageDBAggregationFunction.getInstance(), CountDBAggregationFunction.getInstance(),
        FirstDBAggregationFunction.getInstance(), LastDBAggregationFunction.getInstance(),
        MaxDBAggregationFunction.getInstance(), MinDBAggregationFunction.getInstance(),
        SumDBAggregationFunction.getInstance()};

    private final HashMap<String, DBAggregationFunction> m_supportedAggregationFunctions;

    private final String m_dbIdentifier;

    private final StatementManipulator m_stmtManipulator;

    /**
     * Returns a utility implementation for the given database type. If no specific implementation is available, a
     * generic manipulator is returned.
     *
     * @param dbIdentifier the database identifier, usually the second part of a JDBC URL
     * ({@link #getDatabaseIdentifier()}; must not be <code>null</code>
     * @return an SQL manipulator
     */
    public static synchronized DatabaseUtility getUtility(final String dbIdentifier) {
        if (DEFAULT_DATABASE_IDENTIFIER.equals(dbIdentifier)) {
            //this is the default utility identifier
            return new DatabaseUtility();
        }
        DatabaseUtility utility = UTILITY_MAP.get(dbIdentifier);
        if (utility == null) {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

            for (IExtension ext : point.getExtensions()) {
                IConfigurationElement[] elements = ext.getConfigurationElements();
                for (IConfigurationElement utilityElement : elements) {
                    final String extensionPointDBIdentifier = utilityElement.getAttribute("database");
                    if (dbIdentifier.equals(extensionPointDBIdentifier)) {
                        try {
                            utility = (DatabaseUtility)utilityElement.createExecutableExtension("class");
                            if (!utility.getDatabaseIdentifier().equals(dbIdentifier)) {
                                NodeLogger.getLogger(DatabaseUtility.class).error(
                                    "Extension point database identifier and database identifier of implementing class "
                                    + "are different extension point identifier: " + extensionPointDBIdentifier
                                    + " implementing class identifier: " + utility.getDatabaseIdentifier()
                                    + ". This might result in problems when fetching the DatabaseUtility class.");
                            }
                        } catch (CoreException ex) {
                            NodeLogger.getLogger(DatabaseUtility.class).error(
                                "Could not create registered database utility "
                                    + utilityElement.getAttribute("class") + " for " + dbIdentifier
                                    + " from plug-in " + utilityElement.getNamespaceIdentifier() + ": "
                                    + ex.getMessage(), ex);
                        }
                    }
                }
            }

            if (utility == null) {
                utility = new DatabaseUtility();
            }
            UTILITY_MAP.put(dbIdentifier, utility);
        }
        return utility;
    }

    /**
     * @return {@link Collection} of database identifiers of all registered database utility implementations
     * @since 2.11
     */
    public static synchronized Collection<String> getDatabaseIdentifiers() {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;
        final Collection<String> databaseIdentifier = new LinkedList<>();
        databaseIdentifier.add(DEFAULT_DATABASE_IDENTIFIER);
        for (IExtension ext : point.getExtensions()) {
            final IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement utilityElement : elements) {
                final String extensionPointDBIdentifier = utilityElement.getAttribute("database");
                databaseIdentifier.add(extensionPointDBIdentifier);
            }
        }
        return databaseIdentifier;
    }

    /**
     * Constructor that uses all default aggregation methods.
     */
    public DatabaseUtility() {
        this(null, null, (DBAggregationFunction[]) null);
    }

    /**
     * @param dbIdentifier the unique database identifier or <code>null</code> to use default
     * @param stmtManipulator  the {@link StatementManipulator} or <code>null</code> to use default
     * @param supportedFunctions array of all supported {@link DBAggregationFunction}s or <code>null</code>
     * to use default
     * @since 2.11
     */
    public DatabaseUtility(final String dbIdentifier, final StatementManipulator stmtManipulator,
        final DBAggregationFunction... supportedFunctions) {
        m_dbIdentifier = dbIdentifier != null ? dbIdentifier : DEFAULT_DATABASE_IDENTIFIER;
        m_stmtManipulator = stmtManipulator != null ? stmtManipulator : DEFAULT_MANIPULATOR;
        final DBAggregationFunction[] f;
        if (supportedFunctions != null) {
            f = supportedFunctions;
        } else {
            f = DEFAULT_AGGREGATION_FUNCTIONS;
        }
        m_supportedAggregationFunctions = new HashMap<>(f.length);
        for (DBAggregationFunction function : f) {
            m_supportedAggregationFunctions.put(function.getName(), function);
        }
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
     * @return unmodifiable {@link Collection} of all supported {@link DBAggregationFunction}s
     * @since 2.11
     */
    public Collection<DBAggregationFunction> getSupportedAggregationFunctions() {
        return Collections.unmodifiableCollection(m_supportedAggregationFunctions.values());
    }

    /**
     * @param name the name of the {@link DBAggregationFunction}
     * @return the {@link DBAggregationFunction} for the given name or <code>null</code>
     * @since 2.11
     */
    public DBAggregationFunction getAggregationFunction(final String name) {
        return m_supportedAggregationFunctions.get(name);
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
}

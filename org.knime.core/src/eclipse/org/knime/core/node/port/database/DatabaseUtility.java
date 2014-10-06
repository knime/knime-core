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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * This class is the entry point for database specific routines and information. All implementations must be
 * thread-safe.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DatabaseUtility {
    private static final String EXT_POINT_ID = "org.knime.core.DatabaseUtility";

    private static final Map<String, DatabaseUtility> UTILITY_MAP = new HashMap<>();

    private static final StatementManipulator DEFAULT_MANIPULATOR = new StatementManipulator();

    /**
     * Returns a utility implementation for the given database type. If no specific implementation is available, a
     * generic manipulator is returned.
     *
     * @param dbIdentifier the database identifier, the same as the second part of a JDBC URL; must not be
     *            <code>null</code>
     * @return an SQL manipulator
     */
    public static synchronized DatabaseUtility getUtility(final String dbIdentifier) {
        DatabaseUtility utility = UTILITY_MAP.get(dbIdentifier);
        if (utility == null) {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

            for (IExtension ext : point.getExtensions()) {
                IConfigurationElement[] elements = ext.getConfigurationElements();
                for (IConfigurationElement utilityElement : elements) {
                    if (dbIdentifier.equals(utilityElement.getAttribute("database"))) {
                        try {
                            utility = (DatabaseUtility)utilityElement.createExecutableExtension("class");
                        } catch (CoreException ex) {
                            NodeLogger.getLogger(StatementManipulator.class).error(
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
     * Returns a statement manipulator for the database.
     *
     * @return a statement manipulator
     */
    public StatementManipulator getStatementManipulator() {
        return DEFAULT_MANIPULATOR;
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
     * Returns whether the given table name exists in the database denoted by the connection.
     *
     * @param conn a database connection
     * @param tableName the table's name
     * @return <code>true</code> if the table exists, <code>false</code> otherwise
     * @throws SQLException if an DB error occurs
     * @since 2.10
     */
    public boolean tableExists(final Connection conn, final String tableName) throws SQLException {
        String sql = getStatementManipulator().forMetadataOnly("SELECT 1 FROM " + tableName);

        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            return true;
        } catch (SQLException ex) {
            NodeLogger.getLogger(getClass()).debug(
                "Got exception while checking for existence of table '" + tableName + "': " + ex.getMessage(), ex);
            return false; // we assume this is because the table does not exist; must be fixed!!!
        }
    }
}

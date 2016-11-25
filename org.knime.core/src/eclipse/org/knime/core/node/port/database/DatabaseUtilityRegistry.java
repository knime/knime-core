/*
 * ------------------------------------------------------------------------
 *
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
 *   25.05.2016 (koetter): created
 */
package org.knime.core.node.port.database;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.connection.DBDriverFactory;
import org.knime.core.node.port.database.connection.PriorityDriverFactory;

/**
 * Singleton that collects all {@link DatabaseUtility} extension points.
 * @author Tobias Koetter, KNIME.com
 */
class DatabaseUtilityRegistry {

    private static final DatabaseUtilityRegistry INSTANCE = new DatabaseUtilityRegistry();

    private static final String EXT_POINT_ID = "org.knime.core.DatabaseUtility";

    private final Map<String, DatabaseUtility> m_utilityMap = new HashMap<>();

    private final Set<String> m_driverNames = new LinkedHashSet<>();

    @SuppressWarnings("deprecation")
    private DatabaseUtilityRegistry() {
        loadUtilities();
        //register the default utility
        addUtility(new DatabaseUtility());
        //add all extension point driver class names
        m_driverNames.addAll(DatabaseDriverLoader.getExtensionPointDriver());
    }

    /**
     * @return the only instance
     */
    static DatabaseUtilityRegistry getInstance() {
        return INSTANCE;
    }

    /**
     *
     */
    private void loadUtilities() {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;
        for (IExtension ext : point.getExtensions()) {
            final IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement utilityElement : elements) {
                try {
                    final DatabaseUtility utility = (DatabaseUtility)utilityElement.createExecutableExtension("class");
                    addUtility(utility);
                } catch (CoreException ex) {
                    NodeLogger.getLogger(DatabaseUtility.class).error(
                        "Could not create registered database utility "
                            + utilityElement.getAttribute("class") + " from plug-in "
                                + utilityElement.getNamespaceIdentifier() + ": " + ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * @param utility the {@link DatabaseUtility} to register
     */
    private void addUtility(final DatabaseUtility utility) {
        m_utilityMap.put(utility.getDatabaseIdentifier(), utility);
        final DBDriverFactory driverFactory = utility.getConnectionFactory().getDriverFactory();
        if (driverFactory instanceof PriorityDriverFactory) {
            final PriorityDriverFactory prioFact = (PriorityDriverFactory) driverFactory;
            for (final DBDriverFactory factory : prioFact.getFactories()) {
                if (!RegisteredDriversConnectionFactory.getInstance().equals(factory)) {
                    m_driverNames.addAll(factory.getDriverNames());
                }
            }
        } else if (!RegisteredDriversConnectionFactory.getInstance().equals(driverFactory)) {
            m_driverNames.addAll(driverFactory.getDriverNames());
        }
    }

    /**
     * @param dbIdentifier the database identifier
     * @return the corresponding {@link DatabaseUtility} or the default {@link DatabaseUtility} if none could be found
     */
    @SuppressWarnings("deprecation")
    DatabaseUtility getUtility(final String dbIdentifier) {
        DatabaseUtility utility = m_utilityMap.get(dbIdentifier);
        if (utility == null) {
            utility = new DatabaseUtility();
            m_utilityMap.put(dbIdentifier, utility);
        }
        return utility;
    }

    /**
     * @return
     */
    Collection<String> getDBIdentifier() {
        return m_utilityMap.keySet();
    }

    /**
     * @return all available driver names
     */
    Set<String> getJDBCDriverClasses() {
        final Set<String> names = new LinkedHashSet<String>(m_driverNames);
        //always get the user defined driver names fresh to also get recently registered drivers
        names.addAll(DatabaseDriverLoader.getUserDefinedDriver());
        final String[] driverNames = names.toArray(new String[0]);
        Arrays.sort(driverNames);
        names.clear();
        names.addAll(Arrays.asList(driverNames));
        return names;
    }
}

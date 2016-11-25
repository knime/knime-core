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
 *   16.11.2015 (koetter): created
 */
package org.knime.core.node.port.database;

import java.io.File;
import java.io.IOException;
import java.sql.Driver;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import org.knime.core.node.port.database.connection.DBDriverFactory;
import org.knime.core.node.port.database.connection.PriorityDriverFactory;

/**
 * {@link DBDriverFactory} implementation for jar drivers registered by the user or via the old jar extension point.
 * @author Tobias Koetter, KNIME.com
 * @since 3.2
 */
public final class RegisteredDriversConnectionFactory implements DBDriverFactory {

    private static final RegisteredDriversConnectionFactory INSTANCE = new RegisteredDriversConnectionFactory();

    /**
     *
     */
    private RegisteredDriversConnectionFactory() {
        //prevent object creation
    }

    /**
     * @return the instance
     */
    public static RegisteredDriversConnectionFactory getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public Set<String> getDriverNames() {
        return DatabaseDriverLoader.getLoadedDriver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver getDriver(final DatabaseConnectionSettings settings) throws Exception {
        try {
            final Driver d = DatabaseDriverLoader.getDriver(settings.getDriver());
            return d;
        } catch (Exception ex) {
            final DBDriverFactory factory = getLegacyFactory(settings);
            if (factory != null) {
                return factory.getDriver(settings);
            }
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<File> getDriverFiles(final DatabaseConnectionSettings settings) throws IOException {
      //locate the jdbc jar files
        try {
            final Collection<File> jars = new LinkedList<>();
            @SuppressWarnings("deprecation")
            final File[] driverFiles = DatabaseDriverLoader.getDriverFilesForDriverClass(settings.getDriver());
            if (driverFiles != null) {
                jars.addAll(Arrays.asList(driverFiles));
            } else {
                //the method returns null if no driver files where found try the legacy way
                final DBDriverFactory factory = getLegacyFactory(settings);
                if (factory != null) {
                    return factory.getDriverFiles(settings);
                }
            }
            return jars;
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * @param settings
     * @return
     */
    private DBDriverFactory getLegacyFactory(final DatabaseConnectionSettings settings) {
        //this is necessary when the user has set the database identifier to default and the driver
        //is provided by a specific DatabaseUtility e.g. SQLiteUtility instead of as before via the jdbc extension point.
        //The "default" DatabaseUtility class only searches for the driver in the RegisteredDriversConnectionFactory
        //which no longer contains the driver since the extension point based driver registration has been replaced
        //by the DatabaseUtility specific DriverFactories this is why we compare the jdbc based db identifier with
        //the given identifier and if they are different we also ask the DatabaseUtility for the jdbc based
        //database identifier for the connection
        if (DatabaseUtility.DEFAULT_DATABASE_IDENTIFIER.equals(settings.getDatabaseIdentifier())) {
            final String dbIDFromJDBCUrl =
                    DatabaseConnectionSettings.getDatabaseIdentifierFromJDBCUrl(settings.getJDBCUrl());
            //try to find the driver based on the id from the jdbc url
            final DatabaseUtility utility = DatabaseUtility.getUtility(dbIDFromJDBCUrl);
            final DBDriverFactory driverFactory = utility.getConnectionFactory().getDriverFactory();
            if (driverFactory instanceof PriorityDriverFactory) {
                final DBDriverFactory[] factories = ((PriorityDriverFactory)driverFactory).getFactories();
                for (DBDriverFactory factory : factories) {
                    //we can use object equality since this is a singleton
                    if (factory != this) {
                        return factory;
                    }
                }
            } else if (driverFactory != this) {
                return driverFactory;
            }
        }
        return null;
    }
}

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
 *   20.05.2016 (koetter): created
 */
package org.knime.core.node.port.database.connection;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.osgi.framework.Bundle;

/**
 * Default {@link DBDriverFactory} implementation that uses the old
 * <code>Class.forName()</code> method to create an instance of the db driver.
 * An extension of this class should be part of the eclipse plugin that has access
 * to the the driver files.
 * @author Tobias Koetter, KNIME.com
 * @since 3.2
 */
public class DefaultDBDriverFactory implements DBDriverFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultDBDriverFactory.class);

    private Collection<File> m_driverFiles;
    private Set<String> m_driverNames;

    /**
     * @param driverName the name of the driver class
     * @param bundleId the id of the eclipse bundle that contains the file
     * @param withinBundlePaths the paths of all JDBC driver files within the bundle
     * @throws IllegalStateException if an {@link IOException} occurs
     */
    public DefaultDBDriverFactory(final String driverName, final String bundleId, final String... withinBundlePaths) {
        this(driverName, getBundleFiles(bundleId, withinBundlePaths));
    }

    /**
     * @param driverName the name of the driver class
     * @param driverFile the jar file with the jdbc driver
     */
    public DefaultDBDriverFactory(final String driverName, final File driverFile) {
        this(driverName, Arrays.asList(new File[] {driverFile}));
    }

    /**
     * @param driverName the name of the driver class
     * @param driverFiles the files that are required by the driver e.g. the jar with the jdbc driver
     */
    public DefaultDBDriverFactory(final String driverName, final Collection<File> driverFiles) {
        if (driverName == null || driverName.isEmpty()) {
            throw new IllegalArgumentException("driverName must not be empty");
        }
        if (driverFiles == null || driverFiles.isEmpty()) {
            throw new IllegalArgumentException("driverFiles must not be empty");
        }
        m_driverFiles = driverFiles;
        m_driverNames = new HashSet<>();
        m_driverNames.add(driverName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getDriverNames() {
        return m_driverNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver getDriver(final DatabaseConnectionSettings settings) throws Exception {
//        final Class<?> c =  Class.forName(settings.getDriver(), true, getClass().getClassLoader());
        final String driverName = settings.getDriver();
        LOGGER.debug("Loading driver from DB factory: " + driverName);
        final Class<?> c =  Class.forName(driverName);
        final Driver d = (Driver)c.newInstance();
        LOGGER.debug("Driver loaded from DB factory: " + driverName);
        return d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<File> getDriverFiles(final DatabaseConnectionSettings settings) throws IOException {
        return m_driverFiles;
    }

    /**
     * @param bundleId the id of the eclipse bundle that contains the file
     * @param withinBundlePaths the paths of all JDBC driver files within the bundle
     * @return the {@link File}
     * @throws IllegalStateException if an {@link IOException} occurs
     */
    public static Collection<File> getBundleFiles(final String bundleId, final String... withinBundlePaths)
            throws IllegalStateException {
        if (withinBundlePaths == null) {
            throw new NullPointerException("withinBundlePaths must not be null");
        }
        if (bundleId == null || bundleId.isEmpty()) {
            throw new IllegalArgumentException("bundleId must not be empty");
        }
        try {
            final Bundle bundle = Platform.getBundle(bundleId);
            final Collection<File> files = new ArrayList<>(withinBundlePaths.length);
            for (String withinBundlePath : withinBundlePaths) {
                final URL jdbcUrl = FileLocator.find(bundle, new Path(withinBundlePath), null);
                files.add(new File(FileLocator.toFileURL(jdbcUrl).getPath()));
            }
            return files;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
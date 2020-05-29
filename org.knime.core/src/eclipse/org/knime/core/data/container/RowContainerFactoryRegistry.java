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
 * History
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.osgi.framework.FrameworkUtil;

/**
 * Collects extensions from the extension point and provides it to the framework.
 *
 * @author Christian Dietz, KNIME GmbH Konstanz
 * @since 4.2
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class RowContainerFactoryRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RowContainerFactoryRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.RowContainer";

    private static final IEclipsePreferences CORE_PREFS =
        InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(RowContainerFactoryRegistry.class).getSymbolicName());

    private static final IEclipsePreferences CORE_DEFAULT_PREFS =
        DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(RowContainerFactoryRegistry.class).getSymbolicName());

    /** Preference constant for selecting row container factory. */
    public static final String PREF_KEY_ROWCONTAINER_FACTORY = "knime.core.row-container-factory";

    private static RowContainerFactoryRegistry INSTANCE = createInstance();

    private static RowContainerFactoryRegistry createInstance() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        List<RowContainerFactory> factoryList = Stream.of(point.getExtensions())
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())).map(cfe -> readFactory(cfe))
            .filter(f -> f != null).sorted(Comparator.comparing(f -> f.getClass().getSimpleName(), (a, b) -> {
                // sort formats so that the "KNIME standard" format comes first.
                if (Objects.equals(a, b)) {
                    return 0;
                } else if (BufferedRowContainerFactory.class.getName().equals(a)) {
                    return -1;
                } else if (BufferedRowContainerFactory.class.getName().equals(b)) {
                    return +1;
                } else {
                    return a.compareTo(b);
                }
            })).collect(Collectors.toList());

        boolean hasFallback = factoryList.stream().anyMatch(f -> f.getClass().equals(BufferedRowContainerFactory.class));
        CheckUtils.checkState(hasFallback, "No fallback row container factory registered, expected '%s' but not present in '%s'",
            RowContainerFactory.class.getName(),
            StringUtils.join(factoryList.stream().map(f -> f.getClass().getName()).iterator(), ", "));

        return new RowContainerFactoryRegistry(factoryList);
    }

    private static RowContainerFactory readFactory(final IConfigurationElement cfe) {
        try {
            RowContainerFactory f = (RowContainerFactory)cfe.createExecutableExtension("factoryClass");
            LOGGER.debugWithFormat("Added row container factory '%s' from '%s'", f.getClass().getName(),
                cfe.getContributor().getName());
            return f;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s",
                RowContainerFactory.class.getName(), cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

    /** @return the instance to use. */
    public static RowContainerFactoryRegistry getInstance() {
        return INSTANCE;
    }

    private final List<RowContainerFactory> m_rowContainerFactories;

    private RowContainerFactoryRegistry(final List<RowContainerFactory> rowContainerFactories) {
        m_rowContainerFactories = Collections.unmodifiableList(rowContainerFactories);
    }

    /**
     * The 'default' row container factory as defined by the default preference scope, or the standard KNIME row container factory if unset. This
     * method is used by the preference page and should not be used by clients otherwise.
     *
     * @return non-null 'default' format.
     * @see #getInstanceRowContainerFactory()
     */
    public RowContainerFactory getDefaultRowContainerFactory() {
        String defaultFactoryClassName;
        if (KNIMEConstants.isNightlyBuild()) {
            defaultFactoryClassName = BufferedRowContainerFactory.class.getName();
            // TODO make this the nightly build default once we're there
            // defaultFactoryClassName = "XYZ Arrow";
        } else {
            defaultFactoryClassName = BufferedRowContainerFactory.class.getName();
        }
        String defaultID = CORE_DEFAULT_PREFS.get(PREF_KEY_ROWCONTAINER_FACTORY, defaultFactoryClassName);
        Optional<RowContainerFactory> defaultFactory =
            m_rowContainerFactories.stream().filter(f -> f.getClass().getName().equals(defaultID)).findFirst();
        if (!defaultFactory.isPresent()) {
            LOGGER.warnWithFormat("Invalid row container factory '%s' -- using standard KNIME data container instead.", defaultID);
        }
        return defaultFactory.orElse(m_rowContainerFactories.get(0));
    }

    /**
     * @return the row container factory as defined by the KNIME preferences or the default instead. This is is what is actually used
     *         by the core.
     */
    public RowContainerFactory getInstanceRowContainerFactory() {
        String result = CORE_PREFS.get(PREF_KEY_ROWCONTAINER_FACTORY, null); // instance scope prefs
        if (result == null) {
            return getDefaultRowContainerFactory();
        }
        final String resultFinal = result;
        Optional<RowContainerFactory> match =
            m_rowContainerFactories.stream().filter(f -> f.getClass().getName().equals(resultFinal)).findFirst();
        if (!match.isPresent()) {
            LOGGER.warnWithFormat("Invalid row container factory '%s' -- using standard KNIME data container instead.", result);
            return m_rowContainerFactories.get(0);
        }
        return match.get();
    }

    /** @return the row container factories in an unmodifiable list. */
    public List<RowContainerFactory> getRowContainerFactories() {
        return m_rowContainerFactories;
    }

    /**
     * @param spec the spec of the table to write.
     * @return the row container factories accepting to write that schema, if possible {@link #getInstanceRowContainerFactory()}.
     */
    public RowContainerFactory getRowContainerFactoryFor(final DataTableSpec spec) {
        RowContainerFactory instanceRowContainerFactory = getInstanceRowContainerFactory();
        if (instanceRowContainerFactory.supports(spec)) {
            return instanceRowContainerFactory;
        }
        return m_rowContainerFactories.stream().filter(f -> f.supports(spec)).findFirst()
            .orElseThrow(() -> new InternalError("No registered row container factory accepts the current table schema."));
    }

    /**
     * @param fullyQualifiedClassName class name in question
     * @return the row container factory with the given class name - used to restore a previously saved table.
     * @throws IllegalArgumentException If the factory is unknown (usually means: not installed)
     */
    public RowContainerFactory getRowContainerFactory(final String fullyQualifiedClassName)
        throws IllegalArgumentException {
        return m_rowContainerFactories.stream()//
            .filter(f -> f.getClass().getName().equals(fullyQualifiedClassName))//
            .findFirst()//
            .orElseThrow(() -> new IllegalArgumentException(fullyQualifiedClassName));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("RowContainer Factories: [");
        b.append(String.join(", ",
            m_rowContainerFactories.stream().map(s -> s.getClass().getName()).collect(Collectors.toList())))
            .append("]");
        return b.toString();
    }

}

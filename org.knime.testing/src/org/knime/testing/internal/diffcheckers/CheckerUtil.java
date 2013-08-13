/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   12.08.2013 (thor): created
 */
package org.knime.testing.internal.diffcheckers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.testing.core.DifferenceCheckerFactory;

/**
 * Utility class that load the available checkers via the extension point and provides access to the factories.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public final class CheckerUtil {
    private static final String EXT_POINT_ID = "org.knime.testing.DifferenceCheckerFactory";

    private static final String EXT_POINT_ATTR_DF = "class";

    private final Map<Class<? extends DataValue>, List<DifferenceCheckerFactory<? extends DataValue>>> m_checkersByType =
            new HashMap<Class<? extends DataValue>, List<DifferenceCheckerFactory<? extends DataValue>>>();

    private final Map<String, DifferenceCheckerFactory<? extends DataValue>> m_checkersByClass =
            new HashMap<String, DifferenceCheckerFactory<? extends DataValue>>();

    /** Public singleton instance of this class. */
    public static final CheckerUtil instance = new CheckerUtil();

    @SuppressWarnings("unchecked")
    private CheckerUtil() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            throw new IllegalStateException("ACTIVATION ERROR: --> Invalid extension point: " + EXT_POINT_ID);
        }

        for (IConfigurationElement elem : point.getConfigurationElements()) {
            String className = elem.getAttribute(EXT_POINT_ATTR_DF);

            if (className == null || className.isEmpty()) {
                throw new IllegalStateException("Missing required attribute '" + EXT_POINT_ATTR_DF + "' for extension "
                        + EXT_POINT_ID + " from " + elem.getContributor().getName());
            }

            DifferenceCheckerFactory<? extends DataValue> factory;
            try {
                factory =
                        (DifferenceCheckerFactory<? extends DataValue>)elem
                                .createExecutableExtension(EXT_POINT_ATTR_DF);
                m_checkersByClass.put(className, factory);
                List<DifferenceCheckerFactory<? extends DataValue>> l = m_checkersByType.get(factory.getType());
                if (l == null) {
                    l = new ArrayList<DifferenceCheckerFactory<? extends DataValue>>(2);
                    m_checkersByType.put(factory.getType(), l);
                }
                l.add(factory);
            } catch (CoreException ex) {
                NodeLogger.getLogger(getClass()).error("Could not create instance of " + className, ex);
            }
        }
    }

    /**
     * Returns a list of checker factories that can handle the given data type. The data type is asked for all its
     * value interfaces and all matching checker factories are returned. Therefore the list always includes at least
     * the factory for the {@link EqualityChecker} because it can be applied to any type.
     *
     * @param type a data type
     *
     * @return a non-empty list with checker factories
     */
    public List<DifferenceCheckerFactory<? extends DataValue>> getFactoryForType(final DataType type) {
        List<DifferenceCheckerFactory<? extends DataValue>> checkers =
                new ArrayList<DifferenceCheckerFactory<? extends DataValue>>();
        for (Class<? extends DataValue> valueClass : type.getValueClasses()) {
            List<DifferenceCheckerFactory<? extends DataValue>> l = m_checkersByType.get(valueClass);
            if (l != null) {
                checkers.addAll(l);
            }
        }

        return checkers;
    }

    /**
     * Returns the factory class for the given class name or <code>null</code> if no such factory is registered.
     *
     * @param className a class name
     * @return a factory class or <code>null</code>
     */
    public DifferenceCheckerFactory<? extends DataValue> getFactory(final String className) {
        return m_checkersByClass.get(className);
    }
}

/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.12.2014 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.base.node.preproc.pmml.missingval.handlers.DoNothingMissingCellHandlerFactory;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;

/**
 * Manager for missing cell handler factories that are provided by extensions.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class MissingCellHandlerFactoryManager {

    /** The id of the MissingCellHandler extension point. */
    private static final String EXT_POINT_ID = "org.knime.base.MissingCellHandler";

   /**
    * The attribute of the missing cell handler extension point pointing to the factory class.
    */
    private static final String EXT_POINT_ATTR_DF = "MissingCellHandlerFactory";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MissingCellHandlerFactoryManager.class);

    private static MissingCellHandlerFactoryManager instance;

    /** List of registered factories. **/
    private List<MissingCellHandlerFactory> m_factories
                = new ArrayList<MissingCellHandlerFactory>();

    /** Map for resolving registered factories by id. **/
    private Map<String, MissingCellHandlerFactory> m_factoryNameMap
                = new HashMap<String, MissingCellHandlerFactory>();

    /**
     * protected constructor because this class is a singleton.
     * @param extPointId id of the extension point
     * @param extPointAttrDf attribute of the factory class within the handler extension point
     */
    protected MissingCellHandlerFactoryManager(final String extPointId, final String extPointAttrDf) {
        registerExtensionPoints(extPointId, extPointAttrDf);
    }

    /**
     * Singleton instance of the MissingCellHandlerManager.
     * @return the instance of the MissingCellHandlerManager
     */
    public static MissingCellHandlerFactoryManager getInstance() {
        if (instance == null) {
            instance = new MissingCellHandlerFactoryManager(EXT_POINT_ID, EXT_POINT_ATTR_DF);
        }
        return instance;
    }

    /**
     * @return all factories managed by this class.
     */
    public List<MissingCellHandlerFactory> getFactories() {
        return m_factories;
    }

    /**
     * Registers a handler factory.
     * @param factory the factory to register
     */
    private void addMissingCellHandlerFactory(final MissingCellHandlerFactory factory) {
        m_factories.add(factory);
        m_factoryNameMap.put(factory.getID(), factory);
    }

    /**
     * Registers all extension point implementations.
     * @param extPointId id of the extension point
     * @param extPointAttrDf attribute of the factory class within the handler extension point
     */
    protected void registerExtensionPoints(final String extPointId, final String extPointAttrDf) {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(extPointId);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + extPointId);
                throw new IllegalStateException("ACTIVATION ERROR: " + " --> Invalid extension point: " + extPointId);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String operator = elem.getAttribute(extPointAttrDf);
                final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

                if ((operator == null) || operator.isEmpty()) {
                    LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                            + extPointAttrDf + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }

                try {
                    final MissingCellHandlerFactory factory =
                        (MissingCellHandlerFactory)elem.createExecutableExtension(extPointAttrDf);
                    addMissingCellHandlerFactory(factory);
                } catch (final Exception t) {
                    LOGGER.error("Problems during initialization of missing value handler factory (with id '"
                                + operator + "'.)");
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", t);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering MissingCellHandler extensions");
        }
    }

    /**
     * Returns a list of missing cell handler factories for a certain data type.
     * @param type the data type
     * @return the list of suitable missing cell handler factories
     */
    public List<MissingCellHandlerFactory> getFactories(final DataType type) {
        List<MissingCellHandlerFactory> factories = new ArrayList<MissingCellHandlerFactory>();

        // Add the do nothing default factory
        factories.add(getDoNothingHandlerFactory());

        for (MissingCellHandlerFactory fac : m_factories) {
            if (fac.isApplicable(type)) {
                factories.add(fac);
            }
        }
        return factories;
    }

    /**
     * Returns a list of missing value handler factories for certain data types sorted by display name.
     *
     * @param types the data types
     * @return the list of suitable missing cell handler factories
     */
    public List<MissingCellHandlerFactory> getFactoriesSorted(final DataType[] types) {
        List<MissingCellHandlerFactory> factories = new ArrayList<MissingCellHandlerFactory>();

        // Add the do nothing default factory
        factories.add(getDoNothingHandlerFactory());

        for (MissingCellHandlerFactory fac : m_factories) {
            boolean isApplicable = true;
            for (DataType type : types) {
                if (!fac.isApplicable(type)) {
                    isApplicable = false;
                }
            }
            if (isApplicable) {
                factories.add(fac);
            }
        }

        factories.sort(new Comparator<MissingCellHandlerFactory>() {
            @Override
            public int compare(final MissingCellHandlerFactory a, final MissingCellHandlerFactory b) {
                return a.getDisplayName().compareTo(b.getDisplayName());
            }
        });

        return factories;
    }

    /**
     * Returns a factory instance with the given class name.
     * @param id the id of the factory
     * @return the factory with the given name
     */
    public MissingCellHandlerFactory getFactoryByID(final String id) {
        if (id.equals(getDoNothingHandlerFactoryId())) {
            return getDoNothingHandlerFactory();
        }
        return m_factoryNameMap.get(id);
    }

    /** @return true if one or more handler produce non standard PMML */
    public boolean hasNonStandardPMMLHandlers() {
        for (MissingCellHandlerFactory handler : getFactories()) {
            if (!handler.producesPMML4_2()) {
                return true;
            }
        }

        return false;
    }

    /** @return id of do nothing handler factory */
    protected String getDoNothingHandlerFactoryId() {
        return DoNothingMissingCellHandlerFactory.ID;
    }

    /** @return instance of do nothing handler factory */
    protected MissingCellHandlerFactory getDoNothingHandlerFactory() {
        return DoNothingMissingCellHandlerFactory.getInstance();
    }
}

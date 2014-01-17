/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * Created on 23.05.2013 by thor
 */
package org.knime.core.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.8
 */
public abstract class ExtensibleUtilityFactory extends UtilityFactory {
    private static final String EXT_POINT_ID = "org.knime.core.DataValueRenderer";

    private static final IEclipsePreferences CORE_PREFS = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(
        DataValueRendererFactory.class).getSymbolicName());

    private static final IEclipsePreferences CORE_DEFAULT_PREFS = DefaultScope.INSTANCE.getNode(FrameworkUtil
        .getBundle(DataValueRendererFactory.class).getSymbolicName());

    /**
     * A logger for this factory.
     */
    protected final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    /**
     * The value class this utility factory is responsible for.
     */
    protected final Class<? extends DataValue> m_valueClass;

    private final Map<String, DataValueRendererFactory> m_renderers = new HashMap<String, DataValueRendererFactory>();

    private static final Map<Class<? extends DataValue>, ExtensibleUtilityFactory> ALL_FACTORIES =
        new HashMap<Class<? extends DataValue>, ExtensibleUtilityFactory>();

    /**
     * Creates a new utility factory that is responsible for the given value class.
     *
     * @param valueClass the value class for which this factory is responsible
     */
    public ExtensibleUtilityFactory(final Class<? extends DataValue> valueClass) {
        m_valueClass = valueClass;
        readRenderersFromExtensionPoint();

        synchronized (ALL_FACTORIES) {
            if (ALL_FACTORIES.containsKey(valueClass)) {
                ExtensibleUtilityFactory existingFactory = ALL_FACTORIES.get(valueClass);

                throw new IllegalArgumentException("There is already a factory registered for " + valueClass + ": "
                    + existingFactory.getClass().getName());
            } else {
                ALL_FACTORIES.put(valueClass, this);
            }
        }
    }

    /**
     * Reads and sets the preferred renderers from the preferences.
     *
     * @deprecated this method is only used for reading old renderer settings from the MoleculeUtilityFactory. Do not
     *             call or override this method.
     */
    @Deprecated
    protected void readPreferredRendererFromPreferences() {
        // nothing to do here any more
    }

    /**
     * Sets the preferred renderer using the ID of its factory. Please think twice before using this method since the
     * preferred renderer should usually only be changed by the user via the preference page.
     *
     * @param rendererId the factory's ID name or <code>null</code> if the preferred renderer should be unset
     */
    public final void setPreferredRenderer(final String rendererId) {
        if (rendererId == null) {
            CORE_PREFS.put(getPreferenceKey(), null);
        } else {
            DataValueRendererFactory matchedFactory = m_renderers.get(rendererId);
            if (matchedFactory != null) {
                CORE_PREFS.put(getPreferenceKey(), rendererId);
                m_logger.debug("Setting " + rendererId + " as preferred renderer for " + m_valueClass.getName());
            } else {
                m_logger.warn("Preferred renderer " + rendererId + " for " + m_valueClass.getName() + " was not found");
            }
        }
    }

    /**
     * Sets the default renderer using the ID of its factory. The default renderer must never be <code>null</code> and
     * it must exist.
     *
     * @param rendererId the factory's ID name
     * @since 2.9
     */
    public final void setDefaultRenderer(final String rendererId) {
        if (rendererId == null) {
            throw new IllegalArgumentException("Default renderer ID must not be null");
        } else {
            DataValueRendererFactory matchedFactory = m_renderers.get(rendererId);
            if (matchedFactory != null) {
                CORE_DEFAULT_PREFS.put(getPreferenceKey(), rendererId);
                m_logger.debug("Setting " + rendererId + " as default renderer for " + m_valueClass.getName());
            } else {
                throw new IllegalArgumentException("Default renderer with ID '" + rendererId + "' does not exist");
            }
        }
    }


    private void readRenderersFromExtensionPoint() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

        String defaultRendererId = null;
        for (IExtension ext : point.getExtensions()) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement valueClassElement : elements) {
                String valueClassName = valueClassElement.getAttribute("valueClass");
                if (!m_valueClass.getName().equals(valueClassName)) {
                    continue;
                }

                for (IConfigurationElement rendererClassElement : valueClassElement.getChildren()) {
                    boolean suggestAsDefault =
                        Boolean.parseBoolean(rendererClassElement.getAttribute("suggestAsDefault"));

                    try {
                        DataValueRendererFactory rendererFactory =
                            (DataValueRendererFactory)rendererClassElement
                                .createExecutableExtension("rendererFactoryClass");

                        m_renderers.put(rendererFactory.getId(), rendererFactory);

                        if (suggestAsDefault || (defaultRendererId == null)) {
                            defaultRendererId = rendererFactory.getId();
                            CORE_DEFAULT_PREFS.put(getPreferenceKey(), defaultRendererId);
                        }
                    } catch (CoreException ex) {
                        m_logger.error(
                            "Could not load registered renderer factory "
                                + rendererClassElement.getAttribute("rendererFactoryClass") + " for " + valueClassName
                                + " from plug-in " + valueClassElement.getNamespaceIdentifier() + ": "
                                + ex.getMessage(), ex);
                    }
                }
            }
        }
    }

    /**
     * Adds a factory to the list of available renderers. This method should not be used because all renderers are
     * better registered via the extension point.
     *
     * @param fac a renderer factory
     * @param suggestAsDefault <code>true</code> if this renderer should be used as default renderer, <code>false</code>
     *            otherwise. This will override any existing default renderer.
     */
    protected final void addRendererFactory(final DataValueRendererFactory fac, final boolean suggestAsDefault) {
        m_renderers.put(fac.getId(), fac);
        if (suggestAsDefault) {
            CORE_DEFAULT_PREFS.put(getPreferenceKey(), fac.getId());
        }
    }

    /**
     * Returns a (unmodifiable) set of renderer factories for this data value. If no renderer factories are registered
     * (highly unlikely), an empty set is returned.
     *
     * @return a set of renderer factories
     */
    public Collection<DataValueRendererFactory> getAvailableRenderers() {
        if (m_renderers.isEmpty()) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableCollection(m_renderers.values());
        }
    }

    /**
     * Returns the default renderer for this data value. If no renderer is registered with the data value (highly
     * unlikely) <code>null</code> is returned.
     *
     * @return a renderer factory or <code>null</code>
     */
    public DataValueRendererFactory getDefaultRenderer() {
        String defaultId = CORE_DEFAULT_PREFS.get(getPreferenceKey(), null);

        DataValueRendererFactory matchedFactory = m_renderers.get(defaultId);
        if (matchedFactory != null) {
            return matchedFactory;
        } else {
            return null;
        }
    }

    /**
     * Returns the preferred renderer for this data value. If no preferred renderer is set for the data value,
     * <code>null</code> is returned.
     *
     * @return a renderer factory or <code>null</code>
     */
    public DataValueRendererFactory getPreferredRenderer() {
        String prefId = CORE_PREFS.get(getPreferenceKey(), null);

        DataValueRendererFactory matchedFactory = m_renderers.get(prefId);
        if (matchedFactory != null) {
            return matchedFactory;
        } else {
            return null;
        }
    }

    /**
     * Returns a name for the data value this utility factory is responsible for.
     *
     * @return a (short) name
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    @Override
    protected final DataValueRendererFamily getRendererFamily(final DataColumnSpec spec) {
        DataValueRenderer[] renderers = new DataValueRenderer[m_renderers.size()];
        int i = 0;
        readPreferredRendererFromPreferences();
        DataValueRendererFactory prefRenderer = getPreferredRenderer();
        DataValueRendererFactory defaultRenderer = getDefaultRenderer();

        if (prefRenderer != null) {
            renderers[i++] = prefRenderer.createRenderer(spec);
        } else if (defaultRenderer != null) {
            renderers[i++] = defaultRenderer.createRenderer(spec);
        }
        for (DataValueRendererFactory fac : m_renderers.values()) {
            if (fac.equals(prefRenderer) || ((prefRenderer == null) && fac.equals(defaultRenderer))) {
                continue;
            }
            renderers[i++] = fac.createRenderer(spec);
        }

        return new DefaultDataValueRendererFamily(renderers);
    }

    /**
     * Returns the preference key for the preferred renderer for this factory's value class.
     *
     * @return the preference key
     */
    public final String getPreferenceKey() {
        return getPreferenceKey(m_valueClass);
    }

    /**
     * Returns a list with all existing utility factories.
     *
     * @return an (unmodifiable) list of utility factories
     */
    public static Collection<? extends ExtensibleUtilityFactory> getAllFactories() {
        loadAllFactories();
        synchronized (ALL_FACTORIES) {
            return Collections.unmodifiableCollection(ALL_FACTORIES.values());
        }
    }

    /**
     * Returns the preference key for the preferred renderer for the given value class.
     *
     * @param valueClass any data value class
     * @return the preference key
     */
    public static final String getPreferenceKey(final Class<? extends DataValue> valueClass) {
        return "preferredRendererFor_" + valueClass.getName();
    }

    /*
     * Load all extensible utility factories by traversing the renderer extension point. Not all factories may be
     * loaded if nobody has access the data type yet.
     * TODO This should be changed once we have an extension point for data types
     */
    @SuppressWarnings("unchecked")
    private static void loadAllFactories() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

        for (IExtension ext : point.getExtensions()) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement valueClassElement : elements) {
                String valueClassName = valueClassElement.getAttribute("valueClass");
                try {
                    DataType.getUtilityFor((Class<? extends DataValue>)Class.forName(valueClassName));
                } catch (ClassNotFoundException ex) {
                    NodeLogger.getLogger(ExtensibleUtilityFactory.class).coding(
                        "Could not find implementation for " + valueClassName, ex);
                }
            }
        }
    }

    /**
     * Returns the name of a group the data value belongs to. This group name is used e.g. in the preferences in order
     * to group data types. You can define you own group but returning a new, yet unused group name. But beware of
     * spelling errors if you want to join an existing group.<br />
     * The default group if this method is not overridden is <em>Other</em>.
     *
     * @return a group name
     */
    public String getGroupName() {
        return "Other";
    }
}

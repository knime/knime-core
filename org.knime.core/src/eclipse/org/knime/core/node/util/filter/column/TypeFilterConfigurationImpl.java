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
 * Created on Oct 10, 2013 by Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.util.filter.column;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.InputFilter;

/**
 * Configuration to the {@link TypeFilterPanelImpl}.
 *
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
final class TypeFilterConfigurationImpl implements Cloneable {

    /** The identifier for this filter type. */
    static final String TYPE = "datatype";

    private static final String CFG_TYPELIST = "typelist";

    private LinkedHashMap<String, Boolean> m_selections = new LinkedHashMap<String, Boolean>();

    private final InputFilter<Class<? extends DataValue>> m_filter;

    /**
     * Creates a configuration to the DataValue filter panel.
     *
     * @param filter The filter to use, if null no filter will be applied.
     */
    TypeFilterConfigurationImpl(final InputFilter<Class<? extends DataValue>> filter) {
        m_filter = filter;
    }

    /**
     * Loads the configuration from the given settings object. Fails if not valid.
     *
     * @param settings Settings object containing the configuration.
     * @throws InvalidSettingsException If settings are invalid
     */
    void loadConfigurationInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selections.clear();
        NodeSettingsRO typeListSettings = settings.getNodeSettings(CFG_TYPELIST);
        for (String key : typeListSettings.keySet()) {
            m_selections.put(key, typeListSettings.getBoolean(key, false));
        }
    }

    /**
     * Loads the configuration from the given settings object. Sets defaults if invalid.
     * @param settings Settings object containing the configuration.
     * @param tableSpec The column specs to find the available types.
     */
    void loadConfigurationInDialog(final NodeSettingsRO settings, final DataTableSpec tableSpec) {
        m_selections.clear();
        NodeSettingsRO typeListSettings;
        try {
            typeListSettings = settings.getNodeSettings(CFG_TYPELIST);
            for (String key : typeListSettings.keySet()) {
                m_selections.put(key, typeListSettings.getBoolean(key, false));
            }
        } catch (InvalidSettingsException e) {
            // ignore
        }
    }

    /** Save the current configuration inside the given settings object.
     * @param settings Settings object the current configuration will be put into. */
    void saveConfiguration(final NodeSettingsWO settings) {
        NodeSettingsWO typeListSettings = settings.addNodeSettings(CFG_TYPELIST);
        for (Map.Entry<String, Boolean> entry : m_selections.entrySet()) {
            typeListSettings.addBoolean(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Applies this configuration to the given collection of data values.
     *
     * @param values The data values to check
     * @return The set of values that were accepted
     */
    Set<Class<? extends DataValue>> applyTo(final Collection<Class<? extends DataValue>> values) {
        Set<Class<? extends DataValue>> includes = new HashSet<Class<? extends DataValue>>();
        for (Class<? extends DataValue> value : values) {
            if (m_filter == null || m_filter.include(value)) {
                String key = value.getName();
                if (m_selections.containsKey(key) && m_selections.get(key)) {
                    includes.add(value);
                }
            }
        }
        return includes;
    }

    /**
     * @return the selections
     */
    Map<String, Boolean> getSelections() {
        return m_selections;
    }

    /**
     * @param selections the selections to set
     */
    void setSelections(final LinkedHashMap<String, Boolean> selections) {
        m_selections = selections;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    protected TypeFilterConfigurationImpl clone() {
        try {
            TypeFilterConfigurationImpl clone = (TypeFilterConfigurationImpl)super.clone();
            clone.m_selections = (LinkedHashMap<String, Boolean>)m_selections.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Object not clonable although it implements java.lang.Clonable", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypeFilterConfigurationImpl other = (TypeFilterConfigurationImpl)obj;
        if (!m_selections.equals(other.m_selections)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_selections.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String type : m_selections.keySet()) {
            if (m_selections.get(type).booleanValue()) {
                builder.append(", " + type);
            }
        }
        return "Selected types: " + builder.toString().replaceFirst(", ", "");
    }

}

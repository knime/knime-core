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
 *   May 1, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.appendvariabletotable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.Pair;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class AppendVariableToTableSettings {
    
    private final List<Pair<String, FlowVariable.Type>> m_variablesOfInterest
        = new ArrayList<Pair<String, FlowVariable.Type>>();
    
    /** @param variablesOfInterest the variablesOfInterest to set */
    public void setVariablesOfInterest(
            final FlowVariable[] variablesOfInterest) {
        m_variablesOfInterest.clear();
        for (FlowVariable v : variablesOfInterest) {
            m_variablesOfInterest.add(new Pair<String, FlowVariable.Type>(
                    v.getName(), v.getType()));
        }
    }
    
    /** @return the variablesOfInterest */
    public List<Pair<String, FlowVariable.Type>> getVariablesOfInterest() {
        return m_variablesOfInterest;
    }
    
    /** @param settings to save to. */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        NodeSettingsWO sub = settings.addNodeSettings("variables");
        for (Pair<String, FlowVariable.Type> v : m_variablesOfInterest) {
            NodeSettingsWO sub2 = sub.addNodeSettings(v.getFirst());
            sub2.addString("name", v.getFirst());
            sub2.addString("type", v.getSecond().name());
        }
    }
    
    /** Loads settings.
     * @param settings to load from
     * @throws InvalidSettingsException if settings not present
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        m_variablesOfInterest.clear();
        NodeSettingsRO sub = settings.getNodeSettings("variables");
        if (sub == null) {
            throw new InvalidSettingsException("No settings available");
        }
        for (String key : sub.keySet()) {
            NodeSettingsRO sub2 = sub.getNodeSettings(key);
            String name = sub2.getString("name");
            String typeS = sub2.getString("type");
            if (name == null || typeS == null) {
                throw new InvalidSettingsException(
                        "Name and type must not be null.");
            }
            FlowVariable.Type type;
            try {
                type = FlowVariable.Type.valueOf(typeS);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException(
                        "Can't parse type: " + typeS);
            }
            m_variablesOfInterest.add(
                    new Pair<String, FlowVariable.Type>(name, type));
        }
    }
    
    /**
     * Load settings.
     * @param settings to load
     * @param scopeVariableMap map of keys to scope variables
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, 
            final Map<String, FlowVariable> scopeVariableMap) {
        m_variablesOfInterest.clear();
        NodeSettingsRO sub = null;
        Set<String> keySet;
        try {
            sub = settings.getNodeSettings("variables");
        } catch (InvalidSettingsException e) {
        }
        if (sub == null) {
            keySet = Collections.emptySet();
        } else {
            keySet = sub.keySet();
        }
        for (String key : keySet) {
            NodeSettingsRO sub2;
            try {
                sub2 = sub.getNodeSettings(key);
            } catch (InvalidSettingsException e) {
                continue;
            }
            String name = sub2.getString("name", null);
            String typeS = sub2.getString("type", null);
            if (name == null || typeS == null) {
                continue;
            }
            FlowVariable.Type type;
            try {
                type = FlowVariable.Type.valueOf(typeS);
            } catch (IllegalArgumentException iae) {
                continue;
            }
            m_variablesOfInterest.add(
                    new Pair<String, FlowVariable.Type>(name, type));
        }
    }

}

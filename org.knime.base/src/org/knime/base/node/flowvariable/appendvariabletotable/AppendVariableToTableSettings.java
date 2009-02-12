/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.util.Pair;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class AppendVariableToTableSettings {
    
    private final List<Pair<String, ScopeVariable.Type>> m_variablesOfInterest
        = new ArrayList<Pair<String, ScopeVariable.Type>>();
    
    /** @param variablesOfInterest the variablesOfInterest to set */
    public void setVariablesOfInterest(
            final ScopeVariable[] variablesOfInterest) {
        m_variablesOfInterest.clear();
        for (ScopeVariable v : variablesOfInterest) {
            m_variablesOfInterest.add(new Pair<String, ScopeVariable.Type>(
                    v.getName(), v.getType()));
        }
    }
    
    /** @return the variablesOfInterest */
    public List<Pair<String, ScopeVariable.Type>> getVariablesOfInterest() {
        return m_variablesOfInterest;
    }
    
    /** @param settings to save to. */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        NodeSettingsWO sub = settings.addNodeSettings("variables");
        for (Pair<String, ScopeVariable.Type> v : m_variablesOfInterest) {
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
            ScopeVariable.Type type;
            try {
                type = ScopeVariable.Type.valueOf(typeS);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException(
                        "Can't parse type: " + typeS);
            }
            m_variablesOfInterest.add(
                    new Pair<String, ScopeVariable.Type>(name, type));
        }
    }
    
    /**
     * Load settings.
     * @param settings to load
     * @param scopeVariableMap map of keys to scope variables
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, 
            final Map<String, ScopeVariable> scopeVariableMap) {
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
            ScopeVariable.Type type;
            try {
                type = ScopeVariable.Type.valueOf(typeS);
            } catch (IllegalArgumentException iae) {
                continue;
            }
            m_variablesOfInterest.add(
                    new Pair<String, ScopeVariable.Type>(name, type));
        }
    }

}

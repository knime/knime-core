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
 *   14.07.2009 (mb): created
 */
package org.knime.core.node;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.config.ConfigEditTreeModel;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.workflow.FlowVariable;


/** Container holding information regarding variables which represent
 * settings of a node and/or are used to replace settings of a node.
 *
 * This allows NodeDialogPane implementations to encapsulate all of the
 * information related to variable/settings replacements.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class FlowVariableModel {

    // private members
    private NodeDialogPane m_parent;
    private String[] m_keys;  // the hierarchy of Config Keys for this object
    private FlowVariable.Type m_type;   // the class of the variable

    /* variable names that are to be used for the corresponding settings
     * as "input" resp. "output". If one or both are null, the replacement
     * will not happen.
     */
    private String m_inputVariableName;
    private String m_outputVariableName;

    private final CopyOnWriteArrayList<ChangeListener> m_listeners;

    /** Create a new WVM object.
     * 
     * @param parent NodeDialogPane (needed to retrieve visible variables)
     * @param keys of corresponding settings object
     * @param type of variable/settings object
     */
    FlowVariableModel(final NodeDialogPane parent, final String[] keys,
            final FlowVariable.Type type) {
        m_parent = parent;
        m_keys = keys.clone();
        m_type = type;
        m_listeners = new CopyOnWriteArrayList<ChangeListener>();
    }

    /**
     *  @return parent NodeDialogPane
     */
    NodeDialogPane getParent() {
        return m_parent;
    }

    /**
     * @return the key of the corresponding setting object.
     */
    public String[] getKeys() {
        return m_keys;
    }

    /**
     * @return the type of the variable/settings object.
     */
    public FlowVariable.Type getType() {
        return m_type;
    }

    /**
     * @return the inputVariableName
     */
    public String getInputVariableName() {
        return m_inputVariableName;
    }

    /**
     * @param variableName the inputVariableName to set. Set to null
     * if no replacement is wanted.
     */
    public void setInputVariableName(final String variableName) {
        
        if (!ConvenienceMethods.areEqual(variableName, m_inputVariableName)) {
            m_inputVariableName = variableName;
            notifyChangeListeners();
        }
    }
    
    /**
     * @return true if variable replacement is activated.
     */
    public boolean isVariableReplacementEnabled() {
        return getInputVariableName() != null;
    }

    /**
     * @return the outputVariableName
     */
    public String getOutputVariableName() {
        return m_outputVariableName;
    }

    /**
     * @param variableName the outputVariableName to set. Set to null
     * if no replacement is wanted.
     */
    public void setOutputVariableName(final String variableName) {
        if (!ConvenienceMethods.areEqual(variableName, m_outputVariableName)) {
            m_outputVariableName = variableName;
            notifyChangeListeners();
        }
    }

    /**
     * @return array of variables names that match the type of this model.
     */
    FlowVariable[] getMatchingVariables() {
        ArrayList<FlowVariable> list = new ArrayList<FlowVariable>();
        for (FlowVariable sv 
                : getParent().getAvailableFlowVariables().values()) {
            if (ConfigEditTreeModel.doesTypeAccept(m_type, sv.getType())) {
                list.add(sv);
            }
        }
        return list.toArray(new FlowVariable[list.size()]);
    }
    
    /**
     * Adds a listener which is notified whenever a new value is set in the
     * model. Does nothing if the listener is already registered.
     * 
     * @param l listener to add.
     */
    public void addChangeListener(final ChangeListener l) {
        if (!m_listeners.contains(l)) {
            m_listeners.add(l);
        }
    }

    /**
     * Remove a specific listener.
     * 
     * @param l listener to remove.
     */
    public void removeChangeListener(final ChangeListener l) {
        m_listeners.remove(l);
    }

    /**
     * Notifies all registered listeners about a new model content.
     */
    protected void notifyChangeListeners() {
        for (ChangeListener l : m_listeners) {
            l.stateChanged(new ChangeEvent(this));
        }
    }
    
}

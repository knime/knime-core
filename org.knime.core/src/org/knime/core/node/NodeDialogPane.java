/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node;

import java.util.ArrayList;
import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.ScopeObjectStack;


/**
 * The base class for all node dialogs. It provides a tabbed pane to which the
 * derived dialog can add its own components (method
 * <code>addTab(String,Component)</code>). Methods <code>#onOpen()</code>
 * and <code>#onClose()</code> are to be implemented, to setup and cleanup the
 * dialog. A method <code>#onApply()</code> must be provided by the derived
 * class to transfer the user choices into the node's model.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeDialogPane extends GenericNodeDialogPane {
    
    /** Remember types in input spec. We use the type information to figure
     * out, which of the entries in the spec array are of type DataTableSpec
     * (can't use instanceof as the entry may be null).
     */
    private PortObjectSpec[] m_latestInSpecs;
    private PortType[] m_lastestInTypes;
    
    /** {@inheritDoc} */
    @Override
    void internalLoadSettingsFrom(final NodeSettingsRO settings,
            final PortType[] portTypes, final PortObjectSpec[] specs,
            final ScopeObjectStack scopeStack) throws NotConfigurableException {
        m_latestInSpecs = specs;
        assert portTypes != null : "Port Types are null";
        if (m_lastestInTypes != null) {
            assert Arrays.equals(portTypes, m_lastestInTypes)
            : "Changing inport types in dialog, it was \""
                + Arrays.toString(m_latestInSpecs) + "\", but it is \""
                + Arrays.toString(portTypes) + "\"";
        }
        m_lastestInTypes = portTypes;
        super.internalLoadSettingsFrom(settings, portTypes, specs, scopeStack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        assert Arrays.equals(specs, m_latestInSpecs) 
            : "Input specs changed when they were not supposed to change.";
        // get the data specs into a new (possibly smaller) array
        ArrayList<DataTableSpec> dataSpecList = new ArrayList<DataTableSpec>();
        for (int i = 0; i < m_lastestInTypes.length; i++) {
            if (m_lastestInTypes[i].getPortObjectSpecClass().equals(
                    DataTableSpec.class)) {
                dataSpecList.add((DataTableSpec)specs[i]);
            }
        }
        DataTableSpec[] dataSpecs = dataSpecList.toArray(
                new DataTableSpec[dataSpecList.size()]);
        loadSettingsFrom(settings, dataSpecs);
    }

    protected abstract void loadSettingsFrom(NodeSettingsRO settings,
            DataTableSpec[] specs) throws NotConfigurableException;


}

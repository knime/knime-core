/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jun 19, 2008 (wiswedel): created
 */
package org.knime.core.node;

import java.util.Arrays;

import org.knime.core.node.workflow.ScopeObjectStack;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ParameterizedNodeDialogPane extends GenericNodeDialogPane {
    
    private final GenericNodeDialogPane m_delegate;
    
    /**
     * 
     */
    public ParameterizedNodeDialogPane(final GenericNodeDialogPane delegate) {
        m_delegate = delegate;
        addTab("Delegate", m_delegate.getPanel());
    }
    
    /** {@inheritDoc} */
    @Override
    void internalLoadSettingsFrom(final NodeSettingsRO settings,
            final PortType[] portTypes, final PortObjectSpec[] specs,
            final ScopeObjectStack scopeStack) throws NotConfigurableException {
        PortType[] subType = Arrays.copyOfRange(portTypes, 1, portTypes.length);
        PortObjectSpec[] subSpec = Arrays.copyOfRange(specs, 1, specs.length);
        m_delegate.internalLoadSettingsFrom(
                settings, subType, subSpec, scopeStack);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        assert false : "This method is not supposed to be called";
    }
    
    /** {@inheritDoc} */
    @Override
    void internalSaveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_delegate.finishEditingAndSaveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert false : "This method is not supposed to be called";
    }

}

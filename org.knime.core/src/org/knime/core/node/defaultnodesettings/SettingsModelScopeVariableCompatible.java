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
 *   16.07.2009 (mb): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.workflow.ScopeVariable;

/** Interface for a @link SettingsModel which can be represented by a
 * ScopeVariable.
 *
 * This allows DefaultNodeDialogPane implementations to easily use Variables
 * for compatible SettingsModels/DialogComponents.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public interface SettingsModelScopeVariableCompatible {
    
    /**
     * @return key of the settings object.
     */
    public String getKey();
    
    /**
     * @return required type of the ScopeVariable replacing these settings.
     */
    public ScopeVariable.Type getScopeVariableType();
}

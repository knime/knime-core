/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Sep 18, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.List;

import org.knime.core.node.Node;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;


/**
 * 
 * @author wiswedel, University of Konstanz
 */
interface SingleNodeContainerPersistor extends NodeContainerPersistor {
    
    /** Name of the settings file in a node's directory. */
    static final String SETTINGS_FILE_NAME = "settings.xml";
    
    /** Key for the factory class name, used to load nodes. */
    static final String KEY_FACTORY_NAME = "factory";

    Node getNode();
    
    SingleNodeContainerSettings getSNCSettings();
    
    List<ScopeObject> getScopeObjects();
}

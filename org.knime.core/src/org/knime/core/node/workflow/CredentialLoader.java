/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2010
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
 */
package org.knime.core.node.workflow;

import java.util.List;

/**
 * Class is used as a callback handler to load credentials during loading
 * of a workflow. 
 * @author Thomas Gabriel, KNIME GmbH, Germany
 */
public interface CredentialLoader {

    /**
     * Caller method invoked when credentials are needed during loading
     * of a workflow.
     * @param credentials to be initialized
     * @return a list of new <code>Credentials</code>
     */
    List<Credentials> load(final List<Credentials> credentials);
    
}

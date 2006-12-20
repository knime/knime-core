/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   Dec 20, 2006 (wiswedel): created
 */
package org.knime.deprecated;

import org.eclipse.core.runtime.Plugin;
import org.knime.base.node.preproc.nominal.NominalValueFactory;
import org.knime.base.node.preproc.normalize.NormalizeNodeFactory;
import org.knime.core.node.NodeFactory;
import org.osgi.framework.BundleContext;


/**
 * This class is solely for registering the deprecated node factories.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DeprecatedPlugin extends Plugin {
    /**
     * @see Plugin#start(BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        NodeFactory.addLoadedFactory(NominalValueFactory.class);
        NodeFactory.addLoadedFactory(NormalizeNodeFactory.class);
    }
}

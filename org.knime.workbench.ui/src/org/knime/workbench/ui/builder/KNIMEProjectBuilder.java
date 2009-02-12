/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   11.03.2005 (georg): created
 */
package org.knime.workbench.ui.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.NodeLogger;

import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Builder for KNIME Projects. TODO Not used yet - may be used e.g. to validate
 * project/workflows and attach resource markers to the .knime files.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEProjectBuilder extends IncrementalProjectBuilder {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(KNIMEProjectBuilder.class);

    /**
     * Builder id.
     * 
     * NOTE: This is always constructed by the ID of the plugin(!) + the ID as
     * defined in plugin.xml !
     */
    public static final String BUILDER_ID = KNIMEUIPlugin.PLUGIN_ID
            + ".KNIMEProjectBuilder";

    /**
     * Constructor.
     */
    public KNIMEProjectBuilder() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IProject[] build(final int kind, final Map args,
            final IProgressMonitor monitor) throws CoreException {

        LOGGER.debug("KNIME project builder invoked...");

        return new IProject[0];
    }
}

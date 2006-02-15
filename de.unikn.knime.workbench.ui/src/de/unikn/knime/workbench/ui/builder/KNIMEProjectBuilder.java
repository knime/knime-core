/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   11.03.2005 (georg): created
 */
package de.unikn.knime.workbench.ui.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.workbench.ui.KNIMEUIPlugin;

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
     * @see org.eclipse.core.internal.events.InternalBuilder #build(int,
     *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected IProject[] build(final int kind, final Map args,
            final IProgressMonitor monitor) throws CoreException {

        LOGGER.debug("KNIME project builder invoked...");

        return new IProject[0];
    }

}

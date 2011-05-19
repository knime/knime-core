/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: May 18, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.navigator.actions;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.TreeViewer;


/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class CopyAction extends MoveWorkflowAction {

    public static final String ID = "org.knime.workbench.CopyAction";

    /**
     * @param source
     * @param target
     */
    public CopyAction(final IPath source, final IPath target) {
        super(source, target);
        setId(ID);
        setActionDefinitionId("Copy...");
    }


    /**
     * @param viewer
     */
    public CopyAction(final TreeViewer viewer) {
        super(viewer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void moveFiles(final File source, final File target) {


    }

}

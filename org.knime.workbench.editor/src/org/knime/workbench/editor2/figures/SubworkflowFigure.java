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
 *   09.06.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.swt.graphics.Image;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.workbench.editor2.ImageRepository;


/**
 * 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SubworkflowFigure extends NodeContainerFigure {
    
    // load images
    private static final Image IDLE_STATE = ImageRepository.getImage(
            "icons/meta/meta_idle2.png");
    
    private static final Image EXECUTING_STATE = ImageRepository.getImage(
            "icons/meta/meta_executing5.png");
    
    private static final Image EXECUTED_STATE = ImageRepository.getImage(
            "icons/meta/meta_executed.png");
    
    
    private static final Image BACKGROUND_ICON = ImageRepository.getImage(
            "icons/meta/meta_node.png");
    
    /**
     * Everything like the {@link NodeContainerFigure} but without the status
     * traffic light, state is reflected by icons on the node.
     * 
     * @param progress progress figure for super contructor
     */
    public SubworkflowFigure(final ProgressFigure progress) {
        super(progress);
        remove(getStatusFigure());
        ((NodeContainerFigure.ContentFigure)
                getContentFigure()).setBackgroundIcon(BACKGROUND_ICON);
    }
    
    /**
     * 
     * {@inheritDoc}
     * 
     * Only reflects three different states: idle, executing, executed.
     */
    @Override
    public void setState(final State state) {
        switch (state) {
        case IDLE:
        case CONFIGURED:
            ((NodeContainerFigure.ContentFigure)getContentFigure()).setIcon(
                    IDLE_STATE);
            break;
        case MARKEDFOREXEC:
        case UNCONFIGURED_MARKEDFOREXEC:
        case QUEUED:
        case EXECUTING:
        case EXECUTINGREMOTELY:
            ((NodeContainerFigure.ContentFigure)getContentFigure()).setIcon(
                    EXECUTING_STATE);
            break;
        case EXECUTED:
            ((NodeContainerFigure.ContentFigure)getContentFigure()).setIcon(
                    EXECUTED_STATE);
        }
        revalidate();
    }
    
    /**
     * {@inheritDoc}
     * Ignores it - since the type is fix.
     */
    @Override
    public void setType(final NodeType type) {
        // do nothing
    }

}

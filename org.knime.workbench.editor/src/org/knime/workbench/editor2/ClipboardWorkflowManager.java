/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.Collection;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.actions.CopyAction;
import org.knime.workbench.editor2.actions.CutAction;
import org.knime.workbench.editor2.actions.PasteAction;
import org.knime.workbench.editor2.actions.PasteActionContextMenu;

/**
 * A {@link WorkflowManager} instance that acts as a clipboard for copy, cut, 
 * and paste actions. The copied or cut nodes are inserted into the workflow 
 * manager clipboard and retrieved by the paste action. Only one set of nodes 
 * can be in the clipboard at the time. In addition a retrieval counter is 
 * provided, which allows counting how often the nodes were accessed. This 
 * information is used in the {@link PasteAction}.
 * 
 *   @see PasteAction
 *   @see PasteActionContextMenu
 *   @see CopyAction
 *   @see CutAction
 *   
 * 
 * @author Fabian Dill, University of Konstanz
 */
public final class ClipboardWorkflowManager {
    
    private ClipboardWorkflowManager() {
        // Utility class -> use static methods only 
    }
    
    private static final WorkflowManager CLIP_BOARD; 
        
    private static final String NAME = "KNIME Clipboard";
    
    private static int counter = 0;
    
    
    static {
        CLIP_BOARD = WorkflowManager.ROOT.createAndAddProject(NAME);
    }
    
    /**
     * 
     * @param source the {@link WorkflowManager} from which the nodes are 
     * copied
     * @param ids the ids of the nodes that should be copied to the clipboard
     */
    public static void put(final WorkflowManager source, final NodeID... ids) {
        clear();
        CLIP_BOARD.copy(source, ids);
    }
    
    /**
     * 
     * @return the current content of the clipboard
     *  
     * @see WorkflowManager#getNodeContainers()
     */
    public static Collection<NodeContainer> get() {
        return CLIP_BOARD.getNodeContainers();
    }
    
    /**
     * 
     * @return the clipboard {@link WorkflowManager}, necessary for the copy 
     * operation into a target workflow manager
     */
    public static WorkflowManager getSourceWorkflowManager() {
        return CLIP_BOARD;
    }
    
    /**
     * 
     * @return the number of retrievals of the current clipboard content
     */
    public static int getRetrievalCounter() {
        return counter;
    }
    
    /**
     * Resets the retrieval counter to zero. 
     */
    public static void resetRetrievalCounter() {
        counter = 0;
    }
    
    /**
     * Increments the retrieval counter.
     */
    public static void incrementRetrievalCounter() {
        counter++;
    }
    
    private static void clear() {
        counter = 0;
        // first copy all nodes...
        Collection<NodeContainer>containers = new ArrayList<NodeContainer>();
        containers.addAll(get());
        // ...in order to avoid ConcurrentModificationException
        for (NodeContainer cont : containers) {
            CLIP_BOARD.removeNode(cont.getID());
        }
    }

}

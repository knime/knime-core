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
 *   16.03.2005 (georg): created
 */
package de.unikn.knime.workbench.repository.model;

import java.util.Comparator;

/**
 * Default Comparator for the repository objects. This sorts alphabetically, and
 * categories before nodes. Additionally, the ordering for categories is
 * determined by the "after" field.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class DefaultRepositoryComparator implements Comparator {

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(final Object arg0, final Object arg1) {
        if (((arg0 == arg1) || (arg0.equals(arg1)))) {
            return 0;
        }

        //
        // 1. First arg is a Category
        //
        if (arg0 instanceof Category) {
            Category cat0 = (Category) arg0;
            //
            // 1.1 Second arg is category
            //
            // compare the level-id vs. the "after" field
            if (arg1 instanceof Category) {
                Category cat1 = (Category) arg1;
                // is there an ordering based on the "after" field ?
                String id0 = "" + cat0.getID();
                String id1 = "" + cat1.getID();
                String after0 = "" + cat0.getAfterID();
                String after1 = "" + cat1.getAfterID();

                // cat1 should be after cat0 ?
                if (after1.equals(id0)) {
                    return -1;
                } else
                // cat0 should be after cat1 ?
                if (after0.equals(id1)) {
                    return +1;
                }

                // else: sort alphabetically
                return ("" + cat0.getName()).compareTo(cat1.getName() + "");
            } else if (arg1 instanceof NodeTemplate) {
                //
                // 1.2 Second arg is node
                //
                // Nodes are always sorted behind categories
                return -1;
            }
        } else
        //
        // 2. First arg is a NodeTemplate
        //
        if (arg0 instanceof NodeTemplate) {

            NodeTemplate node0 = (NodeTemplate) arg0;
            //
            // 2.1 Second arg is node
            //
            // compare alphabetically by name
            if (arg1 instanceof NodeTemplate) {
                NodeTemplate node1 = (NodeTemplate) arg1;
                return ("" + node0.getName()).compareTo(node1.getName() + "");
            } else if (arg1 instanceof Category) {
                //
                // 2.2 Second arg is category
                //
                // Nodes are always sorted behind categories
                return +1;
            }
        }

        return 0;
    }
}

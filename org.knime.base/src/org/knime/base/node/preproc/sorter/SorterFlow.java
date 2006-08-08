/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
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
 * -------------------------------------------------------------------
 * 
 * History
 *   02.02.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.base.node.viz.table.TableNodeFactory;
import org.knime.core.node.Node;

/**
 * Example workflow for the Sorter Node.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public final class SorterFlow {

    private SorterFlow() {
        final Node fileNode = new Node(new FileReaderNodeFactory());
        final Node sorterNode = new Node(new SorterNodeFactory());
        final Node viewnode = new Node(new TableNodeFactory());
        final Node viewnode2 = new Node(new TableNodeFactory());

        sorterNode.getInPort(0).connectPort(fileNode.getOutPort(0));
        viewnode.getInPort(0).connectPort(fileNode.getOutPort(0));
        viewnode2.getInPort(0).connectPort(sorterNode.getOutPort(0));

        fileNode.showDialog();
        fileNode.execute();
        viewnode.execute();
        viewnode.showView(0);
        sorterNode.showDialog();
        sorterNode.showDialog();
        sorterNode.execute();
        viewnode2.execute();
        viewnode2.showView(0);
    }

    /**
     * Main function called from command line.
     * 
     * @param args not in use
     */
    public static void main(final String[] args) {
        new SorterFlow();
    }
}

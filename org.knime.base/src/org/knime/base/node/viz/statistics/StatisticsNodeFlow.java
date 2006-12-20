/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   18.04.2005 (cebron): created
 */
package org.knime.base.node.viz.statistics;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.core.node.Node;

/**
 * Test Flow for the Statistics Node.
 * @author cebron, University of Konstanz
 */
public final class StatisticsNodeFlow {

    /**
     * Flow : Read File - compute statistics - show view.
     */
    private StatisticsNodeFlow() {
        final Node fileNode = new Node(new FileReaderNodeFactory());
        final Node statNode = new Node(new StatisticsNodeFactory());    
        statNode.getInPort(0).connectPort(fileNode.getOutPort(0));
        
        fileNode.showDialog();
        fileNode.execute();
        statNode.execute();
        statNode.showView(0);
    }
    
    /**
     * Main function called from command line.
     * 
     * @param args Not in use.
     */
    public static void main(final String[] args) {
          new StatisticsNodeFlow();
    }
}

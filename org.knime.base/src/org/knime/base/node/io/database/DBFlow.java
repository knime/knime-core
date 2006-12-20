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
 *   16.11.2005 (gabriel): created
 */
package org.knime.base.node.io.database;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.base.node.viz.table.TableNodeFactory;
import org.knime.core.node.Node;

/**
 * Test the Database Access Node.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DBFlow {
    
    private DBFlow() {
        read();
    }
    
    /**
     * Reads from database.
     *
     */
    void read() {
        Node read = new Node(new DBReaderNodeFactory());
        read.showDialog();
        read.execute();

        Node table = new Node(new TableNodeFactory());
        table.getInPort(0).connectPort(read.getOutPort(0));
        table.execute();
        table.showView(0);
    }

    
    /**
     * Writes to database.
     *
     */
    void write() {
    
        Node file = new Node(new FileReaderNodeFactory());
        file.showDialog();
        file.execute();
        
        Node table = new Node(new TableNodeFactory());
        table.getInPort(0).connectPort(file.getOutPort(0));
        table.execute();
        table.showView(0);
        

        Node writer = new Node(new DBWriterNodeFactory());
        writer.getInPort(0).connectPort(file.getOutPort(0));
        writer.showDialog();
        writer.execute();
    }
    
    

    /**
     * @param args command line parameters
     */
    public static void main(final String[] args) {
        new DBFlow();
    }
}

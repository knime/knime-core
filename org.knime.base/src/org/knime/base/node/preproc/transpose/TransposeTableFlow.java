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
 */
package org.knime.base.node.preproc.transpose;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.base.node.util.cache.CacheNodeFactory;
import org.knime.base.node.viz.table.TableNodeFactory;
import org.knime.core.node.Node;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class TransposeTableFlow {
    /** Hides puplic constructor. */
    private TransposeTableFlow() {

    }

    /**
     * @param args don't need arguments.
     */
    public static void main(final String[] args) {

        // create: file reader node
        String file = "../dataset/iris/data.all.xml";
        Node fileReaderNode = new Node(new FileReaderNodeFactory(file));
        fileReaderNode.showDialog();
        fileReaderNode.execute();

        // create: cache node
        Node cache = new Node(new CacheNodeFactory());
        cache.getInPort(0).connectPort(fileReaderNode.getOutPort(0));
        cache.execute();

        // create: table view node
        Node table = new Node(new TableNodeFactory());
        table.getInPort(0).connectPort(cache.getOutPort(0));
        table.showView(0);
        table.execute();

        // create: transpose table
        Node transposeNode = new Node(new TransposeTableNodeFactory());
        transposeNode.getInPort(0).connectPort(cache.getOutPort(0));
        transposeNode.execute();

        // create: table view node
        Node tableNode = new Node(new TableNodeFactory());
        tableNode.getInPort(0).connectPort(transposeNode.getOutPort(0));
        tableNode.showView(0);
        tableNode.execute();

        // create: csv writer node
        // Node csv = new Node(new CSVWriterNodeFactory());
        // csv.getNodeInPort(0).connectPort(transposeNode.getNodeOutPort(0));
        // csv.showDialog();
        // csv.executeNode();

    }
}

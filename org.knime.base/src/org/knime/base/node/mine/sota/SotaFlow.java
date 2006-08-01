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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.base.node.viz.property.color.ColorManagerNodeFactory;
import org.knime.base.node.viz.table.TableNodeFactory;
import org.knime.core.node.Node;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class SotaFlow {
    private SotaFlow() {
    }

    /**
     * Test the SotaTreeCell.
     * 
     * @param args not needed
     */
    public static void main(final String[] args) {

        // create: file reader node
        String file = "../dataset/iris/data.all.xml";
        // String file = "../dataset/artificial/data.all.xml";
        // String file = "../dataset/iris/data.tst.xml";
        // String file = "../dataset/wine/wine.xml";

        // String file = "../dataset/diabetes/data.all.xml";
        // String file = "../dataset/satimage/data.tst.xml";
        // String file = "../dataset/segment/data.trn.xml";

        // String file = "../dataset/shuttle/data.tst.xml";
        // String file = "../dataset/australian/data.all.xml";

        Node fileReaderNode = new Node(new FileReaderNodeFactory(file));
        fileReaderNode.showDialog();
        fileReaderNode.execute();

        Node colorManager = new Node(new ColorManagerNodeFactory());
        colorManager.getInPort(0).connectPort(fileReaderNode.getOutPort(0));
        colorManager.showDialog();
        colorManager.execute();

        // create: normalizer node
        /*
         * Node normNode = new Node(new NormalizeNodeFactory());
         * normNode.getNodeInPort(0).connectPort(colorManager.getNodeOutPort(0));
         * normNode.showDialog(); normNode.executeNode(); //
         */

        /*
         * //Node fuzzy = new Node(new FuzzyBasisFunctionLearnerNodeFactory());
         * Node fuzzy = new Node( new
         * FuzzyBasisFunctionHierarchyLearnerNodeFactory());
         * fuzzy.getNodeInPort(0).connectPort(colorManager.getNodeOutPort(0));
         * fuzzy.showDialog(); fuzzy.executeNode(); //
         */

        // create: table view node
        // /*
        Node tableNode = new Node(new TableNodeFactory());
        tableNode.getInPort(0).connectPort(colorManager.getOutPort(0));
        tableNode.showView(0);
        tableNode.execute();
        // */

        // create: sota view node for file reader
        Node sotaNode = new Node(new SotaNodeFactory());
        sotaNode.getInPort(0).connectPort(colorManager.getOutPort(0));
        sotaNode.showDialog();
        sotaNode.showView(0);
        sotaNode.execute();

        // create: mds view node for file reader
        // Node mdsNode = new Node(new MDSNodeFactory());
        // mdsNode.getNodeInPort(0).connectPort(
        // colorManager.getNodeOutPort(0));
        // mdsNode.showDialog();
        // mdsNode.showView(0);
        // mdsNode.executeNode();

        // create: paarcoord
        // Node parcoord = new Node(new ParallelCoordinatesNodeFactory());
        // parcoord.getNodeInPort(0).connectPort(
        // colorManager.getNodeOutPort(0));
        // parcoord.showDialog();
        // parcoord.executeNode();
        // parcoord.showView(0);
    }
}

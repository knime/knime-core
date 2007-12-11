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
 */
package org.knime.base.node.viz.parcoord;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.base.node.mine.bfn.fuzzy.FuzzyBasisFunctionLearnerNodeFactory;
import org.knime.base.node.viz.property.color.ColorManager2NodeFactory;
import org.knime.base.node.viz.table.TableNodeFactory;
import org.knime.core.node.Node;

/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public final class ParallelCoordinates {
    
    /*
     * 
     */
    private ParallelCoordinates() {
        
        // create: file reader
        Node fileNode = new Node(new FileReaderNodeFactory(
                "../dataset/iris/data.all.xml"));        
        //fileNode.showDialog();
        fileNode.execute();    
        
        // create: color manager node
        Node colorNode = new Node(new ColorManager2NodeFactory());
        colorNode.getInPort(0).connectPort(fileNode.getOutPort(0));
        colorNode.showDialog();
        colorNode.execute();
        
        // create: table view node for file reader
        Node tableNode = new Node(new TableNodeFactory());
        tableNode.getInPort(0).connectPort(colorNode.getOutPort(0));
        tableNode.execute();
        tableNode.showView(0);
        
        // create: parallel coordinates view on the data
        Node parallelCoordinatesNode = new Node(
                new ParallelCoordinatesNodeFactory());
        parallelCoordinatesNode.getInPort(0).connectPort(
                colorNode.getOutPort(0));
        parallelCoordinatesNode.execute();
        parallelCoordinatesNode.showView(0);
        
        // create: fuzzy basisfunction learner
        Node bfNode = new Node(new FuzzyBasisFunctionLearnerNodeFactory());
        bfNode.getInPort(0).connectPort(colorNode.getOutPort(0));
        bfNode.showDialog();
        bfNode.execute();
        
        // create: table view node for file reader
        Node tableNodeBF = new Node(new TableNodeFactory());
        tableNodeBF.getInPort(0).connectPort(bfNode.getOutPort(0));
        tableNodeBF.execute();
        tableNodeBF.showView(0);
        
        // create: parallel coordinates view on the basisfunctions
        Node parallelCoordinatesNodeBF = new Node(
                new ParallelCoordinatesNodeFactory());
        parallelCoordinatesNodeBF.getInPort(0).connectPort(
                bfNode.getOutPort(0));
        parallelCoordinatesNodeBF.execute();
        parallelCoordinatesNodeBF.showView(0);
        
    }
    /**
     * Parallel Coordinates.
     * @param args arguments given to the main function
     */
    public static void main(final String[] args) {
        new ParallelCoordinates();
    }
}

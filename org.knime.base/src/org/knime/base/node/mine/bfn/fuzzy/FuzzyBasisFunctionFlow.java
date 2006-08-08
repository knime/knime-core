/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Test flow for a fuzzy basisfunction network using the learner, predictor
 * node. In addition, we show up some auxiliary nodes to view the data.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class FuzzyBasisFunctionFlow {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FuzzyBasisFunctionFlow.class);

    /**
     * Starts the fuzzy bf learning and predicting flow.
     * 
     * @param trn The trainings data file.
     * @param tst The test data file.
     */
    private FuzzyBasisFunctionFlow(final String trn, final String tst) {
        LOGGER.info("Train data: " + trn);
        LOGGER.info("Test data: " + tst);

        // keeps all possible nodes
        WorkflowManager flow = new WorkflowManager();

        //
        // create nodes
        //

        // create: file reader node for trainings data
        int fileReaderTrain = flow.addNewNode(new FileReaderNodeFactory(trn))
                .getID();
        // create: fuzzy learner node
        int fuzzyLearner = flow.addNewNode(
                new FuzzyBasisFunctionLearnerNodeFactory()).getID();

        //
        // connect nodes
        //

        flow.addConnection(fileReaderTrain, 0, fuzzyLearner, 0);

        //
        // show dialogs
        //

        flow.getNodeContainerById(fileReaderTrain).showDialog();
        flow.executeUpToNode(fileReaderTrain, true);
        flow.getNodeContainerById(fuzzyLearner).showDialog();
        flow.executeUpToNode(fuzzyLearner, true);

        flow.getNodeContainerById(fuzzyLearner).showView(0);

    }

    /**
     * Main function called from command line.
     * 
     * @param args not used
     */
    public static final void main(final String[] args) {
        // get trainings and test data file name from cmd
        final String trn = (args.length > 0 ? args[0] : null);
        final String tst = (args.length > 1 ? args[1] : trn);
        try {
            new FuzzyBasisFunctionFlow(trn, tst);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

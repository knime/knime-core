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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.radial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;
import org.knime.base.node.mine.scorer.ScorerNodeFactory;
import org.knime.base.node.preproc.binner.BinnerNodeFactory;
import org.knime.base.node.preproc.filter.column.FilterColumnNodeFactory;
import org.knime.base.node.util.cache.CacheNodeFactory;
import org.knime.base.node.viz.parcoord.ParallelCoordinatesNodeFactory;
import org.knime.base.node.viz.property.color.ColorManagerNodeFactory;
import org.knime.base.node.viz.table.TableNodeFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.WorkflowInExecutionException;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Test flow for a radial basisfunction network using the learner, predictor
 * node. In addition, we show up some auxiliary nodes to view the data.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class RadialBasisFunctionFlow {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RadialBasisFunctionFlow.class);

    /**
     * Starts the radial bf learning and predicting flow.
     * 
     * @param trn the training data file
     * @param tst the test data file
     * 
     */
    private RadialBasisFunctionFlow(final String trn, final String tst)
            throws IOException, CanceledExecutionException,
            WorkflowInExecutionException, InvalidSettingsException {

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
        // create: binner
        int binner = flow.addNewNode(new BinnerNodeFactory()).getID();
        // create: color manager
        int color = flow.addNewNode(new ColorManagerNodeFactory()).getID();
        // create: column filter
        int column = flow.addNewNode(new FilterColumnNodeFactory()).getID();
        // create: cache node trainings data
        int cache1 = flow.addNewNode(new CacheNodeFactory()).getID();
        // create: table view trainings data
        int table1 = flow.addNewNode(new TableNodeFactory()).getID();
        // create: radial bf learner node
        int rbfLearner = flow.addNewNode(
                new RadialBasisFunctionLearnerNodeFactory()).getID();
        // create: parallel coord
        int parcoord = flow.addNewNode(new ParallelCoordinatesNodeFactory())
                .getID();
        // create: table view fuzzy model
        int table2 = flow.addNewNode(new TableNodeFactory()).getID();
        // create: file reader node for test data
        int fileReaderTest = flow.addNewNode(new FileReaderNodeFactory(tst))
                .getID();
        // create: cache node trainings data
        int cache2 = flow.addNewNode(new CacheNodeFactory()).getID();
        // create: table view test data
        int table3 = flow.addNewNode(new TableNodeFactory()).getID();
        // create: rbf predictor node
        int rbfPredictor = flow.addNewNode(
                new RadialBasisFunctionPredictorNodeFactory()).getID();
        // create: table view predicted test data
        int table4 = flow.addNewNode(new TableNodeFactory()).getID();
        // create: scorer node
        int scorer = flow.addNewNode(new ScorerNodeFactory()).getID();

        //
        // connect nodes
        //

        flow.addConnection(fileReaderTrain, 0, cache1, 0);
        flow.addConnection(cache1, 0, binner, 0);
        flow.addConnection(binner, 0, column, 0);
        flow.addConnection(column, 0, color, 0);
        flow.addConnection(color, 0, table1, 0);
        flow.addConnection(color, 0, rbfLearner, 0);
        flow.addConnection(rbfLearner, 0, table2, 0);
        flow.addConnection(rbfLearner, 0, parcoord, 0);
        flow.addConnection(fileReaderTest, 0, cache2, 0);
        flow.addConnection(cache2, 0, table3, 0);
        flow.addConnection(cache2, 0, rbfPredictor, 0);
        flow.addConnection(rbfLearner, 0, rbfPredictor, 1);
        flow.addConnection(rbfPredictor, 0, table4, 0);
        flow.addConnection(rbfPredictor, 0, scorer, 0);

        //
        // show dialogs
        //

        flow.getNodeContainerById(fileReaderTrain).showDialog();
        flow.getNodeContainerById(fileReaderTrain).startExecution(null);
        flow.getNodeContainerById(binner).showDialog();
        flow.getNodeContainerById(column).showDialog();
        flow.getNodeContainerById(color).showDialog();
        flow.getNodeContainerById(rbfLearner).showDialog();
        flow.getNodeContainerById(fileReaderTest).showDialog();
        flow.getNodeContainerById(rbfPredictor).showDialog();
        flow.getNodeContainerById(scorer).showDialog();

        // 
        // save and load flow in Config
        //         

        NodeSettingsRO settings = new NodeSettings("pnn_flow");
        File file = File.createTempFile(WorkflowManager.WORKFLOW_FILE, "");
        flow.save(file, new DefaultNodeProgressMonitor());
        FileOutputStream fos = new FileOutputStream(file);
        settings.saveToXML(fos);
        settings = NodeSettings.loadFromXML(new FileInputStream(file));
        flow = new WorkflowManager(file, new DefaultNodeProgressMonitor());

        // 
        // execute all sinks
        //

        flow.executeAll(true);

        //
        // show all views
        //

        flow.getNodeContainerById(scorer).showView(0);
        flow.getNodeContainerById(table1).showView(0);
        flow.getNodeContainerById(table2).showView(0);
        flow.getNodeContainerById(table3).showView(0);
        flow.getNodeContainerById(table4).showView(0);
        flow.getNodeContainerById(parcoord).showView(0);

    } // RadialBasisFunctionFlow(String,String)

    /**
     * Main function called from command line.
     * 
     * @param args Not used.
     */
    public static final void main(final String[] args) {
        // get trainings and test data file name from cmd
        final String trn = (args.length > 0 ? args[0] : null);
        final String tst = (args.length > 1 ? args[1] : trn);
        try {
            new RadialBasisFunctionFlow(trn, tst);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

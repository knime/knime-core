/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.core.node.port.pmml;

/**
 * Enum to describe valid PMML models (and extensions) as of version 2.1.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public enum PMMLModelType {
        /** PMML TreeModel. */
        TreeModel,
        /** PMML NeuralNetwork. */
        NeuralNetwork,
        /** PMML ClusteringModel. */
        ClusteringModel,
        /** PMML RegressionModel. */
        RegressionModel,
        /** PMML GeneralRegressionModel. */
        GeneralRegressionModel,
        /** PMML NaiveBayes. */
        NaiveBayesModel,
        /** PMML AssociationModel.*/
        AssociationModel,
        /** PMML SequenceModel. */
        SequenceModel,
        /** 
         * PMML Extension fur own model implementation if PMML standard 
         * models don't fit. 
         */
        Extension,
        /** Neither a vaild PMML model nor an extension was found. */
        None
}

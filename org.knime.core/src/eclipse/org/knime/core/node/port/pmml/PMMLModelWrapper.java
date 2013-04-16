/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 */
package org.knime.core.node.port.pmml;

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.ClusteringModelDocument.ClusteringModel;
import org.dmg.pmml.DataDictionaryDocument.DataDictionary;
import org.dmg.pmml.GeneralRegressionModelDocument.GeneralRegressionModel;
import org.dmg.pmml.MiningModelDocument.MiningModel;
import org.dmg.pmml.NaiveBayesModelDocument.NaiveBayesModel;
import org.dmg.pmml.NeuralNetworkDocument.NeuralNetwork;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.RegressionModelDocument.RegressionModel;
import org.dmg.pmml.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml.SegmentDocument.Segment;
import org.dmg.pmml.SupportVectorMachineModelDocument.SupportVectorMachineModel;
import org.dmg.pmml.TargetDocument.Target;
import org.dmg.pmml.TargetsDocument.Targets;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.knime.core.pmml.PMMLFormatter;
import org.knime.core.pmml.PMMLModelType;

/**
 *
 * @author Alexander Fillbrunn, Universitaet Konstanz
 * @since 2.8
 */
public abstract class PMMLModelWrapper {

    /**
     * Returns the model type for this wrapper.
     * @return The model type
     */
    public abstract PMMLModelType getModelType();

    /**
     * Returns all target columns of the model.
     * @return A list of target columns
     */
    public abstract List<String> getTargetCols();

    /**
     * Gets the "MiningSchema" element.
     * @return The mining schema
     */
    public abstract org.dmg.pmml.MiningSchemaDocument.MiningSchema getMiningSchema();

    /**
     * Sets the "MiningSchema" element.
     * @param miningSchema The mining schema
     */
    public abstract void setMiningSchema(org.dmg.pmml.MiningSchemaDocument.MiningSchema miningSchema);

    /**
     * Gets the "functionName" attribute.
     * @return The function name
     */
    public abstract org.dmg.pmml.MININGFUNCTION.Enum getFunctionName();

    /**
     * Sets the "functionName" attribute.
     * @param functionName The function name
     */
    public abstract void setFunctionName(org.dmg.pmml.MININGFUNCTION.Enum functionName);

    /**
     * Gets the "algorithmName" attribute.
     * @return The algorithm name
     */
    public abstract java.lang.String getAlgorithmName();

    /**
     * True if has "algorithmName" attribute.
     * @return The algorithm name
     */
    public abstract boolean isSetAlgorithmName();

    /**
     * Sets the "algorithmName" attribute.
     * @param algorithmName The name of the algorithm
     */
    public abstract void setAlgorithmName(java.lang.String algorithmName);

    /**
     * Adds the model to a segment in a pmml segmentation.
     * @param s The segment
     */
    public abstract void addToSegment(final Segment s);

    /**
     * Creates a pmml document with the model as content.
     * @param dataDict the data dictionary.
     * @return The PMML document
     */
    public abstract PMMLDocument createPMMLDocument(DataDictionary dataDict);

    /**
     * Returns a list of all models that are in segments of the given MiningModel.
     * @param model The MiningModel
     * @return The list of models
     */
    public static List<PMMLModelWrapper> getModelListFromMiningModel(final MiningModel model) {
        ArrayList<PMMLModelWrapper> list = new ArrayList<PMMLModelWrapper>();
        for (Segment s : model.getSegmentation().getSegmentList()) {
            if (s.getTreeModel() != null) {
                list.add(new PMMLTreeModelWrapper(s.getTreeModel()));
            }
            if (s.getClusteringModel() != null) {
                list.add(new PMMLClusteringModelWrapper(s.getClusteringModel()));
            }
            if (s.getGeneralRegressionModel() != null) {
                list.add(new PMMLGeneralRegressionModelWrapper(s.getGeneralRegressionModel()));
            }
            if (s.getRegressionModel() != null) {
                list.add(new PMMLRegressionModelWrapper(s.getRegressionModel()));
            }
            if (s.getNaiveBayesModel() != null) {
                list.add(new PMMLNaiveBayesModelWrapper(s.getNaiveBayesModel()));
            }
            if (s.getNeuralNetwork() != null) {
                list.add(new PMMLNeuralNetworkWrapper(s.getNeuralNetwork()));
            }
            if (s.getRuleSetModel() != null) {
                list.add(new PMMLRuleSetModelWrapper(s.getRuleSetModel()));
            }
            if (s.getSupportVectorMachineModel() != null) {
                list.add(new PMMLSupportVectorMachineModelWrapper(s.getSupportVectorMachineModel()));
            }
        }
        return list;
    }

    /**
     * Returns a list of model wrappers for all models in the PMML document.
     * @param pmmldoc The pmml document
     * @return The list of wrapped models
     */
    public static List<PMMLModelWrapper> getModelListFromPMMLDocument(final PMMLDocument pmmldoc) {
        return getModelListFromPMML(pmmldoc.getPMML());
    }

    /**
     * Returns a list of model wrappers for all models in the PMML.
     * @param pmml The pmml
     * @return The list of wrapped models
     */
    public static List<PMMLModelWrapper> getModelListFromPMML(final PMML pmml) {
        ArrayList<PMMLModelWrapper> list = new ArrayList<PMMLModelWrapper>();
        for (TreeModel m : pmml.getTreeModelList()) {
            list.add(new PMMLTreeModelWrapper(m));
        }
        for (RegressionModel m : pmml.getRegressionModelList()) {
            list.add(new PMMLRegressionModelWrapper(m));
        }
        for (GeneralRegressionModel m : pmml.getGeneralRegressionModelList()) {
            list.add(new PMMLGeneralRegressionModelWrapper(m));
        }
        for (ClusteringModel m : pmml.getClusteringModelList()) {
            list.add(new PMMLClusteringModelWrapper(m));
        }
        for (NaiveBayesModel m : pmml.getNaiveBayesModelList()) {
            list.add(new PMMLNaiveBayesModelWrapper(m));
        }
        for (NeuralNetwork m : pmml.getNeuralNetworkList()) {
            list.add(new PMMLNeuralNetworkWrapper(m));
        }
        for (RuleSetModel m : pmml.getRuleSetModelList()) {
            list.add(new PMMLRuleSetModelWrapper(m));
        }
        for (SupportVectorMachineModel m : pmml.getSupportVectorMachineModelList()) {
            list.add(new PMMLSupportVectorMachineModelWrapper(m));
        }
        return list;
    }

    /**
     * Returns the content of a segment as a model wrapper.
     * @param s The segment
     * @return Returns a wrapper around the model
     */
    public static PMMLModelWrapper getSegmentContent(final Segment s) {
        TreeModel treemodel = s.getTreeModel();
        if (treemodel != null) {
            return new PMMLTreeModelWrapper(treemodel);
        }
        RegressionModel regrmodel = s.getRegressionModel();
        if (regrmodel != null) {
            return new PMMLRegressionModelWrapper(regrmodel);
        }
        GeneralRegressionModel genregrmodel = s.getGeneralRegressionModel();
        if (genregrmodel != null) {
           return new PMMLGeneralRegressionModelWrapper(genregrmodel);
        }
        ClusteringModel clustmodel = s.getClusteringModel();
        if (clustmodel != null) {
            return new PMMLClusteringModelWrapper(clustmodel);
        }
        NaiveBayesModel nbmodel = s.getNaiveBayesModel();
        if (nbmodel != null) {
            return new PMMLNaiveBayesModelWrapper(nbmodel);
        }
        NeuralNetwork nn = s.getNeuralNetwork();
        if (nn != null) {
            return new PMMLNeuralNetworkWrapper(nn);
        }
        RuleSetModel rsmodel = s.getRuleSetModel();
        if (rsmodel != null) {
            return new PMMLRuleSetModelWrapper(rsmodel);
        }
        SupportVectorMachineModel svmmodel = s.getSupportVectorMachineModel();
        if (svmmodel != null) {
            return new PMMLSupportVectorMachineModelWrapper(svmmodel);
        }
        return null;
    }

    /**
     * creates an empty document.
     *
     * @param dataDict the data dictionary.
     * @return a empty pmml document
     */
    protected static PMMLDocument createEmptyDocument(final DataDictionary dataDict) {
        PMMLDocument pmmlDoc = PMMLDocument.Factory.newInstance(
                                 PMMLFormatter.getOptions());
        PMML pmml = pmmlDoc.addNewPMML();
        pmml.setVersion(PMMLPortObject.PMML_V4_1);
        if (dataDict == null) {
            pmml.addNewDataDictionary();
        } else {
            pmml.setDataDictionary(dataDict);
        }
        PMMLPortObjectSpec.writeHeader(pmml);
        return pmmlDoc;
    }

    /**
     * Extracts target columns as strings from the model.
     * @param targets The targets from the xml file
     * @return A list of target column names
     */
    protected List<String> getTargetCols(final Targets targets) {
        ArrayList<String> result = new ArrayList<String>();
        if (targets != null) {
            for (Target t : targets.getTargetList()) {
                result.add(t.getField());
            }
        }
        return result;
    }
}

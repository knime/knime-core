/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ------------------------------------------------------------------------
  *
  * History
  *   May 18, 2011 (morent): created
  */

package org.knime.core.node.port.pmml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import org.dmg.pmml40.AssociationModelDocument.AssociationModel;
import org.dmg.pmml40.ClusteringModelDocument.ClusteringModel;
import org.dmg.pmml40.FIELDUSAGETYPE;
import org.dmg.pmml40.GeneralRegressionModelDocument.GeneralRegressionModel;
import org.dmg.pmml40.INVALIDVALUETREATMENTMETHOD;
import org.dmg.pmml40.INVALIDVALUETREATMENTMETHOD.Enum;
import org.dmg.pmml40.MiningFieldDocument.MiningField;
import org.dmg.pmml40.MiningModelDocument.MiningModel;
import org.dmg.pmml40.MiningSchemaDocument.MiningSchema;
import org.dmg.pmml40.NaiveBayesModelDocument.NaiveBayesModel;
import org.dmg.pmml40.NeuralNetworkDocument.NeuralNetwork;
import org.dmg.pmml40.PMMLDocument;
import org.dmg.pmml40.RegressionModelDocument.RegressionModel;
import org.dmg.pmml40.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml40.SequenceModelDocument.SequenceModel;
import org.dmg.pmml40.SupportVectorMachineModelDocument.SupportVectorMachineModel;
import org.dmg.pmml40.TextModelDocument.TextModel;
import org.dmg.pmml40.TimeSeriesModelDocument.TimeSeriesModel;
import org.dmg.pmml40.TreeModelDocument.TreeModel;
import org.knime.core.node.NodeLogger;
import org.knime.core.pmml.PMMLModelType;
import org.knime.core.pmml.PMMLUtils;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class PMMLMiningSchemaTranslator {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLMiningSchemaTranslator.class);
    private final List<String> m_learningFields;
    private final List<String> m_targetFields;

    /**
     * Creates an empty PMML mining schema translator.
     */
    public PMMLMiningSchemaTranslator() {
        m_learningFields = new ArrayList<String>();
        m_targetFields = new ArrayList<String>();
    }

    /**
     * Initializes the mining schema translator based on a PMML document.
     * See {@link PMMLTranslator#initializeFrom(PMMLDocument)}
     * @param pmmlDoc the PMML document
     */
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        Map<PMMLModelType, Integer> models
            = PMMLUtils.getNumberOfModels(pmmlDoc);
        if (models.isEmpty()) {
            LOGGER.warn("The PMML document contains no model. Hence no "
                    + "mining schema could be found.");
            return;
        }
        // retrieve the first models mining schema
        MiningSchema miningSchema = PMMLUtils.getFirstMiningSchema(pmmlDoc,
                models.keySet().iterator().next().getXmlBeansType());
        for (MiningField miningField : miningSchema.getMiningFieldArray()) {
            if (miningField.isSetMissingValueReplacement()) {
                LOGGER.warn("\"missingValueReplacement\" is not supported and "
                        + "will be ignored. Skipping it");
            }
            if (miningField.isSetMissingValueTreatment()) {
                LOGGER.warn("\"missingValueTreatment\" is not supported and "
                        + "will be ignored. Skipping it");
            }
            if (miningField.isSetOutliers()) {
                LOGGER.warn("\"outliers\" is not supported and "
                        + "will be ignored. Skipping it");
            }
            checkInvalidValueTreatment(pmmlDoc, miningField);

            String name = miningField.getName();
            FIELDUSAGETYPE.Enum usageType = miningField.getUsageType();
            if (FIELDUSAGETYPE.ACTIVE == usageType) {
                m_learningFields.add(name);
            } else if (FIELDUSAGETYPE.PREDICTED == usageType) {
                m_targetFields.add(name);
            }
        }
    }


    /**
     * @param pmmlDoc the PMML document to check
     * @param miningField the mining field to check
     */
    private void checkInvalidValueTreatment(final PMMLDocument pmmlDoc,
            final MiningField miningField) {
        boolean isOldKnimeSchema
                = PMMLPortObject.isKnimeProducedAndOlderThanVersion(
                pmmlDoc.getPMML(), PMMLPortObject.KNIME_V_2_3_3);
        Enum treatment = miningField.getInvalidValueTreatment();
        if (!((isOldKnimeSchema
                && treatment != null)
                || INVALIDVALUETREATMENTMETHOD.AS_IS == treatment)) {
            String treatmentText = treatment == null
                    ? "<default>" : treatment.toString();
            String msg = "MiningField \"" + miningField.getName()
                    + "\": Only \"asIs\" is supported for "
                    + "invalidValueTreatment. invalidValueTreatment=\""
                    + treatmentText +  "\" is treated as \"asIs\".";
            /* At this point the prediction does not
             * give the expected result for outliers (invalid values) from
             * a PMML point of view. But as this is very restrictive and
             * causes the RtoPMML functionality to fail and might be
             * unnecessary if there are no outliers. Hence only a warning
             * message is issued.
             * TODO: Extend the functionality of the PMML predictors to
             * support more invalid value treatment strategies. */
//              throw new RuntimeException(msg);
            LOGGER.warn(msg);
        }
    }


    /**
     * Writes the MiningSchema based upon the fields of the passed
     * {@link PMMLPortObjectSpec}.
     *
     * @param portSpec based upon this port object spec the mining schema is
     *            written
     * @param model the PMML model element to write the mining schema to
     */
    public static void writeMiningSchema(final PMMLPortObjectSpec portSpec,
            final XmlObject model) {
        MiningSchema miningSchema = MiningSchema.Factory.newInstance();

        // avoid duplicate entries
        Set<String> learningNames = new HashSet<String>(
                portSpec.getLearningFields());
        Set<String> targetNames = new HashSet<String>(
                portSpec.getTargetFields());

        for (String colName : portSpec.getLearningFields()) {
            if (!targetNames.contains(colName)) {
                MiningField miningField = miningSchema.addNewMiningField();
                miningField.setName(colName);
                miningField.setInvalidValueTreatment(
                        INVALIDVALUETREATMENTMETHOD.AS_IS);
                // don't write usageType = active (is default)
            }
        }

        // add all fields referenced in local transformations
        for (String colName : portSpec.getPreprocessingFields()) {
            if (!learningNames.contains(colName)
                    && !targetNames.contains(colName)) {
                MiningField miningField = miningSchema.addNewMiningField();
                miningField.setName(colName);
                miningField.setInvalidValueTreatment(
                        INVALIDVALUETREATMENTMETHOD.AS_IS);
                // don't write usageType = active (is default)
            }
        }

        // target columns = predicted
        for (String colName : portSpec.getTargetFields()) {
            MiningField miningField = miningSchema.addNewMiningField();
            miningField.setName(colName);
            miningField.setInvalidValueTreatment(
                    INVALIDVALUETREATMENTMETHOD.AS_IS);
            miningField.setUsageType(FIELDUSAGETYPE.PREDICTED);
        }

        /* Unfortunately the PMML models have no common base class. Therefore
         * a cast to the specific type is necessary for being able to add the
         * mining schema. */
        SchemaType type = model.schemaType();
        if (AssociationModel.type.equals(type)) {
            ((AssociationModel)model).setMiningSchema(miningSchema);
        } else if (ClusteringModel.type.equals(type)) {
            ((ClusteringModel)model).setMiningSchema(miningSchema);
        } else if (GeneralRegressionModel.type.equals(type)) {
            ((GeneralRegressionModel)model).setMiningSchema(miningSchema);
        } else if (MiningModel.type.equals(type)) {
            ((MiningModel)model).setMiningSchema(miningSchema);
        } else if (NaiveBayesModel.type.equals(type)) {
            ((NaiveBayesModel)model).setMiningSchema(miningSchema);
        } else if (NeuralNetwork.type.equals(type)) {
            ((NeuralNetwork)model).setMiningSchema(miningSchema);
        } else if (RegressionModel.type.equals(type)) {
            ((RegressionModel)model).setMiningSchema(miningSchema);
        } else if (RuleSetModel.type.equals(type)) {
            ((RuleSetModel)model).setMiningSchema(miningSchema);
        } else if (SequenceModel.type.equals(type)) {
            ((SequenceModel)model).setMiningSchema(miningSchema);
        } else if (SupportVectorMachineModel.type.equals(type)) {
            ((SupportVectorMachineModel)model).setMiningSchema(miningSchema);
        } else if (TextModel.type.equals(type)) {
            ((TextModel)model).setMiningSchema(miningSchema);
        } else if (TimeSeriesModel.type.equals(type)) {
            ((TimeSeriesModel)model).setMiningSchema(miningSchema);
        } else if (TreeModel.type.equals(type)) {
            ((TreeModel)model).setMiningSchema(miningSchema);
        }
    }


    /**
     * @return the learningFields
     */
    public List<String> getActiveFields() {
        return m_learningFields;
    }

    /**
     * @return the targetFields
     */
    public List<String> getTargetFields() {
        return m_targetFields;
    }
}

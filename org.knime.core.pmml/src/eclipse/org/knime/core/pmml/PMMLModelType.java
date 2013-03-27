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
 * ------------------------------------------------------------------------
 */
package org.knime.core.pmml;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.AssociationModelDocument;
import org.dmg.pmml.AssociationModelDocument.AssociationModel;
import org.dmg.pmml.ClusteringModelDocument;
import org.dmg.pmml.ClusteringModelDocument.ClusteringModel;
import org.dmg.pmml.GeneralRegressionModelDocument;
import org.dmg.pmml.GeneralRegressionModelDocument.GeneralRegressionModel;
import org.dmg.pmml.MiningModelDocument;
import org.dmg.pmml.MiningModelDocument.MiningModel;
import org.dmg.pmml.NaiveBayesModelDocument;
import org.dmg.pmml.NaiveBayesModelDocument.NaiveBayesModel;
import org.dmg.pmml.NeuralNetworkDocument;
import org.dmg.pmml.NeuralNetworkDocument.NeuralNetwork;
import org.dmg.pmml.RegressionModelDocument;
import org.dmg.pmml.RegressionModelDocument.RegressionModel;
import org.dmg.pmml.RuleSetModelDocument;
import org.dmg.pmml.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml.SequenceModelDocument;
import org.dmg.pmml.SequenceModelDocument.SequenceModel;
import org.dmg.pmml.SupportVectorMachineModelDocument;
import org.dmg.pmml.SupportVectorMachineModelDocument.SupportVectorMachineModel;
import org.dmg.pmml.TextModelDocument;
import org.dmg.pmml.TextModelDocument.TextModel;
import org.dmg.pmml.TimeSeriesModelDocument;
import org.dmg.pmml.TimeSeriesModelDocument.TimeSeriesModel;
import org.dmg.pmml.TreeModelDocument;
import org.dmg.pmml.TreeModelDocument.TreeModel;

/**
 * Enum to describe valid PMML models as of version 4.0.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public enum PMMLModelType {
        /** PMML TreeModel. */
        TreeModel(TreeModelDocument.TreeModel.type, TreeModel.class),
        /** PMML NeuralNetwork. */
        NeuralNetwork(NeuralNetworkDocument.NeuralNetwork.type,
                NeuralNetwork.class),
        /** PMML ClusteringModel. */
        ClusteringModel(ClusteringModelDocument.ClusteringModel.type,
                ClusteringModel.class),
        /** PMML RegressionModel. */
        RegressionModel(RegressionModelDocument.RegressionModel.type,
                RegressionModel.class),
        /** PMML GeneralRegressionModel. */
        GeneralRegressionModel(
                GeneralRegressionModelDocument.GeneralRegressionModel.type,
                GeneralRegressionModel.class),
        /** PMML NaiveBayes. */
        NaiveBayesModel(NaiveBayesModelDocument.NaiveBayesModel.type,
                NaiveBayesModel.class),
        /** PMML AssociationModel.*/
        AssociationModel(AssociationModelDocument.AssociationModel.type,
                AssociationModel.class),
        /** PMML SequenceModel. */
        SequenceModel(SequenceModelDocument.SequenceModel.type,
                SequenceModel.class),
        /** PMML Support Vector Machine Model. */
        SupportVectorMachineModel(
                SupportVectorMachineModelDocument
                    .SupportVectorMachineModel.type,
                SupportVectorMachineModel.class),
        /** PMML Model Composition. */
        MiningModel(MiningModelDocument.MiningModel.type,
                MiningModel.class),
        /** PMML RuleSetModel. */
        RuleSetModel(RuleSetModelDocument.RuleSetModel.type,
                RuleSetModel.class),
        /** PMML TextModel. */
        TextModel(TextModelDocument.TextModel.type,
                TextModel.class),
        /** PMML TimeSeriesModel. */
        TimeSeriesModel(TimeSeriesModelDocument.TimeSeriesModel.type,
                TimeSeriesModel.class),
        /** No valid PMML model was found. */
        None(null, null);

        private static final Map<String, PMMLModelType> NAMES;
        private static final Map<SchemaType, Class<?>> CLASSES;
        private static final Map<SchemaType, PMMLModelType> TYPES;
        /**
         * A string representation of all PMMLModelTypes.
         */
        public static final String TYPESTRING;

        static {
            PMMLModelType[] values = PMMLModelType.values();
            NAMES = new HashMap<String, PMMLModelType>(values.length);
            CLASSES = new HashMap<SchemaType, Class<?>>(values.length);
            TYPES = new HashMap<SchemaType, PMMLModelType>(values.length);
            StringBuffer sb = new StringBuffer();
            boolean first = true;
            for (PMMLModelType type : values) {
                String name = type.toString();
                NAMES.put(name, type);
                CLASSES.put(type.getXmlBeansType(), type.getXmlBeansClass());
                TYPES.put(type.getXmlBeansType(), type);
                if (!first) {
                    sb.append(", ");
                }
                sb.append(name);
                first = false;
            }
            TYPESTRING = sb.toString();
        }

        private SchemaType m_xmlbeansType;
        private Class<?> m_xmlbeansClass;

        private PMMLModelType(final SchemaType type, final Class<?> clazz) {
            m_xmlbeansType = type;
            m_xmlbeansClass = clazz;
        }

        /**
         * @return the schema type of the corresponding xmlbean generated class
         */
        public SchemaType getXmlBeansType() {
            return m_xmlbeansType;
        }

        /**
         * @return the corresponding xmlbean generated class
         */
        public Class<?> getXmlBeansClass() {
            return m_xmlbeansClass;
        }

        /**
         * @param name the name of the pmml model
         * @return the model type corresponding to the name or null if there
         *      is no model type of the given name
         */
        public static PMMLModelType getType(final String name) {
            return NAMES.get(name);
        }

        /**
         * @param type the xmlbeans schema type
         * @return the model type corresponding to the xmlbeans schema type or
         *      null if there is no model type of the given name
         */
        public static PMMLModelType getType(final SchemaType type) {
            return TYPES.get(type);
        }

        /**
         * @param type the xmlbeans schema type
         * @return the associated xmlbeans class
         */
        public static Class<?> getXmlbeansClass(final SchemaType type) {
            return CLASSES.get(type);
        }

        /**
         * @param name the name of the pmml model
         * @return true, if a pmml model of this type exists, false otherwise
         */
        public static boolean contains(final String name) {
            return NAMES.containsKey(name);
        }
}

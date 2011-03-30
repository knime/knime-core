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
 */
package org.knime.core.node.port.pmml;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum to describe valid PMML models as of version 3.2.
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
        /** PMML Support Vector Machine Model. */
        SupportVectorMachineModel,
        /** PMML Model Composition. */
        MiningModel,
        /** PMML RuleSetModel. */
        RuleSetModel,
        /** PMML TextModel. */
        TextModel,
        /** No valid PMML model was found. */
        None;

        private static final Map<String, PMMLModelType> NAMES;
        /**
         * A string representation of all PMMLModelTypes.
         */
        public static final String TYPESTRING;

        static {
            PMMLModelType[] values = PMMLModelType.values();
            NAMES = new HashMap<String, PMMLModelType>(
                    values.length);
            StringBuffer sb = new StringBuffer();
            boolean first = true;
            for (PMMLModelType type : values) {
                String name = type.toString();
                NAMES.put(name, type);
                if (!first) {
                    sb.append(", ");
                }
                sb.append(name);
                first = false;
            }
            TYPESTRING = sb.toString();
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
         * @param name the name of the pmml model
         * @return true, if a pmml model of this type exists, false otherwise
         */
        public static boolean contains(final String name) {
            return NAMES.containsKey(name);
        }
}

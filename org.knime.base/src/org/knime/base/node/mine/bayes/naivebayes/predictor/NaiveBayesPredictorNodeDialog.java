/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * -------------------------------------------------------------------
 *
 * History
 *   02.05.2006 (koetter): created
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;

/**
 * <code>NodeDialog</code> for the "Naive Bayes Predictor" Node.
 *
 * @author Tobias Koetter
 */
public class NaiveBayesPredictorNodeDialog extends DefaultNodeSettingsPane {

    private static final String INCL_PROB_VALS_LABEL =
        "Append probability value column per class instance";

    /**
     * New pane for configuring BayesianClassifier node dialog.
     */
    public NaiveBayesPredictorNodeDialog() {
        super();
        final SettingsModelBoolean inclProbValsModel = new SettingsModelBoolean(
                NaiveBayesPredictorNodeModel.CFG_INCL_PROBABILITYVALS_KEY,
                false);
        final DialogComponentBoolean inclprobValsComponent =
            new DialogComponentBoolean(inclProbValsModel, INCL_PROB_VALS_LABEL);
         final SettingsModelDouble laplaceCorrectorModel =
             new SettingsModelDoubleBounded(
                     NaiveBayesPredictorNodeModel.CFG_LAPLACE_CORRECTOR_KEY,
                     0.0, 0.0, Double.MAX_VALUE);
         final DialogComponentNumber laplaceCorrectorComponent =
             new DialogComponentNumber(laplaceCorrectorModel,
                     "Laplace corrector: ", new Double(0.1), 5);
         laplaceCorrectorComponent.setToolTipText(
                 "Set to zero for no correction");

         addDialogComponent(inclprobValsComponent);
         addDialogComponent(laplaceCorrectorComponent);
    }
}

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
 * -------------------------------------------------------------------
 * 
 * History
 *   26.10.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.rprop;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The RPropNodeDialog allows to configure the settings (nr. of training
 * iterations and architecture of the neural net).
 * 
 * @author Nicolas, University of Konstanz
 */
public class RPropNodeDialog extends DefaultNodeSettingsPane {
    /**
     * Creates a new <code>NodeDialogPane</code> for the RProp neural net in
     * order to set the desired options.
     */
    @SuppressWarnings("unchecked")
    public RPropNodeDialog() {
        super();
        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
        /* config-name: */RPropNodeModel.MAXITER_KEY,
        /* default */20,
        /* min: */1,
        /* max: */RPropNodeModel.MAXNRITERATIONS), 
        /* label: */"Maximum number of iterations: ",
        /* step */1));
        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
        /* config-name: */RPropNodeModel.HIDDENLAYER_KEY,
        /* default */1,
        /* min: */1,
        /* max: */100),
        /* label: */"Number of hidden layers: ",
        /* step */ 1));
        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
        /* config-name: */RPropNodeModel.NRHNEURONS_KEY,
        /* default */5,
        /* min: */1,
        /* max: */100), 
        /* label: */"Number of hidden neurons per layer: ",
        /* step */ 1));
        this.addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(
        /* config-name: */RPropNodeModel.CLASSCOL_KEY, ""),
        /* label: */"class column: ",
        /* columns from which port?: */RPropNodeModel.INPORT,
        /* column-type filter: */DataValue.class));
        
        this.addDialogComponent(new DialogComponentBoolean(
                new SettingsModelBoolean(
        /* config-name: */RPropNodeModel.IGNOREMV_KEY,
        /* default */ false),
        /* label: */"Ignore Missing Values"));
    }
}

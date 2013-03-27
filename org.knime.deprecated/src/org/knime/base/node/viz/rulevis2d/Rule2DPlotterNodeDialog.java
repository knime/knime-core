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
 * -------------------------------------------------------------------
 * 
 * History
 *   30.06.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.rulevis2d;

import org.knime.base.node.viz.scatterplot.ScatterPlotNodeDialog;
import org.knime.core.node.defaultnodedialog.DialogComponent;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;


/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class Rule2DPlotterNodeDialog extends ScatterPlotNodeDialog {
    
    private static final int START_ROW = 1;
    private static final int MAX_ROW = 1000;
    
    
    /**
     * Creates a Dialog just like the scatter plotter but with a start row
     * and a maximum nuber of rows for the ruiles, too.
     *
     */
    public Rule2DPlotterNodeDialog() {
        super();
        DialogComponent startRow = new DialogComponentNumber(
                Rule2DNodeModel.CFG_RULES_START_ROW, 
                "First rule row to display:",
                START_ROW, MAX_ROW, START_ROW);
        addDialogComponent(startRow);
        DialogComponent maxRow = new DialogComponentNumber(
                Rule2DNodeModel.CFG_RULES_MAX_ROWS, 
                "Max. number of displayed rules:",
                START_ROW, MAX_ROW, MAX_ROW);
        addDialogComponent(maxRow);
    }

}

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
 *   25.10.2006 (sieb): created
 *   10.06.2008 (ohl): adapted to new default node settings classes
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

/**
 * Dialog for the CAIM discretization algorithm. The dialog offers the selection
 * of those numeric columns that are intended to be discretized.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class CAIMDiscretizationNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Constructor: create NodeDialog with one column filter panel.
     */
    @SuppressWarnings("unchecked")
    public CAIMDiscretizationNodeDialog() {

        // create the default components the class column selector
        DialogComponentColumnNameSelection classColumn =
                new DialogComponentColumnNameSelection(
                        CAIMDiscretizationNodeModel.createClassColModel(),
                        "Class column:",
                        CAIMDiscretizationNodeModel.DATA_INPORT,
                        StringValue.class);
        this.addDialogComponent(classColumn);

        // the column filter panel
        DialogComponentColumnFilter columnFilter =
                new DialogComponentColumnFilter(CAIMDiscretizationNodeModel
                        .createIncludeColsModel(),
                        CAIMDiscretizationNodeModel.DATA_INPORT,
                        DoubleValue.class);
        this.addDialogComponent(columnFilter);

        // whether to sort in memory
        DialogComponentBoolean inMemory =
                new DialogComponentBoolean(
                        CAIMDiscretizationNodeModel.createSortInMemModel(),
                        "Sort in memory:");

        this.addDialogComponent(inMemory);
    }
}

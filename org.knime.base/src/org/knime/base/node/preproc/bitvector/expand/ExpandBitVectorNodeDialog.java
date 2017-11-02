/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *
 * Created on 2014.03.20. by gabor
 */
package org.knime.base.node.preproc.bitvector.expand;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.ExpandVectorNodeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

/**
 * <code>NodeDialog</code> for the "ExpandBitVector" Node. Expands the bitvector to individual integer columns.
 *
 * @author Gabor Bakos
 * @since 2.10
 */
public class ExpandBitVectorNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the ExpandByteVector node.
     */
    protected ExpandBitVectorNodeDialog() {
        createNewGroup("Input");
        @SuppressWarnings("unchecked")
        final DialogComponentColumnNameSelection input =
            new DialogComponentColumnNameSelection(ExpandVectorNodeModel.createInputColumn(), "Bitvector column", 0,
                true, false, BitVectorValue.class);
        addDialogComponent(input);
        addDialogComponent(new DialogComponentBoolean(ExpandVectorNodeModel.createRemoveOriginal(),
            "Remove original column"));
        final DialogComponentBoolean useColumnNames = new DialogComponentBoolean(ExpandVectorNodeModel.createUseNames(),
            "Use the column names specified in properties");
        addDialogComponent(useColumnNames);
        closeCurrentGroup();
        createNewGroup("Output");
        addDialogComponent(new DialogComponentString(ExpandBitVectorNodeModel.createOutputPrefix(),
            "Prefix of output columns: "));
        addDialogComponent(new DialogComponentNumberEdit(ExpandVectorNodeModel.createStartIndex(),
            "Start index for the first new column: ", 6));
        addDialogComponent(new DialogComponentNumberEdit(ExpandVectorNodeModel.createMaxNewColumns(),
            "Maximum number of new columns", 8));
        input.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final DataColumnSpec selected = input.getSelectedAsSpec();
                if (selected == null) {
                    return;
                }
                useColumnNames.getModel().setEnabled(!selected.getElementNames().isEmpty());
            }
        });
    }
}

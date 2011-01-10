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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 10, 2008 (sellien): created
 */
package org.knime.base.node.viz.liftchart;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Class for an configuration dialog for the lift chart node.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public class LiftChartNodeDialog extends DefaultNodeSettingsPane {

    private SettingsModelString m_responseColumn =
            LiftChartNodeModel.createResponseColumnModel();

    private SettingsModelString m_probabilityColumn =
            LiftChartNodeModel.createProbabilityColumnModel();

    private SettingsModelString m_responseLabel =
            LiftChartNodeModel.createResponseLabelModel();

    private SettingsModelString m_intervalWidth =
            LiftChartNodeModel.createIntervalWidthModel();

    private DataTableSpec m_dataTableSpec;

    private DialogComponentStringSelection m_signDC;

    /**
     * Creates a new lift chart configuration dialog.
     */
    public LiftChartNodeDialog() {
        createNewGroup("True Labels:");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_responseColumn, "Column containing true labels:", 0,
                NominalValue.class));

        m_signDC =
                new DialogComponentStringSelection(m_responseLabel,
                        "Positive label (hits):", 
                        getPossibleLabels(m_responseColumn.getStringValue()));

        m_responseColumn.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                m_signDC.replaceListItems(getPossibleLabels(m_responseColumn
                        .getStringValue()), null);
            }
        });

        addDialogComponent(m_signDC);
        closeCurrentGroup();
        setHorizontalPlacement(false);
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_probabilityColumn, 
                "Column containing score (probabilities):", 0,
                DoubleValue.class));

        addDialogComponent(new DialogComponentStringSelection(m_intervalWidth,
                "Interval width in %:", 
                "0.5", "1", "2", "2.5", "5", "10", "12.5", "20", "25"));

    }

    private List<String> getPossibleLabels(final String resColumn) {
        List<String> labels = new LinkedList<String>();

        if (m_dataTableSpec == null) {
            labels.add("No values given null");
            return labels;
        }
        DataColumnSpec cs = m_dataTableSpec.getColumnSpec(resColumn);

        if(cs == null) {
            labels.add("Column doesn't exist");
            return labels;
        }

        if (!cs.getDomain().hasValues()) {
            labels.add("No values given no val");
            return labels;
        }
        for (DataCell cell : cs.getDomain().getValues()) {
            labels.add(((StringValue)cell).getStringValue());
        }
        return labels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);

        if (specs == null || specs.length == 0 || specs[0] == null) {
            throw new NotConfigurableException("No column specs given.");
        }
        DataTableSpec specNull = specs[0];
        if (specNull.getNumColumns() == 0) {
            throw new NotConfigurableException("No column specs given.");
        }
        boolean foundColumn = false;
        for (int col = 0; col < specNull.getNumColumns(); col++) {
            DataColumnSpec cs = specNull.getColumnSpec(col);
            if (cs.getType().isCompatible(NominalValue.class)
                    && cs.getDomain().hasValues()) {
                foundColumn = true;
                break;
            }
        }
        if (!foundColumn) {
            throw new NotConfigurableException(
                    "No nominal column with domain values found."
                            + " Please use the domain calculator first.");
        }

        m_dataTableSpec = specNull;

        m_signDC.replaceListItems(getPossibleLabels(m_responseColumn
                .getStringValue()), null);
    }
}

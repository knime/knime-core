/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.knime.core.node.port.PortObjectSpec;

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
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);

        if (specs == null || specs.length == 0 || specs[0] == null) {
            throw new NotConfigurableException("No column specs given.");
        }
        if (!(specs[0] instanceof DataTableSpec)) {
            throw new NotConfigurableException("Wrong column specs given."
                    + " Port 0 is not a DataTableSpec!");
        }
        DataTableSpec specNull = (DataTableSpec)specs[0];
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

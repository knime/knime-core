/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.scatterplot;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.viz.plotter2D.PlotterPropertiesPanel;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * This implements a panel with the elements allowing the user to set the
 * properties of the ScatterPlotView. It contains combo boxes to select the two
 * columns, a spinner for the starting row, the row count, the dot size, and the
 * zoom factor.
 * 
 * @author ohl University of Konstanz
 */
public class ScatterProps extends PlotterPropertiesPanel {

    private final JComboBox m_xCol;

    private final JComboBox m_yCol;

    private final Vector<DataColumnSpec> m_xAvailCol;

    /**
     * The currently set table spec for the combo boxes.
     */
    private DataTableSpec m_tableSpec;

    /**
     * creates a property pane for the scatter plot view.
     * 
     */
    public ScatterProps() {

        // the combos for the X and Y columns
        m_xCol = new JComboBox();
        m_xCol.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                selectedXColChanged((DataColumnSpec)m_xCol.getSelectedItem());
            }
        });
        m_xCol.setMinimumSize(new Dimension(100, 25));
        m_xCol.setMaximumSize(new Dimension(200, 25));
        m_xCol.setRenderer(new DataColumnSpecListCellRenderer());
        m_xAvailCol = new Vector<DataColumnSpec>();

        Box xLabelBox = Box.createHorizontalBox();
        xLabelBox.add(new JLabel("X Column"));
        xLabelBox.add(Box.createHorizontalGlue());
        // create x column selection panel
        Box xColBox = Box.createVerticalBox();
        xColBox.add(xLabelBox);
        xColBox.add(Box.createVerticalGlue());
        xColBox.add(m_xCol);

        m_yCol = new JComboBox();
        m_yCol.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                selectedYColChanged((DataColumnSpec)m_yCol.getSelectedItem());
            }
        });
        m_yCol.setMinimumSize(new Dimension(100, 25));
        m_yCol.setMaximumSize(new Dimension(200, 25));
        m_yCol.setRenderer(new DataColumnSpecListCellRenderer());

        // create y column selection panel
        Box yLabelBox = Box.createHorizontalBox();
        yLabelBox.add(new JLabel("Y Column"));
        yLabelBox.add(Box.createHorizontalGlue());
        Box yColumnBox = Box.createVerticalBox();
        yColumnBox.add(yLabelBox);
        yColumnBox.add(Box.createVerticalGlue());
        yColumnBox.add(m_yCol);

        // the button for more settings
        JButton settings = new JButton("More settings...");
        settings.setMaximumSize(new Dimension(100, 20));
        settings.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                openSettingsDialog();
            }
        });
        
        JPanel compositePanel = new JPanel();
        compositePanel.setLayout(new BoxLayout(compositePanel, 
                BoxLayout.Y_AXIS));
        
        Box columnBox = Box.createHorizontalBox();
        columnBox.add(xColBox);
        columnBox.add(yColumnBox);
        
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(settings);
        
        compositePanel.add(columnBox);
        compositePanel.add(buttonBox);
        compositePanel.setBorder(BorderFactory.createTitledBorder(
                "Column Selection"));
        addPropertiesComponent(compositePanel);

    }
    
    /**
     * 
     * @param tSpec the data table spec
     * @param filterClass allowed classes
     */
    public void setSelectables(final DataTableSpec tSpec, 
            final Class<? extends DataValue>... filterClass) {
        List<Class<? extends DataValue>> filters = Arrays.asList(filterClass);
        // do nothing if the table spec has been already before
        if (m_tableSpec == tSpec) {
            return;
        }

        // else set the given table spec as the current
        m_tableSpec = tSpec;

        m_xCol.setEnabled(true);
        m_yCol.setEnabled(true);
        m_xAvailCol.clear();

        if (tSpec != null) {
            // put all column names of type double in the drop box
            List<DataColumnSpec> compatibleSpecs 
                = new ArrayList<DataColumnSpec>();
            for (int i = 0; i < tSpec.getNumColumns(); i++) {
                // if we can get a number from that column: add it to the vector

                // check which columns are displayable
                DataType type = tSpec.getColumnSpec(i).getType();
                for (Class<? extends DataValue> cl : filters) {
                    if (type.isCompatible(cl)) {
                        compatibleSpecs.add(tSpec.getColumnSpec(i));
                    }
                }
            }
            for (DataColumnSpec compSpec : compatibleSpecs) {
                // check which columns are displayable
                if (Coordinate.createCoordinate(compSpec) != null) {
                    m_xAvailCol.add(compSpec);
                }
            }
        }


        // store the old selection - in case it's still good
        DataColumnSpec xColSel = (DataColumnSpec)m_xCol.getSelectedItem();
        DataColumnSpec yColSel = (DataColumnSpec)m_yCol.getSelectedItem();

        // now set the (possibly empty) list
        m_xCol.setModel(new DefaultComboBoxModel(m_xAvailCol));
        m_yCol.setModel(new DefaultComboBoxModel(m_xAvailCol));

        // check if we can reuse the old selection
        if (xColSel != null && yColSel != null) {
            m_xCol.setSelectedItem(xColSel);
            m_yCol.setSelectedItem(yColSel);
        } else {
            // set default values if we dont have any selected values
            if ((xColSel == null) && (m_xAvailCol.size() > 0)) {
                m_xCol.setSelectedItem(m_xAvailCol.get(0));
            }
            if (yColSel == null) {
                if (m_xAvailCol.size() > 1) {
                    m_yCol.setSelectedItem(m_xAvailCol.get(1));
                } else if (m_xAvailCol.size() > 0) {
                    m_yCol.setSelectedItem(m_xAvailCol.get(0));
                }
            }
        }
        selectedXColChanged((DataColumnSpec)m_xCol.getSelectedItem());
        selectedYColChanged((DataColumnSpec)m_yCol.getSelectedItem());
    }

    /**
     * Triggers new items in the x/y col combos.
     * 
     * @param tSpec a table spec containing the new columns
     */
    @SuppressWarnings("unchecked")
    public void setSelectables(final DataTableSpec tSpec) {
        setSelectables(tSpec, DataValue.class);
    }

    /**
     * Convenience method to cast the abstract plotter.
     * 
     * @return the undelying scatter plotter
     */
    private ScatterPlotter getScatterPlotter() {

        return (ScatterPlotter)getPlotter();
    }

    /**
     * called when the 'more settings' button is pressed.
     */
    void openSettingsDialog() {

        String xColName = "<not sel.>";
        String yColName = "<not sel.>";
        if (getScatterPlotter().getXColName() != null) {
            xColName = getScatterPlotter().getXColName().toString();
        }
        if (getScatterPlotter().getYColName() != null) {
            yColName = getScatterPlotter().getYColName().toString();

        }

        ScatterSettingsDialog settings = new ScatterSettingsDialog(this,
                xColName, new double[]{getScatterPlotter().getXmin(),
                        getScatterPlotter().getXmax()}, yColName, new double[]{
                        getScatterPlotter().getYmin(),
                        getScatterPlotter().getYmax()}, getScatterPlotter()
                        .getDotSize(), (Frame)getTopLevelAncestor());
        centerDialog(settings);
        settings.pack();
        settings.setVisible(true);
        // the dialog is modal - this function won't return until it's closed.
    }

    /**
     * Sets this dialog in the center of the screen observing the current screen
     * size.
     */
    private void centerDialog(final ScatterSettingsDialog dlg) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = dlg.getSize();
        dlg.setBounds(Math.max(0, (screenSize.width - size.width) / 2), Math
                .max(0, (screenSize.height - size.height) / 2), Math.min(
                screenSize.width, size.width), Math.min(screenSize.height,
                size.height));
    }

    /**
     * This fct is called from the 'more...'-Settings dialog. It reads the
     * values from the settings dialog and sets them in the plotting panel.
     * 
     * @param dlg the dialog to read the settings from
     */
    void applyProperties(final ScatterSettingsDialog dlg) {

        // and set the dot size.
        getScatterPlotter().setDotSize(dlg.getDotSize());

        getScatterPlotter().setRanges(dlg.getXmin(), dlg.getXmax(),
                dlg.getYmin(), dlg.getYmax());
    }

    /**
     * called whenever user changes the x column selection.
     * 
     * @param xColName The new selected x column
     */
    protected void selectedXColChanged(final DataColumnSpec xColName) {
        getScatterPlotter().removeUserXRangeNoReCalc();
        getScatterPlotter().setXColumn(xColName.getName());
        m_xCol.setToolTipText(xColName.getName());
    }

    /**
     * called whenever user changes the y column selection.
     * 
     * @param yColName The new selected y column.
     */
    protected void selectedYColChanged(final DataColumnSpec yColName) {
        getScatterPlotter().removeUserYRangeNoReCalc();
        getScatterPlotter().setYColumn(yColName.getName());
        m_yCol.setToolTipText(yColName.getName());
    }
    
    /**
     * If the index of the x column to select is known, this is a convenient 
     * way to update the model only once.
     *  
     * @param index the index to select.
     */
    public void setSelectedXColIndex(final int index) {
        if (m_xCol != null && m_xCol.getModel().getSize() > index) {
            m_xCol.setSelectedIndex(index);
        }
    }

    /**
     * If the index of the y column to select is known, this is a convenient 
     * way to update the model only once.
     *  
     * @param index the index to select.
     */
    public void setSelectedYColIndex(final int index) {
        if (m_yCol != null && m_yCol.getModel().getSize() > index) {
            m_yCol.setSelectedIndex(index);
        }
    }

    /**
     * Attempts to set the argument as selected y column and disables the combo
     * box if there is only one available.
     * 
     * @param yColName Name of the fixed y columns.
     */
    public void fixYColTo(final String... yColName) {
        DataColumnSpec oldSelected = (DataColumnSpec)m_yCol.getSelectedItem();
        HashSet<String> hash = new HashSet<String>(Arrays.asList(yColName));
        Vector<DataColumnSpec> survivers = new Vector<DataColumnSpec>();
        for (DataColumnSpec s : m_xAvailCol) {
            if (hash.contains(s.getName())) {
                survivers.add(s);
            }
        }
        m_yCol.setModel(new DefaultComboBoxModel(survivers));
        if (survivers.contains(oldSelected)) {
            m_yCol.setSelectedItem(oldSelected);
        } else {
            // may be -1 ... but that is ok
            m_yCol.setSelectedIndex(survivers.size() - 1);
        }
        m_yCol.setEnabled(survivers.size() > 1);
    }

    /**
     * Attempts to set the argument as selected x column and disables the combo
     * box if there is only one available.
     * 
     * @param xColName Name of the fixed x columns.
     */
    public void fixXColTo(final String... xColName) {
        DataColumnSpec oldSelected = (DataColumnSpec)m_xCol.getSelectedItem();
        HashSet<String> hash = new HashSet<String>(Arrays.asList(xColName));
        Vector<DataColumnSpec> survivers = new Vector<DataColumnSpec>();
        for (DataColumnSpec s : m_xAvailCol) {
            if (hash.contains(s.getName())) {
                survivers.add(s);
            }
        }
        m_xCol.setModel(new DefaultComboBoxModel(survivers));
        if (survivers.contains(oldSelected)) {
            m_xCol.setSelectedItem(oldSelected);
        } else {
            // may be -1 ... but that is ok
            m_xCol.setSelectedIndex(survivers.size() - 1);
        }
        m_xCol.setEnabled(survivers.size() > 1);
    }
}

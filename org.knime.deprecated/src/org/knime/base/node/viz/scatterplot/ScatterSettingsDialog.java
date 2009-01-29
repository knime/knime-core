/* 
 * 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   01.04.2005 (ohl): created
 */
package org.knime.base.node.viz.scatterplot;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 
 * @author ohl, University of Konstanz
 */
class ScatterSettingsDialog extends JDialog {

    // the properties pane we are the dialog for
    private final ScatterProps m_sProps;

    /*
     * the components for the settings.
     */
    private JLabel m_xColName;

    private JLabel m_yColName;

    private JSpinner m_xMin;

    private JSpinner m_xMax;

    private JSpinner m_yMin;

    private JSpinner m_yMax;

    private JSpinner m_dotSize;

    private boolean m_xMinChanged;

    private boolean m_xMaxChanged;

    private boolean m_yMinChanged;

    private boolean m_yMaxChanged;
    
    private static final int MAX_DOT_SIZE = 150;

    /**
     * Creates a new dialog for the specified scatterplotter. It allows setting
     * some parameters of the plotter.
     * 
     * @param sProps the properties pane for the scatterplotter
     * @param xColName name of the x column
     * @param xMinMax starting min[0] and max[1] value for x range
     * @param yColName name of the y column
     * @param yMinMax starting min[0] and max[1] value for y range
     * @param dotSize starting value for the dot size
     * @param parent The parent frame of this settings dialog
     */
    ScatterSettingsDialog(final ScatterProps sProps, final String xColName,
            final double[] xMinMax, final String yColName,
            final double[] yMinMax, final int dotSize, final Frame parent) {
        super(parent, true);

        m_sProps = sProps;

        setTitle("Extended Scatterplotter Settings");
        setModal(true);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        m_xMinChanged = false;
        m_xMaxChanged = false;
        m_yMinChanged = false;
        m_yMaxChanged = false;

        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

        // instantiate the settings components
        m_xColName = new JLabel(xColName);
        m_yColName = new JLabel(yColName);
        m_xMin = new JSpinner(new SpinnerNumberModel(new Double(xMinMax[0]),
                null, null, new Double(0.1)));
        if (Double.isNaN(xMinMax[0])) {
            m_xMin.setEnabled(false);
        }
        m_xMin.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_xMinChanged = true;
            }
        });
        m_xMin.setMaximumSize(new Dimension(65, 30));
        m_xMax = new JSpinner(new SpinnerNumberModel(new Double(xMinMax[1]),
                null, null, new Double(0.1)));
        if (Double.isNaN(xMinMax[1])) {
            m_xMax.setEnabled(false);
        }
        m_xMax.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_xMaxChanged = true;
            }
        });
        m_xMax.setMaximumSize(new Dimension(65, 30));
        m_yMin = new JSpinner(new SpinnerNumberModel(new Double(yMinMax[0]),
                null, null, new Double(0.1)));
        if (Double.isNaN(yMinMax[0])) {
            m_yMin.setEnabled(false);
        }
        m_yMin.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_yMinChanged = true;
            }
        });
        m_yMin.setMaximumSize(new Dimension(65, 30));
        m_yMax = new JSpinner(new SpinnerNumberModel(new Double(yMinMax[1]),
                null, null, new Double(0.1)));
        if (Double.isNaN(yMinMax[1])) {
            m_yMax.setEnabled(false);
        }
        m_yMax.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_yMaxChanged = true;
            }
        });
        m_yMax.setMaximumSize(new Dimension(65, 30));
        m_dotSize = new JSpinner(new SpinnerNumberModel(dotSize, 1, 
                MAX_DOT_SIZE, 2));
        m_dotSize.setMaximumSize(new Dimension(45, 30));

        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (apply()) {
                    close();
                }
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                close();
            }
        });

        createBoxes(cont, okButton, cancelButton);
    }



    /**
     * @param cont
     * @param okButton
     * @param cancelButton
     */
    private void createBoxes(final Container cont, 
            final JButton okButton, final JButton cancelButton) {
        // arrange them
        Box infoBox = Box.createHorizontalBox();
        Box iBox = Box.createVerticalBox();
        iBox.add(new JLabel("To change the set of displayed data rows"));
        iBox.add(new JLabel("please open the node's dialog and re-execute."));
        infoBox.add(Box.createGlue());
        infoBox.add(iBox);
        infoBox.add(Box.createGlue());

        Box xBox = Box.createHorizontalBox();
        xBox.add(Box.createGlue());
        xBox.add(m_xColName);
        xBox.add(Box.createHorizontalStrut(10));
        xBox.add(new JLabel("X Range from"));
        xBox.add(Box.createHorizontalStrut(2));
        Box xminBox = Box.createVerticalBox();
        xminBox.add(Box.createHorizontalStrut(50));
        xminBox.add(m_xMin);
        xBox.add(xminBox);
        xBox.add(Box.createHorizontalStrut(2));
        xBox.add(new JLabel("to"));
        xBox.add(Box.createHorizontalStrut(2));
        Box xmaxBox = Box.createVerticalBox();
        xmaxBox.add(Box.createHorizontalStrut(50));
        xmaxBox.add(m_xMax);
        xBox.add(xmaxBox);

        Box yBox = Box.createHorizontalBox();
        yBox.add(Box.createGlue());
        yBox.add(m_yColName);
        yBox.add(Box.createHorizontalStrut(10));
        yBox.add(new JLabel("Y Range from"));
        yBox.add(Box.createHorizontalStrut(2));
        Box yminBox = Box.createVerticalBox();
        yminBox.add(Box.createHorizontalStrut(50));
        yminBox.add(m_yMin);
        yBox.add(yminBox);
        yBox.add(Box.createHorizontalStrut(2));
        yBox.add(new JLabel("to"));
        yBox.add(Box.createHorizontalStrut(2));
        Box ymaxBox = Box.createVerticalBox();
        ymaxBox.add(Box.createHorizontalStrut(50));
        ymaxBox.add(m_yMax);
        yBox.add(ymaxBox);

        Box sizeBox = Box.createHorizontalBox();
        sizeBox.add(Box.createHorizontalStrut(20));
        sizeBox.add(new JLabel("Dot size:"));
        sizeBox.add(Box.createHorizontalStrut(2));
        sizeBox.add(m_dotSize);
        sizeBox.add(Box.createGlue());
        sizeBox.add(Box.createGlue());

        Box byeBox = Box.createHorizontalBox();
        byeBox.add(Box.createGlue());
        byeBox.add(Box.createGlue());
        byeBox.add(okButton);
        byeBox.add(cancelButton);

        JPanel rangePanel = new JPanel();
        rangePanel.setLayout(new BoxLayout(rangePanel, BoxLayout.Y_AXIS));
        rangePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Range Settings"));
        rangePanel.add(xBox);
        rangePanel.add(yBox);

        // add them to the dialog
        cont.add(infoBox);
        cont.add(rangePanel);
        cont.add(sizeBox);
        cont.add(byeBox);
    }
    
    

    /**
     * @return the selected number or null if no user change occured
     */
    Double getXmin() {
        if (!m_xMinChanged) {
            return null;
        }
        return new Double(readDoubleSpinner(m_xMin));
    }

    /**
     * @return the selected number or null if no user change occured
     */
    Double getYmin() {
        if (!m_yMinChanged) {
            return null;
        }
        return new Double(readDoubleSpinner(m_yMin));
    }

    /**
     * @return the selected number or null if no user change occured
     */
    Double getXmax() {
        if (!m_xMaxChanged) {
            return null;
        }
        return new Double(readDoubleSpinner(m_xMax));
    }

    /**
     * @return the selected number or null if no user change occured
     */
    Double getYmax() {
        if (!m_yMaxChanged) {
            return null;
        }
        return new Double(readDoubleSpinner(m_yMax));
    }

    /**
     * @return the currently set dot size.
     */
    int getDotSize() {
        return readIntSpinner(m_dotSize);
    }

    /**
     * closes the dialog.
     */
    protected void close() {
        setVisible(false);
        dispose();
    }

    /**
     * called when okay is pressed.
     * 
     * @return true if the settings were alright and applied, false when if
     *         showed a warning dialog with an error message
     */
    protected boolean apply() {
        if (checkRangesAndWarn()) {
            m_sProps.applyProperties(this);
            return true;
        }
        return false;
    }

    /*
     * read the current value from the spinner assuming it contains Integers
     */
    private int readIntSpinner(final JSpinner intSpinner) {
        try {
            intSpinner.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)intSpinner.getModel();
        return snm.getNumber().intValue();
    }

    /*
     * read the current value from the spinner assuming it contains Doubles
     */
    private double readDoubleSpinner(final JSpinner dblSpinner) {
        try {
            dblSpinner.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)dblSpinner.getModel();
        return snm.getNumber().doubleValue();
    }

    /*
     * returns true if changed values are correct, i.e. min values are less than
     * the max values.
     */
    private boolean checkRangesAndWarn() {
        String msg = "";
        if ((getXmin() != null) || (getXmax() != null)) {
            if (readDoubleSpinner(m_xMin) >= readDoubleSpinner(m_xMax)) {
                msg += "X range incorrect: min >= max!!\n";
            }
        }
        if ((getYmin() != null) || (getYmax() != null)) {
            if (readDoubleSpinner(m_yMin) >= readDoubleSpinner(m_yMax)) {
                msg += "Y range incorrect: min >= max!!\n";
            }
        }
        if (!msg.equals("")) {
            msg += "Settings not applied.";
            JOptionPane.showConfirmDialog(this, msg, "Invalid Range Settings",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;

    }
}

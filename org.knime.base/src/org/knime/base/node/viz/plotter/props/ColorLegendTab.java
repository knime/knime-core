/*
 * ------------------------------------------------------------------
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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Implements a color legend with a "change" button for each entry which opens 
 * a color chooser dialog. The color legend can be updated with a 
 * <code>Map&lt;String, Color&gt;</code>. In contrast to the other properties 
 * tabs the component has no getter but this class provides methods to register
 * <code>ChangeListener</code>s.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ColorLegendTab extends PropertiesTab {
    
    private static final int STRUT = 20;
    private static final int SMALL_STRUT = 10;
    
    private static final int SIZE = 20;
    
    private static final int ROWS = 5;
    
    
    private Map<String, Color> m_mapping;
    
    private List<ChangeListener>m_listener;
    
    /**
     * 
     *
     */
    public ColorLegendTab() {
        m_listener = new ArrayList<ChangeListener>();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultName() {
        return "Color Legend";
    }
    
    /**
     * Adds a listener which gets informed whenever the color mapping was 
     * changed.
     * 
     * @param listener the listener
     */
    public void addChangeListener(final ChangeListener listener) {
        m_listener.add(listener);
    }
    
    /**
     * Removes the passed listener if available.
     * 
     * @param listener the listener
     * @return true if sucessful, false otherwise
     */
    public boolean removeChangeListener(final ChangeListener listener) {
        return m_listener.remove(listener);
    }
    
    /**
     * Removes all change listener.
     *
     */
    public void removeAllChangeListener() {
        m_listener.clear();
    }
  
    /**
     * Updates the color legend with the column names and the referring color.
     * 
     * @param mapping column name - color
     */
    public void update(final Map<String, Color> mapping) {
        removeAll();
        m_mapping = mapping;
        JPanel composite = new JPanel();
        int rows = (int)Math.ceil((double)mapping.size() / (double)ROWS); 
        composite.setLayout(new GridLayout(5, rows));
        for (Map.Entry<String, Color> entry : mapping.entrySet()) {
            composite.add(createOneItem(entry.getKey(), entry.getValue()));
        }
        add(composite);
    }
    
    /**
     * 
     * @return the mapping from column to color
     */
    public Map<String, Color> getColorMapping() {
        return m_mapping;
    }
    
    /**
     * Creates a box with the column name on the left and the color in the 
     * middle and a button to change the color on the right.
     *  
     * @param columnName name of the column
     * @param color color so far
     * @return a box as described above
     */
    private Box createOneItem(final String columnName, final Color color) {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        box.add(Box.createHorizontalStrut(SMALL_STRUT));
        box.add(new JLabel(columnName));
        box.add(Box.createHorizontalStrut(STRUT));
        box.add(Box.createHorizontalGlue());
        JPanel colorPanel = new JPanel();
        colorPanel.setBackground(color);
        colorPanel.setMinimumSize(new Dimension(SIZE, SIZE));
        colorPanel.setMaximumSize(new Dimension(SIZE, SIZE));
        colorPanel.setPreferredSize(new Dimension(SIZE, SIZE));
        box.add(colorPanel);
        box.add(Box.createHorizontalStrut(STRUT));
        box.add(createChangeColorButton(columnName, color));
        box.add(Box.createHorizontalStrut(SMALL_STRUT));
        return box;
    }
    
    /**
     * Creates a button which opens a color chooser with the current color 
     * preselected, updates the color mapping and informs the listeners.
     *  
     * @param colName column name
     * @param currColor current color
     * @return a button with the above described functionality
     */
    private JButton createChangeColorButton(final String colName, 
            final Color currColor) {
        JButton btn = new JButton("Change...");
        final JColorChooser chooser = new JColorChooser(currColor);
        final ActionListener okListener = new ActionListener() {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                // set the new color in the mapping 
                m_mapping.put(colName, chooser.getColor());
                // update the legend
                update(m_mapping);
                // inform the listener about a change 
                for (ChangeListener listener : m_listener) {
                    listener.stateChanged(new ChangeEvent(this));
                }
                
            }
        };        
        btn.addActionListener(new ActionListener() {

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                JDialog dialog = JColorChooser.createDialog(
                        ColorLegendTab.this,
                        "Select new color", true, 
                        chooser, 
                        okListener, null);
                dialog.setVisible(true);
            }
            
        });
        return btn;
    }
}

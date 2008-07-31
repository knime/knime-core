/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 */
package org.knime.base.node.viz.parcoord;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.knime.core.data.DataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteHandler;

  
/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public class ParallelCoordinatesFrame extends JFrame {
    
    /**
     * Create new frame to display Parallel Coordinates from the
     * input <code>DataTable</code>.
     * @param data The table to display.
     * @param exec an object to ask if user canceled operation
     * @throws CanceledExecutionException if user canceled
     */
    public ParallelCoordinatesFrame(final DataTable data, 
            final ExecutionMonitor exec) throws CanceledExecutionException {
        this(data, new DefaultHiLiteHandler(), exec);
    }
    
    /**
     * Creates a new frame to display Parallel Coordinates from the input
     * <code>DataTable</code> and uses the specified <code>HiLiteHandler</code>
     * for event-handling between views.
     * @param data The table to display.
     * @param hdl The <code>HiLiteHandler</code> to register this view to
     *            receive and send hilite events.
     * @param exec an object to ask if user canceled operation
     * @throws CanceledExecutionException if user canceled
     */
    public ParallelCoordinatesFrame(
            final DataTable data, final HiLiteHandler hdl, 
            final ExecutionMonitor exec) throws CanceledExecutionException {
        super("Parallel Coordinates");
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        JMenuItem item = new JMenuItem("Close");
        item.setMnemonic('C');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                dispose();
                setVisible(false);
            }
        });
        menu.add(item);
        menuBar.add(menu);

        ParallelCoordinatesViewContent content =
            new ParallelCoordinatesViewContent(data, new String[0], -1, exec);
        ParallelCoordinatesViewPanel panel =
            new ParallelCoordinatesViewPanel();
        panel.setNewModel(content, hdl, null);
        //setting view type to ALL_VISIBLE
        panel.setViewType(ParallelCoordinatesViewPanel.ALL_VISIBLE);
        //panel.createMenu(menuBar);
        menuBar.add(panel.createHiLiteMenu());
        menuBar.add(panel.createViewTypeMenu());
        menuBar.add(panel.createCoordinateOrderMenu());
        
        
        super.setJMenuBar(menuBar);
        super.getContentPane().add(panel);
        super.pack();
        super.setVisible(true);
    }


}

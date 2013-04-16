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
        this(data, new HiLiteHandler(), exec);
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

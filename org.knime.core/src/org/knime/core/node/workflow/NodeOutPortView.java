/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * History
 *   03.08.2005 (ohl): created
 *   08.05.2006(sieb, ohl): reviewed
 */
package org.knime.core.node.workflow;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DatabasePortObject;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.ModelPortObject;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Implements a view to inspect the data stored in an output port.
 *
 * @author ohl, University of Konstanz
 */
abstract class NodeOutPortView extends JFrame {

    /** Keeps track if view has been opened before. */
    private boolean m_wasOpened = false;

    /** Initial frame width. */
    static final int INIT_WIDTH = 500;

    /** Initial frame height. */
    static final int INIT_HEIGHT = 400;

    /**
     * Returns the appropriate {@link NodeOutPortView} for the passed type.
     * @param type the type of the port
     * @param nodeName the name of the node
     * @param portName the name of the port
     * @return the appropriate type for the given port type
     */
    static NodeOutPortView createOutPortView(final PortType type,
            final String nodeName, final String portName) {
        if (type == BufferedDataTable.TYPE) {
            return new DataOutPortView(nodeName, portName);
        } else if (type == ModelPortObject.TYPE) {
            throw new IllegalArgumentException(
                    "ModelPort type " + type + " not supported yet!");
        } else if (type == NodeModel.OLDSTYLEMODELPORTTYPE) {
            return new ModelContentOutPortView(nodeName, portName);
        } else if (type == DatabasePortObject.TYPE) {
            return new DatabaseOutPortView(nodeName, portName);    
        } else {
            throw new IllegalArgumentException(
                    "Port type " + type + " not supported yet!");
        }
    }

    /**
     * A view showing the data stored in the specified output port.
     *
     * @param name The name of the node the inspected port belongs to.
     */
    NodeOutPortView(final String name) {
        super(name);
        // init frame
        super.setName(name + " View");
        if (KNIMEConstants.KNIME16X16 != null) {
            super.setIconImage(KNIMEConstants.KNIME16X16.getImage());
        }
        super.setBackground(NodeView.COLOR_BACKGROUND);
        super.setSize(INIT_WIDTH, INIT_HEIGHT);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        JMenuItem item = new JMenuItem("Close");
        item.setMnemonic('C');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                setVisible(false);
            }
        });
        menu.add(item);
        menuBar.add(menu);
        setJMenuBar(menuBar);
    }

    /**
     * shows this view and brings it to front.
     */
    void openView() {
        if (!m_wasOpened) { // if the view was already visible
            m_wasOpened = true;
            updatePortView();
            setLocation();
        }
        setVisible(true);
        toFront();
    }

    /**
     * Validates and repaints the super component.
     */
    final void updatePortView() {
        invalidate();
        validate();
        repaint();
    }

    /**
     * Sets this frame in the center of the screen observing the current screen
     * size.
     */
    private void setLocation() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(Math.max(0, (screenSize.width - getWidth()) / 2), Math.max(0,
                (screenSize.height - getHeight()) / 2), Math.min(
                screenSize.width, getWidth()), Math.min(
                        screenSize.height, getHeight()));
    }

    /**
     * Sets the content of the view.
     * @param portObject a data table, model content or other
     * @param portObjectSpec data table spec or model content spec or other spec
     */
    abstract void update(final PortObject portObject,
            final PortObjectSpec portObjectSpec);

}

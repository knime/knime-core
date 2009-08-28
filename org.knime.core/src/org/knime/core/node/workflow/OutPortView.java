/* This source code, its documentation and all appendant files
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
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ViewUtils;

/**
 * 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class OutPortView extends JFrame {
    
    /** Keeps track if view has been opened before. */
    private boolean m_wasOpened = false;

    /** Initial frame width. */
    static final int INIT_WIDTH = 500;

    /** Initial frame height. */
    static final int INIT_HEIGHT = 400;

    private final JTabbedPane m_tabbedPane;
    
    private final LoadingPanel m_loadingPanel = new LoadingPanel();
    
    private static final ExecutorService UPDATE_EXECUTOR = 
        Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger m_counter = new AtomicInteger();
        @Override
        public Thread newThread(final Runnable r) {
            Thread t = new Thread(r, "OutPortView-Updater-" 
                    + m_counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    });

    /**
     * A view showing the data stored in the specified output port.
     *
     * @param nodeNameWithID The name of the node the inspected port belongs to
     * @param portName name of the port which is also displayed in the title
     */
    OutPortView(final String nodeNameWithID, final String portName) {
        super(portName + " - " + nodeNameWithID);
        // init frame
        super.setName(getTitle());
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
        m_tabbedPane = new JTabbedPane();
        getContentPane().add(m_loadingPanel);
    }

    /**
     * shows this view and brings it to front.
     */
    void openView() {
        if (!m_wasOpened) { 
            m_wasOpened = true;
            updatePortView();
            setLocation();
        }
        // if the view was already visible
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
     * @param stack The {@link FlowObjectStack} of the node.
     */
    void update(final PortObject portObject, 
            final PortObjectSpec portObjectSpec,
            final FlowObjectStack stack) {
        // TODO: maybe store the objects, compare them 
        // and only remove and add them if they are different...
        // add all port object tabs
        final Map<String, JComponent> views 
            = new LinkedHashMap<String, JComponent>();
        UPDATE_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                if (portObject != null) {
                    JComponent[] poViews = portObject.getViews();
                    if (poViews != null) {
                        for (JComponent comp : poViews) {
                            views.put(comp.getName(), comp);
                        }
                    }
                } else {
                    // what to display, if no port object is available?
                    JPanel noDataPanel = new JPanel();
                    noDataPanel.setLayout(new BorderLayout());
                    Box boexle = Box.createHorizontalBox();
                    boexle.add(Box.createHorizontalGlue());
                    boexle.add(new JLabel("No data available!"));
                    boexle.add(Box.createHorizontalGlue());
                    noDataPanel.add(boexle, BorderLayout.CENTER);
                    noDataPanel.setName("No Table");
                    views.put("No Table", noDataPanel);
                }
                JComponent[] posViews = portObjectSpec == null 
                    ? new JComponent[0] : portObjectSpec.getViews();
                if (posViews != null) {
                    for (JComponent comp : posViews) {
                        views.put(comp.getName(), comp);
                    }
                }
                if (Boolean.getBoolean(KNIMEConstants.PROPERTY_EXPERT_MODE)) {
                    FlowObjectStackView stackView = new FlowObjectStackView();
                    stackView.update(stack);
                    views.put("Flow Variables", stackView);
                }
                ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
                    @Override
                    public void run() {
                        m_tabbedPane.removeAll();
                        for (Map.Entry<String, JComponent>entry 
                                    : views.entrySet()) {
                            m_tabbedPane.addTab(entry.getKey(), 
                                    entry.getValue());
                        }
                        remove(m_loadingPanel);
                        add(m_tabbedPane);
                        invalidate();
                        validate();
                        repaint();
                    }
                });
            }
        });

    }
}

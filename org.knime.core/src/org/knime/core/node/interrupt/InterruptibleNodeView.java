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
 * 
 * History
 *   11.10.2005 (dill): created
 */
package org.knime.core.node.interrupt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;


/**
 * This class provides a generic view for the InterruptibleNodeModel and all
 * deriving classes, which basically consists in a control panel, with some
 * control elements such as a "Run"-, a "Break"- and a "Finish"-Button and a
 * slider to adjust the delay. Additionally, all the listener stuff is done
 * here, that is the status of he InterruptibleNodeModel is set from here to
 * paused or not or finished.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class InterruptibleNodeView extends NodeView implements
        ActionListener {

    private InterruptControlPanel m_controlPanel;

    private JMenuItem m_runItem = new JMenuItem(InterruptControlPanel.RUN);

    private JMenuItem m_breakItem = new JMenuItem(InterruptControlPanel.BREAK);

    private JMenuItem m_nextItem = new JMenuItem(InterruptControlPanel.NEXT);

    private JMenuItem m_finishItem = new JMenuItem(
            InterruptControlPanel.FINISH);

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InterruptibleNodeView.class);

    /**
     * Constructs an instance of the InterruptibleNodeView with the underlying
     * InterruptibleNodeModel and a title.
     * 
     * @param model - the underlying InterruptibleNodeModel.
     */
    public InterruptibleNodeView(final InterruptibleNodeModel model) {
        super(model);
        setShowNODATALabel(false);
        // The interrupt menu
        // add the menu entry to the menu bar
        super.getJMenuBar().add(createInterruptMenu());
        // TODO: this is still a hack!
//         setComponent(getControlPanel());
    }

    /**
     * Creates an instance of the interruptible node view with the control 
     * elements and a specific additional panel.
     * @param model - the underlying interruptible model.
     * @param innerView - an additional view of the model.
     */
    public InterruptibleNodeView(final InterruptibleNodeModel model,
            final JPanel innerView) {
        super(model);

        setShowNODATALabel(false);

        // add the menu
        getJMenuBar().add(createInterruptMenu());

        // create the control panel
        m_controlPanel = new InterruptControlPanel();
        m_controlPanel.getRunButton().addActionListener(this);
        m_controlPanel.getBreakButton().addActionListener(this);
        m_controlPanel.getNextStepButton().addActionListener(this);
        m_controlPanel.getFinishButton().addActionListener(this);
        m_controlPanel.getDelaySlider().addMouseListener(
                new InterruptMouseAdapter());

        // now create the whole content in a panel
        JPanel composite = new JPanel();
        composite.setLayout(new BorderLayout());

        // add the control panel
        composite.add(m_controlPanel, BorderLayout.NORTH);
        // and now the nodeviews panel
        composite.add(innerView);
    }

    private JMenu createInterruptMenu() {
        JMenu menu = new JMenu("Interrupt");
        // the run entry
        m_runItem.addActionListener(this);
        menu.add(m_runItem);

        // the break entry
        m_breakItem.addActionListener(this);
        menu.add(m_breakItem);

        m_nextItem.addActionListener(this);
        menu.add(m_nextItem);

        // the finish entry
        m_finishItem.addActionListener(this);
        menu.add(m_finishItem);

        refreshInterruptMenu();

        return menu;
    }

    /**
     * Here the control of the InterruptibleNodeModel is done. Either from the
     * menu or the buttons coming events to control the InterruptibleNodeModel
     * are processed here. Basically these events are "run", "break" and
     * "finish".
     * 
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals(InterruptControlPanel.RUN)) {
            ((InterruptibleNodeModel)getNodeModel()).run();
        } else if (e.getActionCommand().equals(InterruptControlPanel.BREAK)) {
            ((InterruptibleNodeModel)getNodeModel()).pause();
        } else if (e.getActionCommand().equals(InterruptControlPanel.NEXT)) {
            ((InterruptibleNodeModel)getNodeModel()).next(1);
        } else if (e.getActionCommand().equals(InterruptControlPanel.FINISH)) {
            ((InterruptibleNodeModel)getNodeModel()).finish();
        }
        refreshInterruptMenu();
    }

    /**
     * This method returns the control panel, which provides control elements
     * over the underlying model. It also realizes a lazy initialization of the
     * control panel.
     * 
     * @return - the controlPanel which itself provides getters to its
     *         components.
     */
    public InterruptControlPanel getControlPanel() {
        if (m_controlPanel == null) {
            m_controlPanel = new InterruptControlPanel();
            m_controlPanel.getRunButton().addActionListener(this);
            m_controlPanel.getBreakButton().addActionListener(this);
            m_controlPanel.getNextStepButton().addActionListener(this);
            m_controlPanel.getFinishButton().addActionListener(this);
            m_controlPanel.getDelaySlider().addMouseListener(
                    new InterruptMouseAdapter());
        }
        return m_controlPanel;
    }

    /**
     * Refreshes the enabled status of the control elements depending on the
     * status of the underlying model. It makes no sense to have an enabled
     * "Break"-Button, when the status is paused, and so on. Call it whenever
     * the status of the underlying method changes without the influence of the
     * control elements in order to set their right status again.
     * 
     */
    public void refreshInterruptMenu() {
        InterruptibleNodeModel model = (InterruptibleNodeModel)getNodeModel();
        m_runItem.setEnabled(model.isPaused() && !model.isFinished());
        m_breakItem.setEnabled(!model.isPaused() && !model.isFinished());
        m_nextItem.setEnabled(model.isPaused() && !model.isFinished());
        m_finishItem.setEnabled(!model.isFinished());

        getControlPanel().getRunButton().setEnabled(
                model.isPaused() && !model.isFinished());
        getControlPanel().getBreakButton().setEnabled(
                !model.isPaused() && !model.isFinished());
        getControlPanel().getNextStepButton().setEnabled(
                model.isPaused() && !model.isFinished());
        getControlPanel().getFinishButton().setEnabled(!model.isFinished());
        
        getControlPanel().setCurrentIteration(model.getNumberOfIterations());
    }

    /**
     * Forces the model to pause and then to finish. Since it makes no sense to
     * let the model run without watching at it.
     * 
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    public void onClose() {
        ((InterruptibleNodeModel)getNodeModel()).finish();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        refreshInterruptMenu();   
    }

    /**
     * Guarantees that the control panel is always added at the very top of the
     * view. The passed component will be set below the control panel.
     * 
     * @param toBeSet the panel to be set below the control panel, mind that
     *            all view components necessary for the underlying model have to
     *            be packed in one component
     *            
     * @see org.knime.core.node.NodeView#setComponent(java.awt.Component)
     */
    public void setEmbeddedComponent(final Component toBeSet) {
        JPanel packPanel = new JPanel();
        packPanel.setLayout(new BorderLayout());
        packPanel.add(getControlPanel(), BorderLayout.NORTH);
        packPanel.add(toBeSet, BorderLayout.CENTER);
        super.setComponent(packPanel);
    }

    /**
     * The <code>updateModel(Object model)</code> method is invoked whenever
     * the <code>NodeModel.notifyViews(Object)</code> method was called and
     * the <code>NodeView.modelChanged()</code> method is only called when
     * execution is finished but is the default method to implement the
     * visualization of the NodeModel. Therefore this method simply invokes the
     * <code>NodeView.modelChanged()</code> method.
     * 
     * {@inheritDoc}
     */
    @Override
    public void updateModel(final Object model) {
        refreshInterruptMenu();
        modelChanged();
    }

    /**
     * Implement here all the view updating methods. This method is called
     * whenever the underlying model triggers a refresh (that is every delay-th
     * iteration).
     * 
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    public abstract void modelChanged();

    private class InterruptMouseAdapter extends MouseAdapter {
        /**
         * Here the slider to adjust the delay is read and passed to the
         * underlying model.
         * 
         * @see java.awt.event.MouseListener#mouseReleased(MouseEvent)
         */
        @Override
        public void mouseReleased(final MouseEvent arg0) {
            LOGGER.debug("Mouse released from: " + arg0.getSource());
            getControlPanel().getDelaySlider().setToolTipText(
                    "" + getControlPanel().getDelaySlider().getValue());
            ((InterruptibleNodeModel)getNodeModel()).setDelay(getControlPanel()
                    .getDelaySlider().getValue());
        }
    }

}

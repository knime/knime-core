/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Node view base class which implements the basic and common window properties.
 * The part specific to the special purpose node view must be implemented in the
 * derived class and must take place in a <code>Panel</code>. This panel is
 * registered in this base class (method <code>#setComponent(Component)</code>)
 * and will be displayed in the JFrame provided and handled by this class.
 * 
 * @see JFrame
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeView {

    /**
     * Holds the underlying <code>NodeModel</code>.
     */
    private final NodeModel m_nodeModel;

    /**
     * Default background color.
     */
    public static final Color COLOR_BACKGROUND = Color.LIGHT_GRAY;

    /**
     * Initial width of the dummy component during construction.
     */
    private static final int INIT_COMP_WIDTH = 300;

    /**
     * Initial height of the dummy component during construction.
     */
    private static final int INIT_COMP_HEIGTH = 200;

    /**
     * Underlying frame, not visible from the outside.
     */
    private final JFrame m_frame;

    /**
     * Component in the center of this frame, set by
     * <code>#setComponent(Component)</code>.
     */
    private Component m_comp;

    /**
     * Component that is shown when no data is available (not connected, e.g.)
     * and the view is open.
     */
    private Component m_noDataComp;

    /**
     * References either to <code>m_comp</code> or <code>m_noDataComp</code>
     * depending on which is currently shown.
     */
    private Component m_activeComp;

    /**
     * If the view is opened the first time, it will be centered and packed. All
     * the following openings do not have an impact on these properties.
     */
    private boolean m_wasOpened = false;

    /**
     * Creates a new view with the given title (<code>#getViewName()</code>),
     * a menu bar, and the panel (<code>#getComponent()</code>) in the
     * center. The default title is <i>View - </i>, and the default close
     * operation <code>JFrame.DISPOSE_ON_CLOSE</code>.
     * 
     * @param nodeModel The underlying node model.
     * @param title The title of this frame.
     * @throws NullPointerException If the <code>nodeModel</code> is null.
     * @see #setComponent(Component)
     * @see #onClose()
     */
    protected NodeView(final NodeModel nodeModel, final String title) {
        if (nodeModel == null) {
            throw new NullPointerException();
        }

        // store reference to the node model
        m_nodeModel = nodeModel;

        // init frame
        m_frame = new JFrame();
        setViewName(title);
        if (KNIMEConstants.KNIME16X16 != null) {
            m_frame.setIconImage(KNIMEConstants.KNIME16X16.getImage());
        }
        m_frame.setBackground(COLOR_BACKGROUND);
        m_frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        m_frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(final WindowEvent e) {
                onClose();
            }
        });

        // creates menu item to close this view
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        JMenuItem item = new JMenuItem("Close");
        item.setMnemonic('C');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                closeView();
            }
        });
        menu.add(item);
        menuBar.add(menu);
        m_frame.setJMenuBar(menuBar);

        // set properties of the content pane
        Container cont = m_frame.getContentPane();
        cont.setLayout(new BorderLayout());
        cont.setBackground(COLOR_BACKGROUND);

        // set a dummy component to get the default size
        setShowNODATALabel(true);
        setComponent(m_noDataComp);

        // after view has been created: register the view with the model
        m_nodeModel.registerView(this);

    } // NodeView(NodeModel,String)

    /**
     * Get reference to underlying <code>NodeModel</code>. Access this if
     * access to your model is needes and cast it if necessary. Alternatively,
     * you can also override this method in your derived node view and do the
     * cast implicitly, for instance:
     * 
     * <pre>
     * protected FooNodeModel getNodeModel() {
     *     return (FooNodeModel)super.getNodeModel();
     * }
     * </pre>
     * 
     * @return NodeModel reference.
     */
    protected NodeModel getNodeModel() {
        assert m_nodeModel != null;
        return m_nodeModel;
    }

    /**
     * Sets the property if the "no data" label is shown when the underlying
     * node is not executed but the view is shown (replaces whatever has been
     * set by <code>#setComponent(Component)</code>. Once the node is
     * executed the user tab is shown again.
     * 
     * @param showIt <code>true</code> for replace the current view,
     *            <code>false</code> always show the real view.
     */
    protected final void setShowNODATALabel(final boolean showIt) {
        m_noDataComp = (showIt ? createNoDataComp() : null);
    }

    /**
     * Called from the model that something has changed. It internally sets the
     * proper component depending on the node's execution state and if the no
     * data label is set. This method will invoke the abstract
     * <code>#modelChanged()</code> method.
     */
    final void callModelChanged() {
        if (!m_nodeModel.isExecuted() && m_noDataComp != null) {
            setComponentIntern(m_noDataComp);
        } else {
            setComponentIntern(m_comp);
        }
        try {
            modelChanged();
        } catch (NullPointerException npe) {
            throw new IllegalStateException(
                    "Implementation error of NodeModel.modelChanged(). "
                    + "NullPointerException during notification of a changed "
                            + "model. Reason: "
                            + npe.getMessage());
        } catch (Exception e) {
            throw new IllegalStateException("Error during notification "
                    + "of a changed model (in NodeModel.modelChange). Reason: "
                    + e.getMessage());
        }

    }

    /**
     * Method is invoked when the underlying <code>NodeModel</code> has
     * changed. Also the HiLightHandler may be changed, as well as the
     * <code>NodeModel</code> content may be not available.
     */
    protected abstract void modelChanged();

    /**
     * This method is supposed to be overridden by views that want to receive
     * events from their assigned models via the
     * <code>NodeModel#notifyViews(Object)</code> method. Can be used to
     * iteratively update the view during execute.
     * 
     * @param arg The argument can be everything.
     */
    protected void updateModel(final Object arg) {
        // dummy statement to get rid of 'parameter not used' warning.
        assert arg == arg;
    }

    /**
     * Invoked when the window has been closed. Unregister
     * <code>HiLiteListeners</code>. Dispose internal members.
     */
    protected abstract void onClose();

    /**
     * Invoked when the window has been opened. Register property listeners.
     */
    protected abstract void onOpen();

    /**
     * Returns menu bar of this frame.
     * 
     * @return menu bar.
     */
    protected final JMenuBar getJMenuBar() {
        return m_frame.getJMenuBar();
    }

    /**
     * Initializes the view before opening.
     */
    private void preOpenView() {
        m_nodeModel.registerView(this);
        callModelChanged();
        if (!m_wasOpened) { // if the view was already visible
            m_wasOpened = true;
            if (m_comp != null) {
                m_comp.invalidate();
                m_comp.repaint();
            }
            m_frame.pack();
            setLocation();
        }
    }

    /**
     * Initializes all view components and returns the view's content pane.
     * 
     * @return The view's content pane.
     */
    public final Component openViewComponent() {
        // init
        preOpenView();
        // since the frame is not opened, we must call this by hand
        onOpen();
        // return content pane
        return m_frame.getContentPane();
    }

    /**
     * Opens the view.
     * 
     * @see #onOpen
     */
    final void openView() {
        // init
        preOpenView();
        // inform derived class
        onOpen();
        // show frame
        m_frame.setVisible(true); // triggers WindowEvent 'Opened' which
        // brings the frame to front
        m_frame.toFront();
    }

    /**
     * Sets this frame in the center of the screen observing the current screen
     * size.
     */
    private void setLocation() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = m_frame.getSize();
        m_frame.setBounds(Math.max(0, (screenSize.width - size.width) / 2),
                Math.max(0, (screenSize.height - size.height) / 2), Math.min(
                        screenSize.width, size.width), Math.min(
                        screenSize.height, size.height));
    }

    /**
     * @return Checks whether the view is open or not.
     */
    protected final boolean isOpen() {
        return m_frame.isVisible();
    }

    /**
     * Called through the menu item 'File->Close' and from the node if deleted.
     */
    final void closeView() {
        m_frame.setVisible(false);
        m_nodeModel.unregisterView(this);
        m_frame.dispose(); // triggers the WindowClosed action event!
    }

    /**
     * Set a new name for this view. The title is updated to <i>View - &lt;
     * <code>newName</code> &gt; </i>. If <code>newName</code> is
     * <code>null</code> the new title is <i>View - no title</i>.
     * 
     * @param newName new title and name to be set
     */
    protected final void setViewName(final String newName) {
        m_frame.setName(newName);
        if (newName == null) {
            m_frame.setTitle("View - no title");
        } else {
            m_frame.setTitle("View - " + newName);
        }
    }

    /**
     * Returns the view name as set by <code>#setViewName(String)</code> or
     * <code>null</code> if that hasn't happen yet.
     * 
     * @return The view's name.
     * @see JFrame#setName(String)
     */
    public final String getViewName() {
        return m_frame.getName();
    }

    /**
     * Returns the underlying content pane's panel placed at the center of the
     * view.
     * 
     * @return panel of the view's center area.
     */
    protected final Component getComponent() {
        return m_comp;
    }

    /**
     * Sets the panel of the view's content pane center area. Register your
     * <code>Component</code> that implements the functionality of the derived
     * class with this function. The foreground and background colors of your
     * panel are set to the default colors defined in this class.
     * 
     * @param comp Component to set in the center of the view.
     */
    protected void setComponent(final Component comp) {
        if (!m_nodeModel.isExecuted() && m_noDataComp != null) {
            setComponentIntern(m_noDataComp);
        } else {
            setComponentIntern(comp);
        }
        m_comp = comp;
    }

    /**
     * Helper method that internally sets the current component; it does not
     * update m_comp (which setComponent does).
     * 
     * @param comp The new component to show (might be m_noDataComp)
     */
    private void setComponentIntern(final Component comp) {
        if (m_activeComp == comp) {
            return;
        }
        Container cont = m_frame.getContentPane();
        if (m_activeComp != null) {
            cont.remove(m_activeComp);
        }
        m_activeComp = comp;
        comp.setBackground(COLOR_BACKGROUND);
        cont.add(m_activeComp, BorderLayout.CENTER);
        m_frame.invalidate();
        m_frame.validate();
        m_frame.repaint();
    }

    /**
     * Creates the label that is shown when no node is not connected or not
     * executed.
     * 
     * @return Default "no label" component.
     */
    private Component createNoDataComp() {
        JLabel noData = new JLabel("<html><center>No data in<br>"
                + getViewName() + "</center></html>", SwingConstants.CENTER);
        noData
                .setPreferredSize(new Dimension(INIT_COMP_WIDTH,
                        INIT_COMP_HEIGTH));
        return noData;
    }

}

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
package org.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.knime.core.node.util.ViewUtils;

/**
 * Node view base class which implements the basic and common window properties.
 * The part specific to the special purpose node view must be implemented in the
 * derived class and must take place in a <code>Panel</code>. This panel is
 * registered in this base class (method <code>#setComponent(Component)</code>)
 * and will be displayed in the {@link JFrame} provided and handled by this
 * class.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @param <T> the implementation of the {@link NodeModel} this node view
 *          is based on
 */
public abstract class NodeView<T extends NodeModel> 
        implements NodeModelWarningListener {
    
    /**
     * The node logger for this class; do not make static to make sure the right
     * class name is printed in messages.
     */
    private final NodeLogger m_logger;

    /**
     * Holds the underlying <code>NodeModel</code> of type T.
     */
    private final T m_nodeModel;

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
     * Remembers the first time the actual component was set. The reason is the
     * resizing, the first time the proper component (not the "no data"
     * component) is set. This resizing should only occur the first time.
     */
    private boolean m_componentSet = false;

    /**
     * Determines if this view is always on top. Useful if special views should
     * stay on top all the time
     */
    private boolean m_alwaysOnTop = false;

    private final WindowListener m_windowListener = new WindowAdapter() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void windowClosing(final WindowEvent e) {
            // triggered when user clicks [x] to close window
            closeView();
        }
    };
    
    private final JLabel m_warningLabel;

    /**
     * This class sends property events when the status changes.
     */
    public static final String PROP_CHANGE_CLOSE = "nodeview_close";
    /* TODO
     * So far, the very only possible listener is the EmbeddedNodeView that is
     * informed when the view finally closes (e.g. because the node was
     * deleted). Once the member m_frame is deleted from this class, the frame
     * will also be a potential listener.
     */

    /**
     * Creates a new view with the given title (<code>#getViewName()</code>),
     * a menu bar, and the panel (<code>#getComponent()</code>) in the
     * center. The default title is <i>View - </i>, and the default close
     * operation <code>JFrame.DISPOSE_ON_CLOSE</code>.
     *
     * @param nodeModel The underlying node model.
     * @throws NullPointerException If the <code>nodeModel</code> is null.
     * @see #setComponent(Component)
     * @see #onClose()
     */
    protected NodeView(final T nodeModel) {
        if (nodeModel == null) {
            throw new NullPointerException();
        }

        // create logger
        m_logger = NodeLogger.getLogger(this.getClass());

        // store reference to the node model
        m_nodeModel = nodeModel;

        // init frame
        m_frame = new JFrame();
        if (KNIMEConstants.KNIME16X16 != null) {
            m_frame.setIconImage(KNIMEConstants.KNIME16X16.getImage());
        }
        m_frame.setBackground(COLOR_BACKGROUND);
        // DO_NOTHING sends a windowClosing to window listeners
        m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        m_frame.addWindowListener(m_windowListener);

        // creates menu item to close this view
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');

        // create always on top entry
        JMenuItem item = new JCheckBoxMenuItem("Always on top", m_alwaysOnTop);
        item.setMnemonic('T');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {

                boolean selected =
                    ((JCheckBoxMenuItem)event.getSource()).isSelected();
                m_alwaysOnTop = selected;
                m_frame.setAlwaysOnTop(m_alwaysOnTop);
            }
        });
        menu.add(item);

        item = NodeViewExport.createNewMenu(m_frame.getContentPane());
        menu.add(item);

        // create close entry
        item = new JMenuItem("Close");
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
        
        // add warning label
        m_warningLabel = new JLabel("", WARNING_ICON, SwingConstants.LEFT);
        Font font = m_warningLabel.getFont().deriveFont(Font.BOLD);
        m_warningLabel.setFont(font);
        m_warningLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        m_warningLabel.setBackground(Color.WHITE);
        m_warningLabel.setOpaque(true);
        cont.add(m_warningLabel, BorderLayout.NORTH);
    }
    
    private static final Icon WARNING_ICON = loadIcon(
                NodeView.class, "/icon/warning.png");
    
    private static Icon loadIcon(
            final Class<?> className, final String path) {
        ImageIcon icon;
        try {
            ClassLoader loader = className.getClassLoader(); 
            String packagePath = 
                className.getPackage().getName().replace('.', '/');
            String correctedPath = path;
            if (!path.startsWith("/")) {
                correctedPath = "/" + path;
            }
            icon = new ImageIcon(
                    loader.getResource(packagePath + correctedPath));
        } catch (Exception e) {
            NodeLogger.getLogger(NodeView.class).debug(
                    "Unable to load icon at path " + path, e);
            icon = null;
        }
        return icon;
    }  

    /**
     * Get reference to underlying <code>NodeModel</code>. Access this if
     * access to your model is needs and cast it if necessary. Alternatively,
     * you can also override this method in your derived node view and do the
     * cast implicitly, for instance:
     *
     * <pre>
     * protected FooNodeModel getNodeModel() {
     *     return (FooNodeModel)super.getNodeModel();
     * }
     * </pre>
     *
     * @return GernericNodeModel reference.
     */
    protected T getNodeModel() {
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
        synchronized (m_nodeModel) {
            try {
                // CALL abstract model changed
                modelChanged();
                setComponent(m_comp);
            } catch (NullPointerException npe) {
                m_logger.coding("NodeView.modelChanged() causes "
                       + "NullPointerException during notification of a "
                       + "changed model, reason: " + npe.getMessage(), npe);
            } catch (Throwable t) {
                m_logger.error("NodeView.modelChanged() causes an error "
                       + "during notification of a changed model, reason: "
                       + t.getMessage(), t);
            } finally {
                // repaint and pack if the view has not been opened yet or
                // the underlying view component was added
                // ensured to happen in the EDT thread
                relayoutFrame(false);
            }
        }
    }

    /**
     * Method is invoked when the underlying <code>NodeModel</code> has
     * changed. Also the HiLightHandler have changed. Note, the
     * <code>NodeModel</code> content may be not available. Be sure to
     * modify GUI components in the EventDispatchThread only.
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
     * Invoked when the window is about to be closed. Unregister
     * <code>HiLiteListeners</code>. Dispose internal members. <br />
     * This method is the first to be called on a close request (right after
     * the view is unregistered from the {@link NodeModel}
     */
    protected abstract void onClose();

    /**
     * Invoked when the window has been opened. Register property listeners.
     * <br />
     * This method is called last on view construction - right before the
     * components are made visible. It is not called on re-opening.
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
     * Initializes the view before opening. Packs and sets the location. Call
     * this only once per view instance.
     */
    private void callOpenView() {
        try {
            onOpen();
        } catch (Throwable t) {
            m_logger.error("NodeView.onOpen() causes an error "
                    + "on opening node view, reason: " + t.getMessage(), t);
        }
        m_nodeModel.registerView(this);
        callModelChanged();

        warningChanged(getNodeModel().getWarningMessage());
        
        if (m_comp != null) {
            m_comp.invalidate();
            m_comp.repaint();
        }
        m_frame.pack();
        setLocation();
    }

    /**
     * Initializes all view components and returns the view's content pane. If
     * you derive this class, <strong>do not</strong> call this method. It's
     * being used by the framework (if views are shown within a JFrame) or by
     * eclipse (if available, i.e. when views are embedded in eclipse)
     *
     * @return The view's content pane.
     */
    public final Component openViewComponent() {
        // init
        callOpenView();
        // return content pane
        return m_frame.getContentPane();
    }

    /**
     * Creates and opens a new view.
     * @param viewTitle the tile for this view
     * @return a {@link JFrame} with an initialized {@link NodeView}
     */
    public final JFrame createFrame(final String viewTitle) {
        final String name;
        if (viewTitle == null) {
            name = "View \"no title\"";
        } else {
            name = viewTitle;
        }
        m_frame.setName(name);
        setTitle(name);
        openView();
        return m_frame;
    }

    /**
     * Opens the view.
     *
     * @see #onOpen
     */
    private void openView() {
        // init
        callOpenView();
        // show frame, make sure to do this in EDT (GUI related task)
        Runnable runner = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                m_frame.setVisible(true);
                m_frame.toFront();
            }
        };
        ViewUtils.runOrInvokeLaterInEDT(runner);
    }

    /**
     * Called by the node when it is deleted or by the "close" button. Disposes
     * the frame.
     * <p>
     * Calls the onClose method and unregisters this view from the model. If you
     * derive this class, <strong>do not</strong> call this method. It's being
     * used by the framework (if views are shown within a JFrame) or by eclipse
     * (if available, i.e. when views are embedded in eclipse).
     */
    public final void closeView() {
        m_nodeModel.unregisterView(this);
        try {
            onClose();
        } catch (Throwable t) {
            m_logger.error("NodeView.onClose() causes an error "
                    + "during closing node view, reason: " + t.getMessage(), t);
        }
        m_frame.getContentPane().firePropertyChange(PROP_CHANGE_CLOSE, 0, 1);
        m_activeComp = null;
        m_comp = null;
        Runnable runner = new Runnable() {
            public void run() {
                m_frame.setVisible(false);
                m_frame.removeWindowListener(m_windowListener);
                m_frame.getContentPane().removeAll();
                m_frame.dispose();
            }
        };
        ViewUtils.invokeAndWaitInEDT(runner);
    }

    /**
     * Sets this frame in the center of the screen observing the current screen
     * size.
     */
    private void setLocation() {
        Runnable runner = new Runnable() {
            public void run() {

                Dimension screenSize =
                        Toolkit.getDefaultToolkit().getScreenSize();
                Dimension size = m_frame.getSize();
                m_frame.setBounds(Math.max(0,
                        (screenSize.width - size.width) / 2), Math.max(0,
                        (screenSize.height - size.height) / 2), Math.min(
                        screenSize.width, size.width), Math.min(
                        screenSize.height, size.height));
            }
        };

        ViewUtils.invokeAndWaitInEDT(runner);
    }

    /**
     * @return Checks whether the view is open or not.
     */
    protected final boolean isOpen() {
        // broken if view is opened via openViewComponent
        return m_frame.isVisible();
    }

    /**
     * Append this suffix to the current view name.
     * If <code>suffix</code> is <code>null</code> the title does not change.
     *
     * @param suffix append this suffix to the current view name
     */
    protected final void setViewTitleSuffix(final String suffix) {
        if (suffix != null) {
            setTitle(m_frame.getName() + " " + suffix);
        }
    }

    private void setTitle(final String title) {
        final Runnable runner = new Runnable() {
            public void run() {
                m_frame.setTitle(title);
            }
        };
        ViewUtils.invokeAndWaitInEDT(runner);
    }



    /**
     * @return The current view's title.
     */
    public final String getViewTitle() {
        return m_frame.getTitle();
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
    protected final void setComponent(final Component comp) {
        Runnable runner = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
             // pack frame only when setting the component for the first time
                boolean pack = false;
                if (!m_nodeModel.hasContent() && m_noDataComp != null) {
                    setComponentIntern(m_noDataComp);
                } else {
                    setComponentIntern(comp);
                    if (!m_componentSet) {
                        pack = true;
                    }
                    m_componentSet = true;
                }
                m_comp = comp;
                relayoutFrame(pack);
            }
        };
        ViewUtils.invokeAndWaitInEDT(runner);
    }

    /**
     * Helper method that internally sets the current component; it does not
     * update m_comp (which setComponent does).
     *
     * @param cmp The new component to show (might be m_noDataComp)
     */
    private void setComponentIntern(final Component cmp) {
        if (m_activeComp == cmp || cmp == null) {
            return;
        }

        Container cont = m_frame.getContentPane();
        if (m_activeComp != null) {
            cont.remove(m_activeComp);
        }

        m_activeComp = cmp;
        cont.add(m_activeComp, BorderLayout.CENTER);
    }

    /**
     * Repaints or pack this frame depending on <code>doPack</code> flag.
     *
     * @param doPack if <code>true</code> the dialog is packed, otherwise just
     *            validated and repainted
     */
    private void relayoutFrame(final boolean doPack) {
        final Runnable run = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                if (doPack) {
                    m_frame.pack();
                } else {
                    m_frame.invalidate();
                    m_frame.validate();
                    m_frame.repaint();
                }
            }
        };
        ViewUtils.invokeAndWaitInEDT(run);
    }

    /**
     * Creates the label that is shown when no node is not connected or not
     * executed.
     *
     * @return Default "no label" component.
     */
    private Component createNoDataComp() {
        JLabel noData =
            new JLabel("<html><center>No data to display</center></html>",
                    SwingConstants.CENTER);
        noData.setPreferredSize(new Dimension(INIT_COMP_WIDTH,
                INIT_COMP_HEIGTH));
        return noData;
    }
    
    /** {@inheritDoc} */
    @Override
    public void warningChanged(final String warning) {
        if (warning != null && warning.trim().length() > 0) {
            m_warningLabel.setIcon(WARNING_ICON);
            m_warningLabel.setText(warning.trim());
            m_warningLabel.setToolTipText(warning.trim());
            m_warningLabel.setPreferredSize(
                    new Dimension(m_frame.getContentPane().getWidth(), 20));
        } else {
            m_warningLabel.setIcon(null);
            m_warningLabel.setText(null);
            m_warningLabel.setToolTipText(null);
            m_warningLabel.setPreferredSize(new Dimension(0, 0));
        }
    }
}

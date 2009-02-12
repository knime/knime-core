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
 *   24.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.node.util.ViewUtils;

/**
 * Provides functionality for zooming, moving and selection and is designed to
 * be extended. For this purpose it provides means to support hiliting, creating
 * the x and y {@link org.knime.base.node.viz.plotter.Axis} and get the mapped
 * values for a given {@link org.knime.core.data.DataCell}, whose values lies
 * within the domain used to create the axes.
 * <p>
 * The plotter consists of a drawing pane and a properties panel which can be
 * accessed. The size of each is adapted automatically. The plotter relies on an
 * object implementing the {@link org.knime.base.node.viz.plotter.DataProvider}
 * to access the data to visualize.
 * <p>
 * The two most important methods are
 * {@link org.knime.base.node.viz.plotter.AbstractPlotter#updatePaintModel()}
 * and {@link org.knime.base.node.viz.plotter.AbstractPlotter#updateSize()}.
 * The former is called whenever the data to visualize might have changed, the
 * latter is called when the size of the plotter has changed. In some cases this
 * means no difference since the mapping of the data to the screen coordinates
 * has to be done anyway, but if there are other things specific to the data to
 * be visualized the
 * {@link org.knime.base.node.viz.plotter.AbstractPlotter#updatePaintModel()} is
 * the correct place to access and display it.
 *
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractPlotter extends JPanel implements HiLiteListener,
        ComponentListener {

    // private static final NodeLogger LOGGER = NodeLogger
    // .getLogger(AbstractPlotter.class);

    /** The default zoom factor. */
    public static final double DEFAULT_ZOOM_FACTOR = 1.2;

    /** Constant for "show hilited only" menu entry. */
    public static final String HIDE_UNHILITED = "Show hilited only";

    /** Constant for the menu entry "show all". */
    public static final String SHOW_ALL = "Show all";

    /** Constant for "Fade unhilited" menu entry. */
    public static final String FADE_UNHILITED = "Fade unhilited";

    /** Constant for the show/hide menu title. */
    public static final String SHOW_HIDE = "Show/Hide";

    private static final int DRAG_TOLERANCE = 5;

    /** The drawing pane. */
    private final AbstractDrawingPane m_drawingPane;

    /** The scroll pane the drawing pane is embedded in. */
    private final Plotter2DScrollPane m_scroller;

    /** Thew properties panel. */
    private final AbstractPlotterProperties m_properties;

    private boolean m_isDragged;

    /** The width of the drawing pane! Not of this component. */
    private int m_width;

    /** The height of the drawing pane! Not of this component. */
    private int m_height;

    /** The x axis, might be null. */
    private Axis m_xAxis;

    /** The y axis, might be null. */
    private Axis m_yAxis;

    /** The mouse listener depending on the current mode. */
    private PlotterMouseListener m_currMouseListener;

    /** The provider for the data to visualize. */
    private DataProvider m_dataProvider;

    /** The hilite handler. */
    private HiLiteHandler m_hiliteHandler;

    /**
     * Flag, whether the axis range should be preserved(true) or adapted to
     * newly added values (false).
     */
    private boolean m_preserve = true;

    // default value is the first index: 0
    private int m_dataArrayIdx = 0;

    /**
     * Creates a new plotter with a drawing pane and a properties panel. The
     * listener to the default properties (selection, zooming, moving, fit to
     * screen) are registered.
     *
     * @param drawingPane the drawing pane
     * @param properties the properties panel
     */
    public AbstractPlotter(final AbstractDrawingPane drawingPane,
            final AbstractPlotterProperties properties) {
        m_drawingPane = drawingPane;
        m_currMouseListener = new SelectionMouseListener();
        m_drawingPane.addMouseListener(m_currMouseListener);
        m_drawingPane.addMouseMotionListener(m_currMouseListener);
        m_drawingPane.setCursor(m_currMouseListener.getCursor());
        m_scroller = new Plotter2DScrollPane(m_drawingPane);
        m_scroller.setLayout(new Plotter2DScrollPaneLayout());

        m_properties = properties;
        m_properties.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                m_properties.getPreferredSize().height));
        m_properties.getMouseSelectionBox().addItem(m_currMouseListener);
        m_properties.getMouseSelectionBox().addItem(new ZoomMouseListener());
        m_properties.getMouseSelectionBox().addItem(new MovingMouseListener());
        m_properties.getMouseSelectionBox()
                .setSelectedItem(m_currMouseListener);
        m_properties.getMouseSelectionBox().addItemListener(new ItemListener() {
            /**
             * Mouse mode sets the referring mouse listener.
             *
             * @see java.awt.event.ItemListener#itemStateChanged(
             *      java.awt.event.ItemEvent)
             */
            public void itemStateChanged(final ItemEvent e) {
                // clean up
                m_drawingPane.removeMouseListener(m_currMouseListener);
                m_drawingPane.removeMouseMotionListener(m_currMouseListener);
                m_drawingPane.setMouseDown(false);
                m_isDragged = false;
                m_currMouseListener =
                        (PlotterMouseListener)m_properties
                                .getMouseSelectionBox().getSelectedItem();
                m_drawingPane.setCursor(m_currMouseListener.getCursor());
                m_drawingPane.addMouseListener(m_currMouseListener);
                m_drawingPane.addMouseMotionListener(m_currMouseListener);
            }

        });
        m_properties.getFitToScreenButton().addActionListener(
                new ActionListener() {

                    /**
                     * Fit to screen.
                     *
                     * @see java.awt.event.ActionListener#actionPerformed(
                     *      java.awt.event.ActionEvent)
                     */
                    public void actionPerformed(final ActionEvent e) {
                        fitToScreen();
                    }

                });
        final ActionListener okListener = new ActionListener() {
            /**
             * Background color chooser.
             *
             * @see java.awt.event.ActionListener#actionPerformed(
             *      java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent arg0) {
                Color newBackground = m_properties.getColorChooser().getColor();
                if (newBackground != null) {
                    getDrawingPane().setBackground(newBackground);
                }

            }
        };
        m_properties.getChooseBackgroundButton().addActionListener(
                new ActionListener() {

                    /**
                     * {@inheritDoc}
                     */
                    public void actionPerformed(final ActionEvent arg0) {
                        JDialog dialog =
                                JColorChooser.createDialog(m_properties,
                                        "Select background color", true,
                                        m_properties.getColorChooser(),
                                        okListener, null);
                        dialog.setVisible(true);
                    }

                });
        m_properties.getAntialiasButton().addChangeListener(
                new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        setAntialiasing(m_properties.getAntialiasButton()
                                .isSelected());
                        getDrawingPane().repaint();
                    }

        });
        m_width = 400;
        m_height = 400;
        m_drawingPane.setPreferredSize(new Dimension(m_width, m_height));
        m_scroller.getViewport().setPreferredSize(
                new Dimension(
                        m_drawingPane.getPreferredSize().width
                                + m_scroller.getVerticalScrollBar()
                                        .getPreferredSize().width,
                        m_drawingPane.getPreferredSize().height
                                + m_scroller.getHorizontalScrollBar()
                                        .getPreferredSize().height));
        m_drawingPane.setBackground(Color.white);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
//                m_scroller, m_properties);
//        add(split);
        add(m_scroller);
        add(m_properties);
        addComponentListener(this);

        m_hiliteHandler = new HiLiteHandler();


    }

    /*----------- viewing methods ---------*/

    /**
     *
     * @return the actual dimension of the drawing pane.
     */
    public Dimension getDrawingPaneDimension() {
        return new Dimension(m_width, m_height);
    }

    /**
     * Zooms the content of the drawing pane to the dragged rectangle.
     *
     * @param draggedRectangle the dragged rectangle
     */
    protected void zoomByWindow(final Rectangle draggedRectangle) {
        Rectangle dragRect = draggedRectangle;
        if (dragRect != null && dragRect.width > DRAG_TOLERANCE
                && dragRect.height > DRAG_TOLERANCE) {
            int zoomWindowWidth = Math.abs(dragRect.width);
            int zoomWindowHeight = Math.abs(dragRect.height);
            JViewport viewPort = m_scroller.getViewport();
            float widthZoomFactor =
                    ((float)viewPort.getViewRect().width)
                            / ((float)zoomWindowWidth);
            float heightZoomFactor =
                    ((float)viewPort.getViewRect().height)
                            / ((float)zoomWindowHeight);
            // adjust the draw pane ranges
            m_width = Math.round(m_width * widthZoomFactor);
            m_height = Math.round(m_height * heightZoomFactor);
            m_drawingPane.setPreferredSize(new Dimension(m_width, m_height));
            m_drawingPane.revalidate();
            updateAxisLength();
            updateSize();
            // calculate the upper left corner corespondence in the adapted draw
            // pane. this is the original upper left point multiplied by the
            // zoom factor

            // adapt by zoom factors
            int upperLeftX = Math.round(dragRect.x * widthZoomFactor);
            int upperLeftY = Math.round(dragRect.y * heightZoomFactor);

            // set the viewport to this coordinates
            Rectangle recToVisible =
                    new Rectangle(upperLeftX, upperLeftY, viewPort
                            .getVisibleRect().width,
                            viewPort.getVisibleRect().height);

            m_drawingPane.scrollRectToVisible(recToVisible);
            m_scroller.revalidate();
            m_drawingPane.repaint();
        }
    }

    /**
     * Zooms the content of the drawing pane with the point clicked in center.
     *
     * @param clicked the point clicked becomes center.
     */
    protected void zoomByClick(final Point clicked) {
        // adjust the draw pane ranges
        m_width = (int)Math.round(m_width * DEFAULT_ZOOM_FACTOR);
        m_height = (int)Math.round(m_height * DEFAULT_ZOOM_FACTOR);
        m_drawingPane.setPreferredSize(new Dimension(m_width, m_height));
        m_drawingPane.invalidate();
        m_drawingPane.revalidate();

        updateAxisLength();
        updateSize();

        Rectangle visibleRect = m_drawingPane.getVisibleRect();
        int vWidth = visibleRect.width;
        int vHeight = visibleRect.height;
        int prefX = ((int)(clicked.x * DEFAULT_ZOOM_FACTOR)) - (vWidth / 2);
        int prefY = ((int)(clicked.y * DEFAULT_ZOOM_FACTOR)) - (vHeight / 2);
        Rectangle recToVisible =
                new Rectangle(prefX, prefY, visibleRect.width,
                        visibleRect.height);

        m_drawingPane.scrollRectToVisible(recToVisible);
        m_scroller.revalidate();
        m_drawingPane.repaint();
    }

    /**
     * Fits to screen, that is it resizes the drawing pane to fit into the
     * plotters dimension.
     */
    public final void fitToScreen() {
        ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                m_width = m_scroller.getViewport().getWidth();
                m_height = m_scroller.getViewport().getHeight();
                Dimension newDim = new Dimension(m_width, m_height);
                m_drawingPane.setPreferredSize(newDim);
                // updatePaintModel();
                updateAxisLength();
                updateSize();
                m_scroller.getViewport().revalidate();
                m_drawingPane.repaint();
            }
        });
    }

    /* ---------- relevant mouse methods ----------- */

    /**
     * Calls the {@link #fillPopupMenu(JPopupMenu)} and then shows the popup
     * menu at the position of the mouse click.
     */
    private void showPopupMenu(final Point at) {
        m_drawingPane.setMouseDown(false);
        m_isDragged = false;
        JPopupMenu menu = new JPopupMenu();
        fillPopupMenu(menu);
        if (menu.isEnabled()) {
            menu.show(m_drawingPane, (int)at.getX(), (int)at.getY());
        }
        return;
    }

    /* ---------------- relevant component listener methods ------- */

    /**
     * Resizes the axes and calls {@link #updateSize()}, the drawing pane is
     * adapted to the new space only if the size is increased.
     *
     * @param e the resize event
     *
     * {@inheritDoc}
     */
    public final void componentResized(final ComponentEvent e) {
        if (m_scroller.getViewport().getSize().width > m_width) {
            m_width = m_scroller.getViewport().getSize().width;
        }
        if (m_scroller.getViewport().getSize().height > m_height) {
            m_height = m_scroller.getViewport().getSize().height;
        }
        Dimension newDim = new Dimension(m_width, m_height);
        m_drawingPane.setPreferredSize(newDim);
        updateAxisLength();
        updateSize();
        m_scroller.getViewport().revalidate();
        m_drawingPane.repaint();
    }

    /**
     * Sets the height for the drawing pane.
     *
     * @param height the height to set for the plotter
     */
    public void setHeight(final int height) {
        m_height = height;
    }

    /**
     * Sets the size of the axes to the dimension of the drawing pane.
     *
     */
    public void updateAxisLength() {
        if (m_xAxis != null) {
            m_xAxis.setPreferredLength(
                    getDrawingPane().getPreferredSize().width);
            m_xAxis.repaint();
        }
        if (m_yAxis != null) {
            m_yAxis.setPreferredLength(
                    getDrawingPane().getPreferredSize().height);
            m_yAxis.repaint();
        }
        m_scroller.revalidate();

    }

    /*---------- accessors ---------*/

    /**
     * If one of the default mouse modes (selection, zooming, moving) should not
     * be available, use this method to remove it. It will then not appear in
     * the mouse mode selection box.
     *
     * @param listener the class of the listener to be removed.
     */
    public void removeMouseListener(
            final Class<? extends PlotterMouseListener> listener) {
        for (int i = 0; i < m_properties.getMouseSelectionBox().getItemCount(); i++) {
            Object item = m_properties.getMouseSelectionBox().getItemAt(i);
            if (item.getClass().equals(listener)) {
                m_properties.getMouseSelectionBox().removeItem(item);
                break;
            }
        }
    }

    /**
     *
     * @return the currently selected mouse listener.
     */
    public PlotterMouseListener getCurrentMouseListener() {
        return m_currMouseListener;
    }

    /**
     * If an additional mouse mode should be supported, extend the
     * {@link org.knime.base.node.viz.plotter.PlotterMouseListener} and add it.
     * It will then appear in the mouse mode selection box displayed with the
     * value of the <code>toString()</code> method.
     *
     * @param listener the listener to add.
     */
    public void addMouseListener(final PlotterMouseListener listener) {
        m_properties.getMouseSelectionBox().addItem(listener);
    }

    /**
     * Turns antialiasing on (true) or off (false).
     *
     * @param doAntialiasing true for antialiasing enabled, false otherwise.
     */
    public void setAntialiasing(final boolean doAntialiasing) {
        getDrawingPane().setAntialiasing(doAntialiasing);
    }

    /**
     * @return the x axis.
     */
    public Axis getXAxis() {
        return m_xAxis;
    }

    /**
     *
     * @param xAxis the x axis to set for the drawing pane
     */
    public void setXAxis(final Axis xAxis) {
        m_xAxis = xAxis;
        m_scroller.setColumnHeaderView(m_xAxis);
        if (m_xAxis != null) {
            m_xAxis.addChangeListener(new ChangeListener() {
                /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    updateSize();
                    repaint();
                }
            });
        }
    }

    /**
     *
     * @return the y axis.
     */
    public Axis getYAxis() {
        return m_yAxis;
    }

    /**
     *
     * @param yAxis the y axis to set for the drawing pane
     */
    public void setYAxis(final Axis yAxis) {
        m_yAxis = yAxis;
        m_scroller.setRowHeaderView(m_yAxis);

        if (m_yAxis != null) {
            m_yAxis.addChangeListener(new ChangeListener() {
               /**
                 * {@inheritDoc}
                 */
                public void stateChanged(final ChangeEvent e) {
                    updateSize();
                    repaint();
                }
            });
        }
    }

    /**
     *
     * @param provider the data provider.
     */
    public void setDataProvider(final DataProvider provider) {
        m_dataProvider = provider;
    }

    /**
     *
     * @return the data provider.
     */
    public DataProvider getDataProvider() {
        return m_dataProvider;
    }

    /**
     * @return the dataArrayIdx
     */
    public int getDataArrayIdx() {
        return m_dataArrayIdx;
    }

    /**
     * @param dataArrayIdx the dataArrayIdx to set
     */
    public void setDataArrayIdx(final int dataArrayIdx) {
        m_dataArrayIdx = dataArrayIdx;
    }

    /**
     *
     * @return the drawing pane
     */
    public AbstractDrawingPane getDrawingPane() {
        return m_drawingPane;
    }

    /**
     *
     * @return the properties panel
     */
    public AbstractPlotterProperties getProperties() {
        return m_properties;
    }

    /**
     * This action calls {@link #hiLiteSelected()}, if it is overridden take
     * care to not return <code>null</code> since it might be called also when
     * the {@link #getHiLiteMenu()} returns null. If no hilite behavior should
     * be implemented either let the {@link #hiLiteSelected()} empty
     * (recommended) or override this method by returning an empty action.
     *
     * @return the menu entry for hilite
     */
    public Action getHiliteAction() {
        Action hilite = new AbstractAction(HiLiteHandler.HILITE_SELECTED) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                hiLiteSelected();
            }
        };
        return hilite;
    }

    /**
     * This action calls {@link #unHiLiteSelected()}, if it is overridden take
     * care to not return <code>null</code> since it might be called also when
     * the {@link #getHiLiteMenu()} returns null. If no hilite behavior should
     * be implemented either let the {@link #unHiLiteSelected()} empty
     * (recommended) or override this method by returning an empty action.
     *
     * @return the menu entry for unhilite
     */
    public Action getUnhiliteAction() {
        Action unhilite = new AbstractAction(HiLiteHandler.UNHILITE_SELECTED) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                unHiLiteSelected();
            }
        };
        return unhilite;
    }

    /**
     * This action calls {@link #delegateUnHiLiteAll()}, if it is overridden
     * take care to not return <code>null</code> since it might be called also
     * when the {@link #getHiLiteMenu()} returns null. If not hilite behavior
     * should be implemented override this method by returning an empty action
     * (not recommended).
     *
     * @return the menu entry for clear hilite
     */
    public Action getClearHiliteAction() {
        Action clear = new AbstractAction(HiLiteHandler.CLEAR_HILITE) {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent e) {
                delegateUnHiLiteAll();
            }
        };
        return clear;
    }

    /**
     * Fills the popup menu with (additional) elements. In this class the
     * hilite, unhilite and clear hilite actions are added.
     *
     * @param popupMenu the popup menu to fill.
     */
    public void fillPopupMenu(final JPopupMenu popupMenu) {
        popupMenu.add(getHiliteAction());
        popupMenu.add(getUnhiliteAction());
        popupMenu.add(getClearHiliteAction());
    }

    /**
     * Returns the hilite menu displayed in the
     * {@link org.knime.core.node.NodeView}'s menu bar. In this class the
     * hilite, unhilite and clear hilite actions are added. This method is
     * called in the
     * {@link org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView}
     * if it is not <code>null</code>, i.e. if an extending plotter doesn't
     * support hiliting override this method by returning <code>null</code>.
     *
     * @return the filled menu for the {@link org.knime.core.node.NodeView}'s
     *         menu bar.
     */
    public JMenu getHiLiteMenu() {
        JMenu menu = new JMenu(HiLiteHandler.HILITE);
        menu.add(getHiliteAction());
        menu.add(getUnhiliteAction());
        menu.add(getClearHiliteAction());
        return menu;
    }

    /* --------------------- hiliting ----------------- */
    /**
     * {@inheritDoc}
     */
    public abstract void hiLite(final KeyEvent event);

    /**
     * {@inheritDoc}
     */
    public abstract void unHiLite(final KeyEvent event);

    /**
     * Is called by the menu entry unhilite selected. Should unhilite selected
     * elements.
     *
     */
    public abstract void unHiLiteSelected();

    /**
     * Is called from the menu entry hilite selected. Should hilite selected
     * elements.
     *
     */
    public abstract void hiLiteSelected();

    /**
     * Delegates the unhilite all command to the hilite handler.
     *
     */
    public void delegateUnHiLiteAll() {
        if (m_hiliteHandler != null) {
            m_hiliteHandler.fireClearHiLiteEvent();
        }
    }

    /**
     *
     * @param handler a new hilite handler
     */
    public void setHiLiteHandler(final HiLiteHandler handler) {
        if (m_hiliteHandler != null && handler != m_hiliteHandler) {
            m_hiliteHandler.removeHiLiteListener(this);
        }
        if (handler != null && handler != m_hiliteHandler) {
            handler.addHiLiteListener(this);
        }
        m_hiliteHandler = handler;
    }

    /**
     * Delegates the listener to the hilite handler.
     *
     * @param listener the listener
     */
    public void delegateAddHiLiteListener(final HiLiteListener listener) {
        if (m_hiliteHandler != null) {
            m_hiliteHandler.addHiLiteListener(listener);
        }
    }

    /**
     * Delegates to the hilite handler.
     *
     * @return the hilited keys.
     * @see org.knime.core.node.property.hilite.HiLiteHandler#getHiLitKeys()
     */
    public Set<RowKey> delegateGetHiLitKeys() {
        if (m_hiliteHandler != null) {
            return m_hiliteHandler.getHiLitKeys();
        }
        return new HashSet<RowKey>();
    }

    /**
     * Delegates to the hilite handler.
     *
     * @param ids the keys to be hilited.
     * @see org.knime.core.node.property.hilite.HiLiteHandler#fireHiLiteEvent(
     *      RowKey...)
     */
    public void delegateHiLite(final RowKey... ids) {
        if (m_hiliteHandler != null) {
            m_hiliteHandler.fireHiLiteEvent(ids);
        }
    }

    /**
     * Delegates to the hilite handler.
     *
     * @param ids the keys to be hilited
     * @see org.knime.core.node.property.hilite.HiLiteHandler#fireHiLiteEvent(
     *      java.util.Set)
     */
    public void delegateHiLite(final Set<RowKey> ids) {
        if (m_hiliteHandler != null) {
            m_hiliteHandler.fireHiLiteEvent(ids);
        }
    }

    /**
     * delegates to the hilite handler.
     *
     * @param ids the ids to be checked.
     * @return true if all passed keys are hilited
     * @see org.knime.core.node.property.hilite.HiLiteHandler#isHiLit(
     *      RowKey...)
     */
    public boolean delegateIsHiLit(final RowKey... ids) {
        if (m_hiliteHandler != null) {
            return m_hiliteHandler.isHiLit(ids);
        }
        return false;
    }

    /**
     * Delegates to the hilite handler.
     *
     * @param ids the ids to be checked.
     * @return true if all passed keys are hilited
     * @see org.knime.core.node.property.hilite.HiLiteHandler#isHiLit(
     *      RowKey...)
     */
    public boolean delegateIsHiLit(final Set<RowKey> ids) {
        RowKey[] cells = new RowKey[ids.size()];
        int i = 0;
        for (RowKey cell : ids) {
            cells[i++] = cell;
        }
        return delegateIsHiLit(cells);
    }

    /**
     * Delegates to the hilite handler.
     *
     * @see org.knime.core.node.property.hilite.HiLiteHandler
     *      #removeAllHiLiteListeners()
     */
    public void delegateRemoveAllHiLiteListeners() {
        if (m_hiliteHandler != null) {
            m_hiliteHandler.removeAllHiLiteListeners();
        }
    }

    /**
     * @param listener the listener to be removed.
     * @see org.knime.core.node.property.hilite.HiLiteHandler
     *      #removeHiLiteListener(org.knime.core.node.property.hilite.HiLiteListener)
     */
    public void delegateRemoveHiLiteListener(final HiLiteListener listener) {
        if (m_hiliteHandler != null) {
            m_hiliteHandler.removeHiLiteListener(listener);
        }
    }

    /**
     * Delegates to the hilite handler.
     * @param ids the ids to be unhilited.
     * @see org.knime.core.node.property.hilite.HiLiteHandler
     *      #fireUnHiLiteEvent(RowKey...)
     */
    public void delegateUnHiLite(final RowKey... ids) {
        if (m_hiliteHandler != null) {
            m_hiliteHandler.fireUnHiLiteEvent(ids);
        }
    }

    /**
     * Delegates to the hilite handler.
     *
     * @param ids the ids to be unhilited.
     * @see org.knime.core.node.property.hilite.HiLiteHandler#fireUnHiLiteEvent(
     *      java.util.Set)
     */
    public void delegateUnHiLite(final Set<RowKey> ids) {
        if (m_hiliteHandler != null) {
            m_hiliteHandler.fireUnHiLiteEvent(ids);
        }
    }

    /* ------------- abstract methods ------------ */

    /**
     * Implementing classes may select the elements in the selection rectangle
     * obtained from the mouse dragging in selection mode.
     *
     * @param selectionRectangle the selection rectangle from the dragged mouse
     *            in selection mode
     */
    public abstract void selectElementsIn(final Rectangle selectionRectangle);

    /**
     * Implementing classes mayxselect the elements depending on the clicked
     * position. This method is called only when the element should be selected,
     * that is, it is already determined whether the CTRL key is pressed or not.
     *
     * @param clicked the clicked point
     */
    public abstract void selectClickedElement(final Point clicked);

    /**
     * Clears current selection.
     *
     */
    public abstract void clearSelection();

    /**
     * Whenever the size of the drawing pane is changed (zooming, resizing) this
     * method is called in order to update the painting.
     *
     */
    public abstract void updateSize();

    /**
     * Do the mapping from the models data to screen coordinates here and pass
     * the visualization model to the drawing pane.
     *
     */
    public abstract void updatePaintModel();

    /**
     * Reset all local data which depends on the input data provided by the data
     * provider.
     */
    public abstract void reset();

    /**
     * Notifies the plotter that it is not needed any more and can clean up
     * all data and references it holds.
     */
    public void dispose() {
        m_dataProvider = null;
    }

    /* -------------- mouse listeners ------------------ */

    /**
     * The selection mouse listener checks the selection status, that is, if the
     * mouse was dragged, the CTRL key is pressed and so on and calls the
     * appropriate methods in the {@link AbstractPlotter}. A drawing pane may
     * be passed in order to let it listen to another drawing pane.
     *
     */
    public class SelectionMouseListener extends PlotterMouseListener {

        private final AbstractDrawingPane m_listenedPane;

        /**
         * Listens to the drawing pane of this plotter.
         */
        public SelectionMouseListener() {
            m_listenedPane = getDrawingPane();
        }

        /**
         *
         * @param pane the panel the selection listener listenes to.
         */
        public SelectionMouseListener(final AbstractDrawingPane pane) {
            m_listenedPane = pane;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            m_listenedPane.setDragStart(e.getPoint());
            // System.out.println("set mouse down: true");
            // m_drawingPane.setMouseDown(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            if (!e.isControlDown() && !e.isPopupTrigger()
                    && !SwingUtilities.isRightMouseButton(e)) {
                clearSelection();
                m_listenedPane.repaint();
            }
            if (!m_isDragged) {
                // On linux, e.isPopupTrigger never returns true, apparently
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    showPopupMenu(e.getPoint());
                    return;
                } else {
                    m_listenedPane.setMouseDown(false);
                    selectClickedElement(e.getPoint());
                    m_listenedPane.repaint();
                    return;
                }
            }
            m_listenedPane.setMouseDown(false);
            m_isDragged = false;
            m_listenedPane.setDragEnd(e.getPoint());
            selectElementsIn(m_listenedPane.getSelectionRectangle());
            m_listenedPane.repaint();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            m_listenedPane.setMouseDown(true);
            m_isDragged = true;
            m_listenedPane.setDragEnd(e.getPoint());
            // simply repaint to update the dragging rectangle
            m_listenedPane.repaint();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Cursor getCursor() {
            return new Cursor(Cursor.CROSSHAIR_CURSOR);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Selection";
        }

        /**
         *
         * @return the drawing pane this listener listens to.
         */
        public AbstractDrawingPane getInternalDrawingPane() {
            return m_listenedPane;
        }
    }

    /**
     * Realizes the zooming behaviour, when the mouse is clicked it calls the
     * {@link AbstractPlotter#zoomByClick(Point)}, when the mouse is dragged
     * the {@link AbstractPlotter#zoomByWindow(Rectangle)} is called.
     *
     * @author Fabian Dill, University of Konstanz
     */
    public class ZoomMouseListener extends PlotterMouseListener {

        private Cursor m_zoomCursor;

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            m_drawingPane.setDragStart(e.getPoint());
            m_drawingPane.setMouseDown(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            if (!m_isDragged) {
                // On linux, e.isPopupTrigger never returns true, apparently
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    showPopupMenu(e.getPoint());
                    return;
                } else {
                    m_drawingPane.setMouseDown(false);
                    m_isDragged = false;
                    zoomByClick(e.getPoint());
                    return;
                }
            }
            m_drawingPane.setMouseDown(false);
            m_isDragged = false;
            m_drawingPane.setDragEnd(e.getPoint());
            zoomByWindow(m_drawingPane.getSelectionRectangle());
            m_properties.revalidate();
            m_drawingPane.repaint();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            m_isDragged = true;
            m_drawingPane.setDragEnd(e.getPoint());
            if (m_drawingPane.isMouseDown()) {
                // simply repaint to update the dragging rectangle
                m_drawingPane.repaint();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Cursor getCursor() {
            if (m_zoomCursor == null) {

                ImageIcon zoomCursorImage = getZoomIcon();
                m_zoomCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                // sets the hot spot to the cross in the magnifier
                        zoomCursorImage.getImage(), new Point(15, 15), "Zoom");
            }

            return m_zoomCursor;
        }

        /**
         * Returns the image icon intended for zoom representation.
         *
         * @return the zoom icon
         */
        private ImageIcon getZoomIcon() {
            return new ImageIcon(AbstractPlotter.class
                    .getResource("zoomInCursor.gif"));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Zooming";
        }
    }

    /**
     * When the drawing pane is larger then the viewport for it, this mouse
     * listener realizes the possibility to drag the drawing pane inside the
     * viewport.
     *
     * @author Fabian Dill, University of Konstanz
     */
    public class MovingMouseListener extends PlotterMouseListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            m_drawingPane.setDragStart(e.getPoint());
            m_drawingPane.setMouseDown(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            // On linux, e.isPopupTrigger never returns true, apparently
            if (!m_isDragged && e.isPopupTrigger()
                    || SwingUtilities.isRightMouseButton(e)) {
                showPopupMenu(e.getPoint());
            }
            m_drawingPane.setMouseDown(false);
            m_isDragged = false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            m_isDragged = true;
            int diffWidth = e.getX() - m_drawingPane.getDragStart().x;
            int diffHeight = e.getY() - m_drawingPane.getDragStart().y;
            m_drawingPane.setDragStart(e.getPoint());
            // add or substract the diff and scroll to rectangle
            Rectangle viewport = m_drawingPane.getVisibleRect();
            // int newX = viewport.x + diffWidth;
            // int newY = viewport.y + diffHeight;
            Rectangle newRect =
                    new Rectangle(viewport.x + diffWidth, viewport.y
                            + diffHeight, viewport.width, viewport.height);
            m_drawingPane.scrollRectToVisible(newRect);
            updateAxisLength();
            m_drawingPane.repaint();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Cursor getCursor() {
            return new Cursor(Cursor.MOVE_CURSOR);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Moving";
        }

    }

    /* ----------- mapping, coordinates -------------- */

    /**
     *
     * @param preserve true if old min max values of the axes should be
     *            preserved, false otherwise.
     */
    public void setPreserve(final boolean preserve) {
        m_preserve = preserve;
    }

    /**
     *
     * @param x domain value
     * @return mapped value
     */
    protected int getMappedXValue(final DataCell x) {
        return (int)getXAxis().getCoordinate().calculateMappedValue(x,
                getDrawingPaneDimension().width);
    }

    /**
     *
     * @param y domain value
     * @return mapped value
     */
    protected int getMappedYValue(final DataCell y) {
        double mappedValue =
                getYAxis().getCoordinate().calculateMappedValue(y,
                        getDrawingPaneDimension().height);
        return (int)getScreenYCoordinate(mappedValue);

    }

    /**
     * Recalculates the domain of the y axis. If preserve is set to false the
     * passed values are taken as min and max no matter was was set before. If
     * preserve is set to true (default) the possibly already available min and
     * max values are preserved.
     *
     * @param min the min value
     * @param max the max value {@link AbstractPlotter#setPreserve(boolean)}
     */
    public void createYCoordinate(final double min, final double max) {
        DataColumnDomainCreator yDomainCreator = new DataColumnDomainCreator();
        double actualMin = min;
        double actualMax = max;
        if (getYAxis() != null && getYAxis().getCoordinate() != null
                && m_preserve) {
            if (!(getYAxis().getCoordinate() instanceof NumericCoordinate)) {
                return;
            }
            actualMin =
                    Math.min(min, ((NumericCoordinate)getYAxis()
                            .getCoordinate()).getMinDomainValue());
            actualMax =
                    Math.max(max, ((NumericCoordinate)getYAxis()
                            .getCoordinate()).getMaxDomainValue());
        }
        yDomainCreator.setLowerBound(new DoubleCell(actualMin));
        yDomainCreator.setUpperBound(new DoubleCell(actualMax));
        DataColumnSpecCreator ySpecCreator =
                new DataColumnSpecCreator("Y", DoubleCell.TYPE);
        ySpecCreator.setDomain(yDomainCreator.createDomain());
        Coordinate yCoordinate =
                Coordinate.createCoordinate(ySpecCreator.createSpec());
        if (getYAxis() == null) {
            Axis yAxis =
                    new Axis(Axis.VERTICAL, getDrawingPaneDimension().height);
            setYAxis(yAxis);
        }
        getYAxis().setCoordinate(yCoordinate);
    }

    /**
     * Recalculates the domain of the x axis. If preserve is set to false the
     * passed values are taken as min and max no matter was was set before. If
     * preserve is set to true (default) the possibly already available min and
     * max values are preserved.
     *
     * @param min the min value
     * @param max the max value {@link AbstractPlotter#setPreserve(boolean)}
     */
    public void createXCoordinate(final double min, final double max) {
        DataColumnDomainCreator xDomainCreator = new DataColumnDomainCreator();
        double actualMin = min;
        double actualMax = max;
        if (getXAxis() != null && getXAxis().getCoordinate() != null
                && m_preserve) {
            if (!(getXAxis().getCoordinate() instanceof NumericCoordinate)) {
                return;
            }
            actualMin =
                    Math.min(min, ((NumericCoordinate)getXAxis()
                            .getCoordinate()).getMinDomainValue());
            actualMax =
                    Math.max(max, ((NumericCoordinate)getXAxis()
                            .getCoordinate()).getMaxDomainValue());
        }
        xDomainCreator.setLowerBound(new DoubleCell(actualMin));
        xDomainCreator.setUpperBound(new DoubleCell(actualMax));
        DataColumnSpecCreator xSpecCreator =
                new DataColumnSpecCreator("X", DoubleCell.TYPE);
        xSpecCreator.setDomain(xDomainCreator.createDomain());
        Coordinate xCoordinate =
                Coordinate.createCoordinate(xSpecCreator.createSpec());
        if (getXAxis() == null) {
            Axis xAxis =
                    new Axis(Axis.HORIZONTAL, getDrawingPaneDimension().width);
            setXAxis(xAxis);
        }
        getXAxis().setCoordinate(xCoordinate);
    }

    /**
     * Recalculates the domain of the x axis. If preserve is set to false the
     * passed values are taken as min and max no matter was was set before. If
     * preserve is set to true (default) the possibly already available min and
     * max values are preserved.
     *
     * @param min the min value
     * @param max the max value {@link AbstractPlotter#setPreserve(boolean)}
     */
    public void createXCoordinate(final int min, final int max) {
        DataColumnDomainCreator xDomainCreator = new DataColumnDomainCreator();
        int actualMin = min;
        int actualMax = max;
        if (getXAxis() != null && getXAxis().getCoordinate() != null
                && m_preserve) {
            if (!(getXAxis().getCoordinate() instanceof NumericCoordinate)) {
                return;
            }
            actualMin =
                    (int)Math.min(min, ((NumericCoordinate)getXAxis()
                            .getCoordinate()).getMinDomainValue());
            actualMax =
                    (int)Math.max(max, ((NumericCoordinate)getXAxis()
                            .getCoordinate()).getMaxDomainValue());
        }
        xDomainCreator.setLowerBound(new IntCell(actualMin));
        xDomainCreator.setUpperBound(new IntCell(actualMax));
        DataColumnSpecCreator xSpecCreator =
                new DataColumnSpecCreator("X", IntCell.TYPE);
        xSpecCreator.setDomain(xDomainCreator.createDomain());
        Coordinate xCoordinate =
                Coordinate.createCoordinate(xSpecCreator.createSpec());
        if (getXAxis() == null) {
            Axis xAxis =
                    new Axis(Axis.HORIZONTAL, getDrawingPaneDimension().width);
            setXAxis(xAxis);
        }
        getXAxis().setCoordinate(xCoordinate);
    }

    /**
     * Creates a nominal x axis.
     *
     * @param values the possible values.
     */
    public void createNominalXCoordinate(final Set<DataCell> values) {
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator();
        domainCreator.setValues(values);
        DataColumnSpecCreator specCreator =
                new DataColumnSpecCreator("X", StringCell.TYPE);
        specCreator.setDomain(domainCreator.createDomain());
        Coordinate nominalCoordinate =
                Coordinate.createCoordinate(specCreator.createSpec());
        if (getXAxis() == null) {
            Axis xAxis =
                    new Axis(Axis.HORIZONTAL, getDrawingPaneDimension().width);
            setXAxis(xAxis);
        }
        getXAxis().setCoordinate(nominalCoordinate);
    }

    /**
     * Creates a nominal y axis.
     *
     * @param values the possible values.
     */
    public void createNominalYCoordinate(final Set<DataCell> values) {
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator();
        domainCreator.setValues(values);
        DataColumnSpecCreator specCreator =
                new DataColumnSpecCreator("X", StringCell.TYPE);
        specCreator.setDomain(domainCreator.createDomain());
        Coordinate nominalCoordinate =
                Coordinate.createCoordinate(specCreator.createSpec());
        if (getYAxis() == null) {
            Axis yAxis =
                    new Axis(Axis.VERTICAL, getDrawingPaneDimension().width);
            setYAxis(yAxis);
        }
        getYAxis().setCoordinate(nominalCoordinate);
    }

    /**
     *
     * @param y the mapped y value
     * @return the y value but counted from bottom.
     */
    public final double getScreenYCoordinate(final double y) {
        return getDrawingPaneDimension().height - y;
    }

    /* --------------- ignore methods ------------------ */

    /**
     * {@inheritDoc}
     */
    public void componentHidden(final ComponentEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void componentMoved(final ComponentEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void componentShown(final ComponentEvent e) {
    }
}

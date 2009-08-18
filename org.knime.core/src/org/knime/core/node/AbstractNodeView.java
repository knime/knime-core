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


/**
 * Abstract implementation of a node view. A node view is the visual component
 * to a node, displaying a computed result (or the ongoing computation). 
 * 
 * <p>
 * <strong>Note:</strong>Concrete implementations do not inherit from this class
 * directly but extend from {@link NodeView}. Nodes that open an external 
 * application extend from {@link ExternalApplicationNodeView}. 
 *  
 * @author Bernd Wiswedel, University of Konstanz
 * @param <T> the implementation of the {@link NodeModel} this node view
 *          is based on
 */
public abstract class AbstractNodeView<T extends NodeModel> {
    
    /**
     * The node logger for this class; do not make static to make sure the right
     * class name is printed in messages.
     */
    private final NodeLogger m_logger;
    
    /** Name of view. Used for log messages and suggested view title (if 
     * subclass allows view titles). */
    private String m_viewName;

    /** Holds the underlying <code>NodeModel</code> of type T, never null. */
    private final T m_nodeModel;

    /** Creates new view. This constructor keeps the node model reference and
     * instantiates the logger. 
     * @param nodeModel The underlying node model.
     * @throws NullPointerException If the <code>nodeModel</code> is null.
     */
    AbstractNodeView(final T nodeModel) {
        if (nodeModel == null) {
            throw new NullPointerException("Node Mode must not be null");
        }
        m_logger = NodeLogger.getLogger(this.getClass());
        m_nodeModel = nodeModel;
    }
    
    /**
     * Get reference to underlying <code>NodeModel</code>, never null.
     * @return NodeModel reference.
     */
    protected T getNodeModel() {
        return m_nodeModel;
    }
    
    /** Get reference to logger, never null. The logger is customized with an 
     * appropriate name (currently the runtime class of the view).
     * @return reference to logger */
    public NodeLogger getLogger() {
        return m_logger;
    }

    /**
     * Called from the model that something has changed. It calls the abstract
     * method {@link #modelChanged()}. 
     * 
     * <p>This method is called when the node makes state transitions that 
     * affect the node model content, i.e. after execute or reset or when the
     * highlight handler has changed.
     */
    void callModelChanged() {
        synchronized (m_nodeModel) {
            try {
                // CALL abstract model changed
                modelChanged();
            } catch (NullPointerException npe) {
                m_logger.coding("NodeView.modelChanged() causes "
                       + "NullPointerException during notification of a "
                       + "changed model, reason: " + npe.getMessage(), npe);
            } catch (Throwable t) {
                m_logger.error("NodeView.modelChanged() causes an error "
                       + "during notification of a changed model, reason: "
                       + t.getMessage(), t);
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
     * This method can be overridden by views that want to receive
     * events from their assigned models via the
     * {@link NodeModel#notifyViews(Object)} method. Can be used to
     * iteratively update the view during execute.
     * 
     * @param arg The argument that is provided in the 
     * {@linkplain NodeModel#notifyViews(Object) notifyViews} method.
     */
    protected void updateModel(final Object arg) {
        // dummy statement to get rid of 'parameter not used' warning.
        assert arg == arg;
    }

    /** Called from the framework to open a new view or bring an existing
     * view to front. The title serves as default view name and should be
     * shown in the view title bar if possible. This method must be called 
     * at most once per view instance!
     * @param title The view title. 
     * @see #closeView() */
    final void openView(final String title) {
        synchronized (m_nodeModel) {
            m_nodeModel.registerView(this);
            m_viewName = title;
            callOpenView(title);
        }
    }

    /** Direct(!) subclasses override this method and open the view or frame. 
     * This method is called from {@link #openView(String)} and is called
     * at most once.
     * @param title the default title of the view. It should be shown in the 
     * view title bar (if at all possible).
     */
    abstract void callOpenView(final String title);
    
    /** Closes the view and disposes all allocated resources. The view is not
     * meant to be opened again. This method is the counterpart to 
     * {@link #openView(String)} and should be final, though the 
     * {@link NodeView#closeView()} method is currently public. */
    void closeView() {
        synchronized (m_nodeModel) {
            m_nodeModel.unregisterView(this);
            callCloseView();
        }
    }
    
    /** Called from {@link #closeView()} to close the view and release all 
     * allocated resources. The view will not be opened again. */
    abstract void callCloseView();
    
    /** @return the viewName as set in the {@link #openView(String)} method. */
    String getViewName() {
        return m_viewName;
    }
    
}

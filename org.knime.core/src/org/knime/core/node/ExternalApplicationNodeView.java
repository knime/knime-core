/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Aug 14, 2009 (wiswedel): created
 */
package org.knime.core.node;

/**
 * Node view which opens an external application. Opening, closing and 
 * updating the application is task of derived view classes.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @param <T> The node model class.
 */
public abstract class ExternalApplicationNodeView<T extends NodeModel> 
    extends AbstractNodeView<T> {
    
    /** Creates the view instance but does not open the external application 
     * yet.
     * @param model The node model assigned to the view, must not be null. 
     */
    protected ExternalApplicationNodeView(final T model) {
        super(model);
    }
    
    /** {@inheritDoc} */
    @Override
    void callCloseView() {
        onClose();
    }

    /** {@inheritDoc} */
    @Override
    void callOpenView(final String title) {
        onOpen(title);
    }
    
    /** To be called by client code when the external view is closed. This will
     * initiate the usual closing procedure, i.e. unregistering this view from 
     * its model and also calling {@link #onClose()}.  
     * {@inheritDoc} */
    @Override
    public final void closeView() {
        super.closeView();
    }
    
    /**
     * Open the external application.  
     * @param title The desired title of the application, possibly ignored.
     */
    protected abstract void onOpen(final String title);
    
    /** Close the view. This method is called when the node is deleted or
     * {@link #closeView()} is called. This method should not be used if the
     * external application initiates the close operation. Clients should call
     * {@link #closeView()} instead. */
    protected abstract void onClose();

}

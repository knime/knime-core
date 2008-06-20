/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jun 19, 2008 (wiswedel): created
 */
package org.knime.core.node;

import org.knime.core.eclipseUtil.GlobalClassCreator;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ParameterizedNodeFactory<T extends GenericNodeModel> extends
        GenericNodeFactory<GenericNodeModel> {
    
    private final GenericNodeFactory<T> m_delegate;
    
    private static GenericNodeFactory generateSampleFileReader() {
        Class<?> c;
        try {
            c = GlobalClassCreator.createClass("org.knime.base.node.io.filereader.FileReaderNodeFactory");
            return c.asSubclass(GenericNodeFactory.class).newInstance();
        } catch (Exception e) {
            NodeLogger.getLogger(ParameterizedNodeFactory.class).error(
                    "Can't create file reader factory", e);
            throw new RuntimeException(e);
        }
    }
    /**
     * 
     */
    public ParameterizedNodeFactory() {
        this(generateSampleFileReader());
    } 
    
    public ParameterizedNodeFactory(final GenericNodeFactory<T> delegate) {
        if (delegate == null) {
            throw new NullPointerException("Arg must not be null.");
        }
        m_delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    protected GenericNodeDialogPane createNodeDialogPane() {
        return new ParameterizedNodeDialogPane(
                m_delegate.createNodeDialogPane());
    }

    /** {@inheritDoc} */
    @Override
    public ParameterizedNodeModel<T> createNodeModel() {
        T delegate = m_delegate.createNodeModel();
        return new ParameterizedNodeModel<T>(delegate);
    }

    /** {@inheritDoc} */
    @Override
    public GenericNodeView<GenericNodeModel> createNodeView(final int viewIndex,
            final GenericNodeModel nodeModel) {
        ParameterizedNodeModel<T> model = 
            (ParameterizedNodeModel<T>)nodeModel;
        return (GenericNodeView<GenericNodeModel>)
            m_delegate.createNodeView(viewIndex, model.getDelegate());
    }

    /** {@inheritDoc} */
    @Override
    protected int getNrNodeViews() {
        return m_delegate.getNrNodeViews();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasDialog() {
        return m_delegate.hasDialog();
    }

}

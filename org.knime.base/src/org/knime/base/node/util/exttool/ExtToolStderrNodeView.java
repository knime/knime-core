/*
 * ------------------------------------------------------------------ *
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
 *
 * History
 *   12.10.2006 (ohl): created
 */
package org.knime.base.node.util.exttool;

import java.util.Arrays;
import java.util.Collection;

/**
 * The view showing the output to standard error. Listens to notifications (see
 * {@link ViewUpdateNotice}) of type <code>stderr</code>.
 *
 * @author ohl, University of Konstanz
 * @param <T> the actual implementation of the abstract node model
 */
public class ExtToolStderrNodeView<T extends ExtToolOutputNodeModel> extends
        ExtToolOutputNodeView<T> {

    /**
     * The constructor.
     *
     * @param nodeModel the model associated with this view.
     */
    public ExtToolStderrNodeView(final T nodeModel) {
        super(nodeModel);
        setViewTitle("Output to standard ERROR");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateModel(final Object arg) {
        if (arg instanceof ViewUpdateNotice) {
            ViewUpdateNotice vun = (ViewUpdateNotice)arg;
            if (vun.TYPE == ViewUpdateNotice.ViewType.stderr) {
                addLineInSwingThreadLater(vun.getNewLine());
            }
        } else if (arg == null) {
            clearText();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<String> getFullFailureOutput() {
        return (getNodeModel()).getFailedExternalErrorOutput();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<String> getFullOutput() {
        return (getNodeModel()).getExternalErrorOutput();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<String> getNoOutputText() {
        return Arrays.asList(new String[]{"No output to standard error"});
    }
}

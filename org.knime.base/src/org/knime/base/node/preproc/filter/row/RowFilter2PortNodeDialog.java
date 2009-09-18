/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
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
 *   Sep 18, 2009 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class RowFilter2PortNodeDialog extends RowFilterNodeDialogPane {

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPanel createDialogPanel() {

        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());

        JPanel s = super.createDialogPanel();
        s.setBorder(BorderFactory.createEtchedBorder());
        result.add(s, BorderLayout.CENTER);

        JPanel title = new JPanel();
        title.add(new JLabel("<html><b>Set the filter criteria "
                + "for the upper port</b></html>"));
        result.add(title, BorderLayout.NORTH);

        return result;
    }

}

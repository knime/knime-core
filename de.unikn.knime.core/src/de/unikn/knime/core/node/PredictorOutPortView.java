/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   26.10.2005 (gabriel): created
 */
package de.unikn.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A port view showing the port's PredictorParams description.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class PredictorOutPortView extends NodeOutPortView {

    /** Shows the PredictorParams. */
    private final JTextArea m_predParamsText;

    /** If no PredictorParams available. */
    private static final String NO_TEXT = "<No Predictor Parameters>";

    /**
     * A view showing the stuff stored in the specified PredictorParams ouput
     * port.
     * 
     * @param nodeName Name of the node the inspected port belongs to. Will
     *            appear in the title of the frame.
     * @param portName Name of the port to view the PredictorParams from. Will
     *            appear in the title of the frame.
     * 
     */
    PredictorOutPortView(final String nodeName, final String portName) {
        super(nodeName + ", Port: " + portName);
        m_predParamsText = new JTextArea(NO_TEXT);
        m_predParamsText.setEditable(false);
        m_predParamsText.setFont(new Font("Courier", Font.PLAIN, 12));
        Container cont = getContentPane();
        cont.setLayout(new BorderLayout());
        cont.setBackground(NodeView.COLOR_BACKGROUND);
        cont.add(new JScrollPane(m_predParamsText), BorderLayout.CENTER);
    }

    /**
     * Updates the view's content with new PredictorParams object.
     * 
     * @param predParams The new content can be null.
     */
    void updatePredictorParams(final PredictorParams predParams) {
        m_predParamsText.removeAll();
        if (predParams == null) {
            m_predParamsText.setText(NO_TEXT);
        } else {
            m_predParamsText.setText(predParams.toString());
        }
        super.updatePortView();
    }

}

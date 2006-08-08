/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   27.02.2006 (dill): created
 */
package org.knime.base.node.mine.subgroupminer;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.core.node.NodeView;


/**
 * The BitvectorGeneratorView provides information about the generation of the
 * bitsets out of the data. In particular, this is the number of processed rows,
 * the resulting bitvector length, the total number of generated zeros and ones
 * and the resulting ratio from 1s to 0s.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BitVectorGeneratorView extends NodeView {
    private JEditorPane m_pane;

    private static final int ROUNDING_CONSTANT = 10000;

    /**
     * Creates the view instance or the BitVectorGeneratorNode with the
     * BitVectorGeneratorNodeModel as the underlying model.
     * 
     * @param model the underlying node model
     */
    public BitVectorGeneratorView(final BitVectorGeneratorNodeModel model) {
        super(model);
        m_pane = new JEditorPane();
        m_pane.setEditable(false);
        m_pane.setText("No data available");
        setComponent(new JScrollPane(m_pane));
    }

    /**
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        BitVectorGeneratorNodeModel model = 
            (BitVectorGeneratorNodeModel)getNodeModel();
        if (model.getNumberOfProcessedRows() > 0) {
            setTextArea();
        }
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        int nrOfRows = ((BitVectorGeneratorNodeModel)getNodeModel())
                .getNumberOfProcessedRows();
        if (nrOfRows > 0) {
            setTextArea();
        }
    }

    private void setTextArea() {
        BitVectorGeneratorNodeModel model 
            = (BitVectorGeneratorNodeModel)getNodeModel();
        StringBuffer buffer = new StringBuffer("<html></body>");
        buffer.append("<h2>BitVector Generator Information:</h2>");
        buffer.append("<hr>");
        buffer.append("<table>");
        buffer.append("<tr><td>Number of processed rows: </td>"
                + "<td align=\"right\">" + model.getNumberOfProcessedRows()
                + " </td></tr>");
        buffer.append("<tr><td>Resulting BitVector length: </td>"
                + "<td align=\"right\">" + model.getBitVectorLength()
                + "</td></tr>");
        buffer.append("<tr><td>Total number of 0s: </td>"
                + "<td align=\"right\">" + model.getTotalNrOf0s()
                + " </td></tr>");
        buffer.append("<tr><td>Total number of 1s: </td>"
                + "<td align=\"right\">" + model.getTotalNrOf1s()
                + "</td></tr>");
        double ratio = (int)(((double)model.getTotalNrOf1s() / (double)model
                .getTotalNrOf0s()) * ROUNDING_CONSTANT);
        ratio = ratio / ROUNDING_CONSTANT;
        buffer.append("<tr><td>Ratio of 1s to 0s: </td>"
                + "<td align=\"right\">" + ratio + "</td></tr></table>");
        buffer.append("</body></html>");
        m_pane = new JEditorPane("text/html", "");
        m_pane.setText(buffer.toString());
        m_pane.setEditable(false);
        setComponent(new JScrollPane(m_pane));
    }
}

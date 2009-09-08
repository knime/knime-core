/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   18.04.2005 (cebron): created
 */
package org.knime.base.node.viz.statistics;

import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.knime.core.data.DataCell;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableView;

/**
 * The view shows the statistical information.
 * 
 * @author cebron, University of Konstanz
 */
public class Statistics2NodeView extends NodeView<Statistics2NodeModel> {

    /** Pane for first order moments. */
    private final TableView m_output = new TableView();
    private final JEditorPane m_nominal = new JEditorPane("text/html", "");
    
    /** Used to get a string representation of the double value. */
    private static final NumberFormat FORMAT = 
        NumberFormat.getNumberInstance(Locale.US);
    static {
        FORMAT.setGroupingUsed(false);
    }
    
    /**
     * Constructs a <code>NodeView</code> consisting of statistical values.
     * 
     * @param model The underlying NodeModel
     */
    Statistics2NodeView(final Statistics2NodeModel model) {
        super(model);
        m_nominal.setEditable(false);
        JScrollPane scroller2 = new JScrollPane(m_nominal);
        JTabbedPane tab = new JTabbedPane();
        tab.setPreferredSize(new Dimension(400, 200));
        tab.add("Numeric columns", m_output);
        tab.add("Nominal columns", scroller2);
        setComponent(tab);
    }

    /**
     * If the model changes, the new statistical information from the original
     * table is added to the upper statistics table.
     * 
     * @see NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        refreshStastics();
        refreshNominalValues();
    }
    
    private void refreshStastics() {
        Statistics2NodeModel myModel = getNodeModel();
        if (myModel.getColumnNames() == null) {
            m_output.setDataTable(null);
        } else {
            m_output.setDataTable(myModel.getStatsTable());
        }
    }
    
    private void refreshNominalValues() {
        Statistics2NodeModel myModel = getNodeModel();
        String[] columnNames = myModel.getNominalColumnNames();
        if (columnNames == null) {
            return;
        }
        
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html>\n");
        buffer.append("<body>\n");
        buffer.append("<p>&nbsp</p>");
        buffer.append("<table border = \"1\">");

        for (int i = 0; i < columnNames.length; i++) {
            if (myModel.getNominals()[i] != null) {
                buffer.append("<th style=\"white-space: nowrap\">" 
                		+ columnNames[i] + "</th>");
            }
        }
        
        buffer.append("<tr valign=\"top\">");
        double[] missings = myModel.getNumMissingValues();
        for (int i = 0; i < columnNames.length; i++) {
        	if (myModel.getNominals()[i] != null) {
                buffer.append("<td style=\"white-space: nowrap\"><strong>"
                		+ "No. missings: </strong>" 
                        + ((int) missings[i]) + "</td>");
            }
        }
        
        buffer.append("</tr><tr valign=\"top\">");
        
        if (myModel.getNominals() != null) {
            final int numNomValues = myModel.numOfNominalValues();
            for (int j = 0; j < myModel.getNominals().length; j++) {
                Map<DataCell, Integer> map = myModel.getNominals()[j];
                if (map != null) {
                    buffer.append("<td nowrap=\"nowrap\">");
                    final int size = map.size();
                    if (size == 0) {
                        buffer.append("<i>contains more than " 
                                + getNodeModel().numOfNominalValuesOutput()
                                + " nominal values</i>");
                    } else {
                        int cnt = 0;
                        buffer.append("<strong>Top " + numNomValues 
                                + ":</strong><br>");
                        for (DataCell c : map.keySet()) {
                            buffer.append(c.toString() + " : " 
                                            + map.get(c) + "<br>");
                            if (++cnt == numNomValues) {
                                break;
                            }
                        }
                        buffer.append("</td>");
                    }
                }                    
            }
        }
        buffer.append("</tr>");
        
        buffer.append("</tr><tr valign=\"top\">");
        
        if (myModel.getNominals() != null) {
            final int numNomValues = myModel.numOfNominalValues();
            for (int j = 0; j < myModel.getNominals().length; j++) {
                Map<DataCell, Integer> map = myModel.getNominals()[j];
                if (map != null) {
                    buffer.append("<td style=\"white-space: nowrap\">");
                    buffer.append("<strong>Bottom " + numNomValues 
                            + ":</strong><br>");
                    final int size = map.size();
                    if (size >= numNomValues) {
                        int cnt = 0;
                        for (DataCell c : map.keySet()) {
                            if (cnt >= 
                            	  Math.max(numNomValues, size - numNomValues)) {
                                buffer.append(c.toString() + " : " 
                                            + map.get(c) + "<br>");
                            }
                            cnt++;
                        }
                        buffer.append("</td>");
                    }
                }                    
            }
        }
        buffer.append("</tr>");

        buffer.append("</table>");
        buffer.append("<p>&nbsp</p>");
        buffer.append("</body>\n");
        buffer.append("</html>\n");
        buffer.append("");
        m_nominal.setText(buffer.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }
}

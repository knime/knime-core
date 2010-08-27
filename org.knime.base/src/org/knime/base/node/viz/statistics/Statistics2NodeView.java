/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

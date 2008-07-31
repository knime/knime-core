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
 *   Jun 11, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.scorer.entrop;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.knime.base.node.util.DoubleFormat;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableView;

/**
 * This panel is the view to a EntropyCalculator. SplitPane that shows on top
 * some basic clustering statistics and at the bottom the clusters, connnected
 * to a hilite handler.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class EntropyView extends JSplitPane {
    /** The table view containing cluster statistics. */
    private final TableView m_tableView;

    /** The text pane that holds the information. */
    private final JEditorPane m_editorPane;
    /** Scrollpane containing m_editorPane. */
    private final JScrollPane m_editorPaneScroller;
    
    /** Constructs new panel. */
    public EntropyView() {
        super(VERTICAL_SPLIT);
        m_editorPane = new JEditorPane("text/html", "");
        m_editorPane.setEditable(false);
        m_tableView = new TableView();
        m_tableView.setShowColorInfo(false);
        m_editorPaneScroller = new JScrollPane(m_editorPane);
        setTopComponent(m_editorPaneScroller);
        setBottomComponent(m_tableView);
    }

    /**
     * Sets a new model. The model may be <code>null</code>.
     * 
     * @param calculator the new entropy statistics
     */
    public void update(final EntropyCalculator calculator) {
        m_editorPane.setText("");
        if (calculator == null) {
            m_tableView.setDataTable(null);
            return;
        }
        m_tableView.setDataTable(calculator.getScoreTable());
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html>");
        buffer.append("<body>\n");
        buffer.append("<h1>Clustering statistics</h1>");
        buffer.append("<hr>\n");
        buffer.append("<table>\n");
        buffer.append("<caption style=\"text-align: left\">");
        buffer.append("Data Statistics</caption>");
        buffer.append("<tr>");
        buffer.append("<th>Statistics</th>");
        buffer.append("<th>Value</th>");
        buffer.append("</tr>");
        String[] stats = new String[]{"Number of clusters found: ",
                "Number of objects in clusters: ",
                "Number of reference clusters: ", "Total number of patterns: "};
        int[] vals = new int[]{calculator.getNrClusters(),
                calculator.getPatternsInClusters(),
                calculator.getNrReference(),
                calculator.getPatternsInReference()};
        for (int i = 0; i < stats.length; i++) {
            buffer.append("<tr>\n");
            buffer.append("<td>\n");
            buffer.append(stats[i]);
            buffer.append("\n</td>\n");
            buffer.append("<td>\n");
            buffer.append(vals[i]);
            buffer.append("\n</td>\n");
            buffer.append("</tr>\n");
        }
        buffer.append("</table>\n");
        buffer.append("<table>\n");
        buffer.append("<caption style=\"text-align: left\">");
        buffer.append("Data Statistics</caption>");
        buffer.append("<tr>");
        buffer.append("<th>Score</th>");
        buffer.append("<th>Value</th>");
        buffer.append("</tr>");
        buffer.append("<tr>\n");
        buffer.append("<td>\n");
        buffer.append("Entropy: ");
        buffer.append("\n</td>\n");
        buffer.append("<td>\n");
        buffer.append(DoubleFormat.formatDouble(calculator.getEntropy()));
        buffer.append("\n</td>\n");
        buffer.append("</tr>\n");
        buffer.append("<tr>\n");
        buffer.append("<td>\n");
        buffer.append("Quality: ");
        buffer.append("\n</td>\n");
        buffer.append("<td>\n");
        buffer.append(DoubleFormat.formatDouble(calculator.getQuality()));
        buffer.append("\n</td>\n");
        buffer.append("</tr>\n");
        buffer.append("</table>\n");
        buffer.append("</body>\n");
        buffer.append("</html>\n");
        m_editorPane.setText(buffer.toString());
        // Do not call m_editorPane.getPreferredSize, bug in java:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4839118
        int preferredEditorSize =
                m_editorPaneScroller.getPreferredSize().height + 10;
        if (getSize().height > preferredEditorSize) {
            setDividerLocation(preferredEditorSize);
        }
    }

    /**
     * Sets the hilite handler to be used.
     * 
     * @param handler new handler or <code>null</code>
     */
    public void setHiliteHandler(final HiLiteHandler handler) {
        m_tableView.setHiLiteHandler(handler);
    }
}

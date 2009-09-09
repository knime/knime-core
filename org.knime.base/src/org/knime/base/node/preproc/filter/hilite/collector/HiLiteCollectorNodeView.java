/* 
 * ------------------------------------------------------------------
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
 * 
 * History
 *   16.04.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.hilite.collector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTable;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableView;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class HiLiteCollectorNodeView
        extends NodeView<HiLiteCollectorNodeModel> {
    
    private final TableView m_table;
    
    /**
     * Creates a new view on a hilite collector model.
     * @param model the underlying hilite collector model
     */
    HiLiteCollectorNodeView(final HiLiteCollectorNodeModel model) {
        super(model);
        super.setShowNODATALabel(false);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        p.setBorder(BorderFactory.createTitledBorder(" Append Annotation "));
        
        final JCheckBox checkBox = new JCheckBox("New Column");
        
        final JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(150, 
                Math.max(20, textField.getHeight())));
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final java.awt.event.KeyEvent e) {
                if (e == null) {
                    return;
                }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    appendAnnotation(
                            textField.getText(), checkBox.isSelected());
                }
            }
        });
        p.add(textField);

        JButton button = new JButton("Apply");
        button.setPreferredSize(new Dimension(100, 
                Math.max(25, button.getHeight())));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (e == null) {
                    return;
                }
                appendAnnotation(textField.getText(), checkBox.isSelected());
            }
        });
        p.add(button);
        p.add(checkBox);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(p, BorderLayout.NORTH);
        TableContentModel cview = new TableContentModel() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void hiLite(final KeyEvent e) {
                modelChanged();
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public void unHiLite(final KeyEvent e) {
                modelChanged();
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public void unHiLiteAll(final KeyEvent e) {
                modelChanged();
            }
        };
        m_table = new TableView(cview);
        super.getJMenuBar().add(m_table.createHiLiteMenu());
        m_table.setPreferredSize(new Dimension(425, 250));
        m_table.setShowColorInfo(false);
        panel.add(m_table, BorderLayout.CENTER);
        super.setComponent(panel);
    }
    
    private void appendAnnotation(final String anno, final boolean newColumn) {
        if (anno != null && !anno.isEmpty()) {
            getNodeModel().appendAnnotation(anno, newColumn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged() {
        DataTable data = super.getNodeModel().getHiLiteAnnotationsTable();
        m_table.setDataTable(data);
        HiLiteHandler hdl = super.getNodeModel().getInHiLiteHandler(0);
        m_table.setHiLiteHandler(hdl);
        m_table.setColumnWidth(50);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateModel(final Object arg) {
        modelChanged();
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

    }

}

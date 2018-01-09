/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   02.04.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.template;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;

import org.knime.base.node.jsnippet.util.JSnippetTemplate;

/**
 * The Panel displayed in the templates tab of a jsnippet dialog.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @param <T> {@link JSnippetTemplate} instance
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings({"rawtypes", "serial" })
public class TemplatesPanel<T extends JSnippetTemplate> extends JPanel {
    private static final String CARD_EMPTY_SELECTION = "CARD_EMPTY_SELECTION";
    private static final String CARD_PREVIEW = "CARD_PREVIEW";
    private JComboBox<String> m_categories;
    /** Displays templates that are in the currently selected category. */
    private JList<T> m_templates;
    private JTextPane m_description;
    private Collection<Class> m_metaCategories;
    private JPanel m_previewPanel;
    private JPanel m_previewPane;
    private TemplateController<T> m_controller;
    private ActionListener m_categoriesListener;
    private JButton m_removeTemplateButton;
    private JButton m_applyTemplateButton;
    private TemplateProvider<T> m_provider;

    /**
     * Create a new instance.
     * @param metaCategories the meta categories to display
     * @param controller the controller used to create the preview and to apply
     * template to the java snippet node
     * @param provider the {@link TemplateProvider} to use
     */
    public TemplatesPanel(final Collection<Class> metaCategories,
            final TemplateController<T> controller, final TemplateProvider<T> provider) {
        super(new BorderLayout());
        m_controller = controller;
        m_metaCategories = metaCategories;

        m_categories = new JComboBox<>();
        m_categoriesListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                String category = (String)cb.getSelectedItem();
                updateTemplatesList(category);
            }
        };
        m_categories.addActionListener(m_categoriesListener);

        m_templates = new JList<>(new DefaultListModel<T>());
        m_templates.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        m_templates.setCellRenderer(new TemplateListCellRenderer<T>());

        m_templates.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent e) {
                Object selected = m_templates.getSelectedValue();

                onSelectionInTemplateList(selected);
            }
        });
        m_description = new JTextPane();
        HTMLEditorKit kit = new HTMLEditorKit();
        m_description.setEditorKit(kit);
        m_description.setEditable(false);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JPanel templatesPanel = new JPanel(new BorderLayout());
        templatesPanel.add(createTemplatesPanel(), BorderLayout.CENTER);
        mainSplit.setTopComponent(templatesPanel);
        m_previewPanel = new JPanel(new BorderLayout());
        m_previewPanel.add(m_controller.getPreview(), BorderLayout.CENTER);
        m_previewPane = new JPanel(new CardLayout());
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel("Please select a template."));
        m_previewPane.add(p, CARD_EMPTY_SELECTION);
        m_previewPane.add(m_previewPanel, CARD_PREVIEW);
        mainSplit.setBottomComponent(m_previewPane);
        add(mainSplit, BorderLayout.CENTER);

        setTemplateProvider(provider); // requires that all GUI elements are initialized
    }

    /**
     * Change template provider and regenerate category/template list.
     * @param provider the {@link TemplateProvider} to use
     */
    public void setTemplateProvider(final TemplateProvider<T> provider) {
        m_provider = provider;
        m_provider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateCategories();
                updateTemplatesList(TemplateProvider.ALL_CATEGORY);
            }
        });
        updateCategories();
        updateTemplatesList(TemplateProvider.ALL_CATEGORY);
        m_templates.setSelectedIndex(-1);
    }

    private void updateCategories() {

        m_categories.removeActionListener(m_categoriesListener);
        m_categories.removeAllItems();
        Set<String> categories = m_provider.getCategories(m_metaCategories);
        for (String category : categories) {
            m_categories.addItem(category);
        }
        m_categories.setSelectedItem(TemplateProvider.ALL_CATEGORY);
        m_categories.addActionListener(m_categoriesListener);
    }

    private void onSelectionInTemplateList(final Object selected) {
        CardLayout cl = (CardLayout)(m_previewPane.getLayout());
        if (selected != null) {
            @SuppressWarnings("unchecked")
            T template = (T) selected;
            m_description.setText(template.getDescription());
            m_description.setCaretPosition(0);

            boolean removeable = m_provider.isRemoveable(template);
            m_removeTemplateButton.setEnabled(removeable);
            m_applyTemplateButton.setEnabled(true);

            m_controller.setPreviewSettings(template);

            cl.show(m_previewPane, CARD_PREVIEW);
            this.validate();
        } else {
            m_description.setText("");
            m_removeTemplateButton.setEnabled(false);
            m_applyTemplateButton.setEnabled(false);
            cl.show(m_previewPane, CARD_EMPTY_SELECTION);

        }

    }

    private void updateTemplatesList(final String category) {
        Object selected = m_templates.getSelectedValue();
        DefaultListModel<T> model = (DefaultListModel<T>)m_templates.getModel();
        model.clear();
        for (T template : m_provider.getTemplates(
                m_metaCategories, category)) {
            model.addElement(template);
        }
        m_templates.setSelectedValue(selected, true);
    }

    /**
     * Renderer the name of Java Snippet templates.
     */
    private static class TemplateListCellRenderer<T extends JSnippetTemplate> extends
            DefaultListCellRenderer {
        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            @SuppressWarnings("unchecked")
            T m = (T)value;
            Component c = super.getListCellRendererComponent(list,
                    m.getName(),
                    index, isSelected, cellHasFocus);
            return c;
        }
    }

    private JPanel createTemplatesPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(6, 6, 4, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        p.add(new JLabel("Category"), c);
        c.gridy++;
        c.insets = new Insets(2, 6, 4, 6);
        p.add(m_categories, c);

        c.gridy++;
        c.insets = new Insets(6, 6, 4, 6);
        p.add(new JLabel("Title"), c);
        c.gridy++;
        c.weighty = 0.8;
        c.insets = new Insets(2, 6, 4, 6);
        JScrollPane manipScroller = new JScrollPane(m_templates);
        manipScroller.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        p.add(manipScroller, c);
        c.weighty = 0;


        c.gridy++;
        c.insets = new Insets(6, 6, 4, 6);
        p.add(new JLabel("Description"), c);
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(2, 6, 4, 6);
        m_description.setPreferredSize(m_description.getMinimumSize());
        JScrollPane descScroller = new JScrollPane(m_description);
        descScroller.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        descScroller.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descScroller.setPreferredSize(descScroller.getMinimumSize());
        p.add(descScroller, c);

        c.gridy++;
        c.insets = new Insets(2, 6, 4, 6);
        m_applyTemplateButton = new JButton("Apply");
        m_applyTemplateButton.setEnabled(false);
        m_applyTemplateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                T template =
                    m_templates.getSelectedValue();
                if (null != template) {
                    m_controller.setSettings(template);
                }
            }
        });
        m_removeTemplateButton = new JButton("Remove");
        m_removeTemplateButton.setEnabled(false);
        m_removeTemplateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                T template = m_templates.getSelectedValue();
                if (null != template) {
                    m_provider.removeTemplate(template);
                }
            }
        });


        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonsPanel.add(m_applyTemplateButton);
        buttonsPanel.add(m_removeTemplateButton);
        c.weightx = 1.0;
        p.add(buttonsPanel, c);

        return p;
    }
}

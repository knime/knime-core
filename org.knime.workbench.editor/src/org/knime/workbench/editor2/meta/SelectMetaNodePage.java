/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   07.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.knime.workbench.editor2.ImageRepository;


/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class SelectMetaNodePage extends WizardPage {

    private static final String TITLE = "Select MetaNode Template";
    private static final String DESCRIPTION = "If you want to create a MetaNode"
        + " with a usual number of data in and out ports, select one; \n"
        + "otherwise click next to define a custom MetaNode";

//    private Combo m_comboBox;

    static final String ZERO_ONE = "0:1";
    static final String ONE_ONE = "1:1";
    static final String ONE_TWO = "1:2";
    static final String TWO_ONE = "2:1";
    static final String TWO_TWO = "2:2";
    static final String CUSTOM = "custom";
    
    private Button m_btnZeroOne;
    private Button m_btnOneOne;
    private Button m_btnOneTwo;
    private Button m_btnTwoOne;
    private Button m_btnTwoTwo;
    private Button m_btnCustom;
    
    private final Map<Button, Image> m_activeIconMap 
        = new HashMap<Button, Image>();

    private final Map<Button, Image> m_inactiveIconMap 
        = new HashMap<Button, Image>();

    
    private Button m_selectedButton;
    
    private String m_selectedMetaNode;


    /**
     *
     */
    public SelectMetaNodePage() {
        super(TITLE);
        setTitle(TITLE);
        setDescription(DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    public void createControl(final Composite parent) { 
        Composite overall = new Composite(parent, SWT.NONE);
        overall.setLayout(new GridLayout(1, true));
        
        Composite buttonGrid = new Composite(overall, SWT.NONE);
        buttonGrid.setLayout(new GridLayout(3, true));
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalAlignment = GridData.CENTER;
        gridData.verticalAlignment = GridData.CENTER;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        overall.setLayoutData(gridData);
        buttonGrid.setLayoutData(gridData);
        m_btnZeroOne = new Button(buttonGrid, SWT.TOGGLE);
        m_btnZeroOne.setImage(ImageRepository.getImage(
                "/icons/meta/meta_0_1_inactive.png"));
        m_inactiveIconMap.put(m_btnZeroOne, ImageRepository.getImage(
            "/icons/meta/meta_0_1_inactive.png"));
        m_activeIconMap.put(m_btnZeroOne, ImageRepository.getImage(
            "/icons/meta/meta_0_1.png"));   
        m_btnZeroOne.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                m_selectedMetaNode = ZERO_ONE;
                changeSelection((Button)e.getSource());
                setPageComplete(true);
            }
            
        });
        m_btnOneOne = new Button(buttonGrid, SWT.TOGGLE);
        m_btnOneOne.setImage(ImageRepository.getImage(
                "/icons/meta/meta_1_1_inactive.png"));
        m_inactiveIconMap.put(m_btnOneOne, ImageRepository.getImage(
                "/icons/meta/meta_1_1_inactive.png"));
        m_activeIconMap.put(m_btnOneOne, ImageRepository.getImage(
                "/icons/meta/meta_1_1.png"));
        m_btnOneOne.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                m_selectedMetaNode = ONE_ONE;
                changeSelection((Button)e.getSource());
                setPageComplete(true);
            }
            
        });
        m_btnOneTwo = new Button(buttonGrid, SWT.TOGGLE);
        m_btnOneTwo.setImage(ImageRepository.getImage(
                "/icons/meta/meta_1_2_inactive.png"));
        m_inactiveIconMap.put(m_btnOneTwo, ImageRepository.getImage(
                "/icons/meta/meta_1_2_inactive.png"));
        m_activeIconMap.put(m_btnOneTwo,
                ImageRepository.getImage("/icons/meta/meta_1_2.png"));
        m_btnOneTwo.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
//                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                m_selectedMetaNode = ONE_TWO;
                changeSelection((Button)e.getSource());
                setPageComplete(true);
            }
            
        });
        m_btnTwoOne = new Button(buttonGrid, SWT.TOGGLE);
        m_btnTwoOne.setImage(ImageRepository.getImage(
                "/icons/meta/meta_2_1_inactive.png"));
                
        m_activeIconMap.put(m_btnTwoOne, ImageRepository.getImage(
                "/icons/meta/meta_2_1.png"));
        m_inactiveIconMap.put(m_btnTwoOne, ImageRepository.getImage(
                "/icons/meta/meta_2_1_inactive.png"));
        m_btnTwoOne.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
//                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                m_selectedMetaNode = TWO_ONE;
                changeSelection((Button)e.getSource());
                setPageComplete(true);
            }
            
        });
        m_btnTwoTwo = new Button(buttonGrid, SWT.TOGGLE);
        m_btnTwoTwo.setImage(ImageRepository.getImage(
                "/icons/meta/meta_2_2_inactive.png"));
        m_activeIconMap.put(m_btnTwoTwo, ImageRepository.getImage(
                "/icons/meta/meta_2_2.png"));                
        m_inactiveIconMap.put(m_btnTwoTwo, ImageRepository.getImage(
                "/icons/meta/meta_2_2_inactive.png"));
        m_btnTwoTwo.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
//                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                changeSelection((Button)e.getSource());
                m_selectedMetaNode = TWO_TWO;
                ((AddMetaNodePage)getNextPage()).setTemplate(
                        m_selectedMetaNode);
                setPageComplete(true);
            }
            
        });
        m_btnCustom = new Button(buttonGrid, SWT.TOGGLE);
        m_btnCustom.setImage(ImageRepository.getImage(
                "/icons/meta/custom_meta_inactive.png"));
        m_activeIconMap.put(m_btnCustom, ImageRepository.getImage(
                "/icons/meta/custom_meta.png"));
        m_inactiveIconMap.put(m_btnCustom, ImageRepository.getImage(
                "/icons/meta/custom_meta_inactive.png"));
        m_btnCustom.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(final SelectionEvent e) {
//                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e) {
                m_selectedMetaNode = CUSTOM;
                changeSelection((Button)e.getSource());
                setPageComplete(false);
            }
            
        });          
        setControl(overall);
    }
    
    private void changeSelection(final Button newSelection) {
        if (m_selectedButton != null) {
            m_selectedButton.setSelection(false);
            m_selectedButton.setImage(m_inactiveIconMap.get(m_selectedButton));
        }
        newSelection.setSelection(true);
        newSelection.setImage(m_activeIconMap.get(newSelection));
        m_selectedButton = newSelection;
        ((AddMetaNodePage)getNextPage()).setTemplate(m_selectedMetaNode);
    }
    
    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isPageComplete() {
        // TODO: replace the last check -> makes no sense 
        // rather check for nr of ports 
        return m_selectedMetaNode != null && m_selectedMetaNode != CUSTOM;
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFlipToNextPage() {
        return m_selectedMetaNode != null; 
    }

    String getSelectedMetaNode() {
        return m_selectedMetaNode;
    }



}

/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.ui.metainfo.editor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.metainfo.model.MetaGUIElement;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class WorkflowSetMetaInfoEditor extends EditorPart {
    
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowSetMetaInfoEditor.class); 
    
    private FormToolkit m_toolkit;
    private ScrolledForm m_form;
    
    private List<MetaGUIElement>m_elements = new ArrayList<MetaGUIElement>();
    
    private boolean m_isDirty = false;
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        m_toolkit = new FormToolkit(parent.getDisplay());
        m_form = m_toolkit.createScrolledForm(parent);
        m_form.setText(getPartName());
        GridLayout layout = new GridLayout();
        layout.numColumns = 2; 
        layout.horizontalSpacing = 20;
        layout.makeColumnsEqualWidth = false;
        m_form.getBody().setLayout(layout);
        
        GridData layoutData = new GridData();
        
        layoutData.minimumWidth = 100;
        layoutData.widthHint = 100;
        layoutData.verticalAlignment = SWT.TOP;
        for (MetaGUIElement element : m_elements) {
            LOGGER.debug("element " + element.getLabel());
            Label  label = m_toolkit.createLabel(m_form.getBody(), 
                    element.getLabel() + ": ");
            label.setLayoutData(layoutData);
            element.createGUIElement(m_toolkit, m_form.getBody());
            element.addListener(new ModifyListener() {
                
                @Override
                public void modifyText(final ModifyEvent e) {
                    setDirty(true);
                }
            });
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        try {
            IPath path = ((IPathEditorInput)getEditorInput()).getPath();
            File inputFile = path.toFile();

            SAXTransformerFactory fac 
                = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = fac.newTransformerHandler();

            Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            OutputStream out = new FileOutputStream(inputFile);
            handler.setResult(new StreamResult(out));

            handler.startDocument();
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "nrOfElements", "CDATA", ""
                    + m_elements.size());
            handler.startElement(null, null, "KNIMEMetaInfo", atts);

            monitor.beginTask("Saving meta information...", m_elements.size());
            for (MetaGUIElement element : m_elements) {
                element.saveTo(handler);
                monitor.worked(1);
            }

            handler.endElement(null, null, "KNIMEMetaInfo");
            handler.endDocument();
            out.close();
            setDirty(false);
        } catch (Exception e) {
            LOGGER.error("An error ocurred while saving "
                    + getEditorInput().toString(), e);
        } finally {
            monitor.done();
        }
    }

    /**
     * 
     * @param isDirty true if the editor should be set to dirty
     */
    public void setDirty(final boolean isDirty) {
        m_isDirty = isDirty;
        firePropertyChange(PROP_DIRTY);
    }


    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void doSaveAs() {
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input)
            throws PartInitException {
        super.setSite(site);
        super.setInput(input);
        if (!(input instanceof IPathEditorInput)) {
            throw new PartInitException("Unexpected input for " 
                    + getClass().getName() + ": "
                    + input.getName());
        }
        IPath path = ((IPathEditorInput)input).getPath();
        File inputFile = path.toFile();
        setPartName(new File(inputFile.getParent()).getName() 
                + " : Meta Information");
        m_elements = parseInput(inputFile);
        LOGGER.debug("input = " + input.toString());
    }
    
    private List<MetaGUIElement>parseInput(final File inputFile) 
        throws PartInitException {
        MetaInfoInputHandler hdl = new MetaInfoInputHandler();
        try {
            SAXParserFactory saxFac = SAXParserFactory.newInstance();
            SAXParser parser = saxFac.newSAXParser();
            parser.parse(inputFile, hdl);
        } catch (Exception e) {
            String msg = "Error while parsing input file " 
                + inputFile.getName();
            LOGGER.error(msg, e);
            throw new PartInitException(msg, e);
        }
        return hdl.getElements(); 
    }
        

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return m_isDirty;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        m_form.setFocus();
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        m_toolkit.dispose();
        super.dispose();
    }

}

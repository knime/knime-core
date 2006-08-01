/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */ 
package embedding.model;

//Java
import java.util.Iterator;
import java.io.IOException;

//SAX
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import embedding.tools.AbstractObjectReader;

/**
 * XMLReader implementation for the ProjectTeam class. This class is used to
 * generate SAX events from the ProjectTeam class.
 */
public class ProjectTeamXMLReader extends AbstractObjectReader {

    /**
     * @see org.xml.sax.XMLReader#parse(InputSource)
     */
    public void parse(InputSource input) throws IOException, SAXException {
        if (input instanceof ProjectTeamInputSource) {
            parse(((ProjectTeamInputSource)input).getProjectTeam());
        } else {
            throw new SAXException("Unsupported InputSource specified. Must be a ProjectTeamInputSource");
        }
    }


    /**
     * Starts parsing the ProjectTeam object.
     * @param projectTeam The object to parse
     * @throws SAXException In case of a problem during SAX event generation
     */
    public void parse(ProjectTeam projectTeam) throws SAXException {
        if (projectTeam == null) {
            throw new NullPointerException("Parameter projectTeam must not be null");
        }
        if (handler == null) {
            throw new IllegalStateException("ContentHandler not set");
        }
        
        //Start the document
        handler.startDocument();
        
        //Generate SAX events for the ProjectTeam
        generateFor(projectTeam);
        
        //End the document
        handler.endDocument();        
    }

    
    /**
     * Generates SAX events for a ProjectTeam object.
     * @param projectTeam ProjectTeam object to use
     * @throws SAXException In case of a problem during SAX event generation
     */
    protected void generateFor(ProjectTeam projectTeam) throws SAXException {
        if (projectTeam == null) {
            throw new NullPointerException("Parameter projectTeam must not be null");
        }
        if (handler == null) {
            throw new IllegalStateException("ContentHandler not set");
        }
        
        handler.startElement("projectteam");
        handler.element("projectname", projectTeam.getProjectName());
        Iterator i = projectTeam.getMembers().iterator();
        while (i.hasNext()) {
            ProjectMember member = (ProjectMember)i.next();
            generateFor(member);
        }
        handler.endElement("projectteam");
    }

    /**
     * Generates SAX events for a ProjectMember object.
     * @param projectMember ProjectMember object to use
     * @throws SAXException In case of a problem during SAX event generation
     */
    protected void generateFor(ProjectMember projectMember) throws SAXException {
        if (projectMember == null) {
            throw new NullPointerException("Parameter projectMember must not be null");
        }
        if (handler == null) {
            throw new IllegalStateException("ContentHandler not set");
        }
        
        handler.startElement("member");
        handler.element("name", projectMember.getName());
        handler.element("function", projectMember.getFunction());
        handler.element("email", projectMember.getEmail());
        handler.endElement("member");
    }

}

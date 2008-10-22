package fi.csc.microarray.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author  Aleksi Kallio
 */
public class XmlUtil {
    private DocumentBuilder docBuilder;
    private static XmlUtil instance;
    
    public static synchronized XmlUtil getInstance() throws ParserConfigurationException {
        if (instance == null) 
            instance = new XmlUtil();
        
        return instance;
    }
        
    private XmlUtil() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }
    
    public Document newDocument() throws ParserConfigurationException {
        return docBuilder.newDocument();
    }
    
    public Document parseFile(File file) throws org.xml.sax.SAXException, IOException {
        return docBuilder.parse(file);
    }
    
    public Document parseReader(Reader reader) throws org.xml.sax.SAXException, IOException {
        return docBuilder.parse(new org.xml.sax.InputSource(reader));
    }
    
    public void printXml(Document xml, Writer out) throws TransformerException, UnsupportedEncodingException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");        
        
        transformer.transform(new DOMSource(xml), new StreamResult(out));        
    }
    
	public Element getChildWithAttribute(Element parent, String attrName, String attrValue) {
		NodeList childNodes = parent.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element element = (Element)node;
				if (attrValue.equals(element.getAttribute(attrName))) {
					return element;
				}
			}
		}
		return null;
	}

}

package com.ldap.desafio.services;

import com.unboundid.ldap.sdk.LDAPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class XMLProcessorService {

    private final LDAPService ldapService;

    @Autowired
    public XMLProcessorService(LDAPService ldapService) {
        this.ldapService = ldapService;
    }

    public void processXML(MultipartFile file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream inputStream = file.getInputStream();
        Document document = builder.parse(inputStream);

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        String rootElement = xPath.evaluate("name(/*)", document);
        if (rootElement.equals("add")) {
            processAdd(document, xPath);
        } else if (rootElement.equals("modify")) {
            processModify(document, xPath);
        } else {
            throw new Exception("Unsupported XML operation: " + rootElement);
        }
    }

    private void processAdd(Document document, XPath xPath) throws XPathExpressionException, LDAPException {
        String className = xPath.evaluate("/add/@class-name", document);
        Map<String, Object> attributes = new HashMap<>();

        NodeList attrNodes = (NodeList) xPath.evaluate("/add/add-attr", document, XPathConstants.NODESET);
        for (int i = 0; i < attrNodes.getLength(); i++) {
            Node attrNode = attrNodes.item(i);
            String attrName = xPath.evaluate("@attr-name", attrNode);

            NodeList valueNodes = (NodeList) xPath.evaluate("value", attrNode, XPathConstants.NODESET);
            if (valueNodes.getLength() > 1) {
                List<String> values = new ArrayList<>();
                for (int j = 0; j < valueNodes.getLength(); j++) {
                    values.add(valueNodes.item(j).getTextContent());
                }
                attributes.put(attrName, values);
            } else {
                String value = valueNodes.item(0).getTextContent();
                attributes.put(attrName, value);
            }
        }

        ldapService.addEntry(className, attributes);
    }

    private void processModify(Document document, XPath xPath) throws XPathExpressionException, LDAPException {
        String uid = xPath.evaluate("/modify/association", document);
        Map<String, List<String>> modifications = new HashMap<>();

        NodeList attrNodes = (NodeList) xPath.evaluate("/modify/modify-attr", document, XPathConstants.NODESET);
        for (int i = 0; i < attrNodes.getLength(); i++) {
            Node attrNode = attrNodes.item(i);
            String attrName = xPath.evaluate("@attr-name", attrNode);

            if (attrName.equals("Grupo")) {
                attrName = "o";
            }

            List<String> values = new ArrayList<>();
            NodeList removeNodes = (NodeList) xPath.evaluate("remove-value", attrNode, XPathConstants.NODESET);
            for (int j = 0; j < removeNodes.getLength(); j++) {
                values.add(removeNodes.item(j).getTextContent());
            }

            NodeList addNodes = (NodeList) xPath.evaluate("add-value", attrNode, XPathConstants.NODESET);
            for (int j = 0; j < addNodes.getLength(); j++) {
                values.add(addNodes.item(j).getTextContent());
            }

            modifications.put(attrName, values);
        }

        ldapService.modifyEntry(uid, modifications);
    }
}
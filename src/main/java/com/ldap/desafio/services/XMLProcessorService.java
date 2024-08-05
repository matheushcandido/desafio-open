package com.ldap.desafio.services;

import com.unboundid.ldap.sdk.LDAPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

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

        String rootElement = document.getDocumentElement().getNodeName();
        if (rootElement.equals("add")) {
            processAdd(document);
        } else if (rootElement.equals("modify")) {
            processModify(document);
        } else {
            throw new Exception("Unsupported XML operation: " + rootElement);
        }
    }

    private void processAdd(Document document) throws LDAPException {
        String className = document.getDocumentElement().getAttribute("class-name");
        Map<String, Object> attributes = new HashMap<>();

        NodeList attrNodes = document.getElementsByTagName("add-attr");
        for (int i = 0; i < attrNodes.getLength(); i++) {
            Element attrElement = (Element) attrNodes.item(i);
            String attrName = attrElement.getAttribute("attr-name");

            NodeList valueNodes = attrElement.getElementsByTagName("value");
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

    private void processModify(Document document) throws LDAPException {
        String uid = document.getDocumentElement().getElementsByTagName("association").item(0).getTextContent();

        Map<String, List<String>> modifications = new HashMap<>();

        NodeList attrNodes = document.getElementsByTagName("modify-attr");
        for (int i = 0; i < attrNodes.getLength(); i++) {
            Element attrElement = (Element) attrNodes.item(i);
            String attrName = attrElement.getAttribute("attr-name");

            if (attrName.equals("Grupo")) {
                attrName = "o";
            }

            List<String> values = new ArrayList<>();
            NodeList removeNodes = attrElement.getElementsByTagName("remove-value");
            for (int j = 0; j < removeNodes.getLength(); j++) {
                values.add(removeNodes.item(j).getTextContent());
            }

            NodeList addNodes = attrElement.getElementsByTagName("add-value");
            for (int j = 0; j < addNodes.getLength(); j++) {
                values.add(addNodes.item(j).getTextContent());
            }

            modifications.put(attrName, values);
        }

        ldapService.modifyEntry(uid, modifications);
    }
}
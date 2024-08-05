package com.ldap.desafio.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.unboundid.ldap.sdk.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class LDAPService {

    @Value("${ldap.host}")
    private String ldapHost;

    @Value("${ldap.port}")
    private int ldapPort;

    @Value("${ldap.admin.dn}")
    private String adminDn;

    @Value("${ldap.admin.password}")
    private String adminPassword;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 .'-]+$");
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9 .'-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\(\\d{2}\\) \\d{4,5}-\\d{4}$");

    public void addEntry(String className, Map<String, Object> attributes) throws LDAPException {
        try (LDAPConnection connection = new LDAPConnection(ldapHost, ldapPort, adminDn, adminPassword)) {
            ensureOuExists(connection, "groups");
            ensureOuExists(connection, "users");

            String dn = createDN(className, attributes);
            Entry entry = new Entry(dn);

            if (className.equalsIgnoreCase("Grupo")) {
                entry.addAttribute("objectClass", "organization");
                entry.addAttribute("o", (String) attributes.get("Identificador"));
                entry.addAttribute("description", (String) attributes.get("Descricao"));
            } else if (className.equalsIgnoreCase("Usuario")) {
                String nome = (String) attributes.get("Nome Completo");
                String login = (String) attributes.get("Login");
                String telefone = (String) attributes.get("Telefone");

                if (!NAME_PATTERN.matcher(nome).matches()) {
                    throw new IllegalArgumentException("Nome inválido: " + nome);
                }
                if (!LOGIN_PATTERN.matcher(login).matches()) {
                    throw new IllegalArgumentException("Login inválido: " + login);
                }
                if (!PHONE_PATTERN.matcher(telefone).matches()) {
                    throw new IllegalArgumentException("Telefone inválido: " + telefone);
                }

                entry.addAttribute("objectClass", "inetOrgPerson");
                entry.addAttribute("cn", nome);
                entry.addAttribute("sn", nome);
                entry.addAttribute("uid", login);
                entry.addAttribute("telephoneNumber", telefone);

                if (attributes.containsKey("Grupo")) {
                    @SuppressWarnings("unchecked")
                    List<String> grupos = (List<String>) attributes.get("Grupo");
                    for (String grupo : grupos) {
                        entry.addAttribute("o", grupo);
                    }
                }
            }

            connection.add(entry);
        }
    }

    public void modifyEntry(String uid, Map<String, List<String>> modifications) throws LDAPException {
        try (LDAPConnection connection = new LDAPConnection(ldapHost, ldapPort, adminDn, adminPassword)) {
            String dn = "uid=" + uid + ",ou=users,dc=mycpu,dc=com";
            List<Modification> modList = new ArrayList<>();

            for (Map.Entry<String, List<String>> entry : modifications.entrySet()) {
                String attrName = entry.getKey();
                List<String> values = entry.getValue();

                modList.add(new Modification(ModificationType.REPLACE, attrName, values.toArray(new String[0])));
            }

            connection.modify(dn, modList);
        }
    }

    private void ensureOuExists(LDAPConnection connection, String ouName) throws LDAPException {
        String ouDn = "ou=" + ouName + ",dc=mycpu,dc=com";
        try {
            connection.search(ouDn, SearchScope.BASE, "(objectClass=*)");
        } catch (LDAPSearchException e) {
            if (e.getResultCode() == ResultCode.NO_SUCH_OBJECT) {
                Entry ouEntry = new Entry(ouDn);
                ouEntry.addAttribute("objectClass", "organizationalUnit");
                ouEntry.addAttribute("ou", ouName);
                connection.add(ouEntry);
            } else {
                throw e;
            }
        }
    }

    private String createDN(String className, Map<String, Object> attributes) {
        if (className.equalsIgnoreCase("Usuario")) {
            return "uid=" + attributes.get("Login") + ",ou=users,dc=mycpu,dc=com";
        } else if (className.equalsIgnoreCase("Grupo")) {
            return "o=" + attributes.get("Identificador") + ",ou=groups,dc=mycpu,dc=com";
        }
        return null;
    }
}
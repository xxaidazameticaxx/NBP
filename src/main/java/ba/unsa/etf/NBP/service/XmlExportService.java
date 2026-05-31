package ba.unsa.etf.NBP.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Exports one or more database tables into an XML document.
 * <p>
 * Uses streaming JDBC + StAX to avoid loading entire tables into memory.
 */
@Service
public class XmlExportService {

    private static final String TABLE_NAME_PATTERN = "^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)?$";

    private final JdbcTemplate jdbcTemplate;

    public XmlExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Generates an XML export for the given tables.
     *
     * @param tableNames list of table names, optionally schema-qualified (e.g. {@code NBP_STUDENT} or {@code NBP.NBP_STUDENT})
     * @return UTF-8 encoded XML bytes
     */
    public byte[] exportTables(List<String> tableNames) {
        Set<String> unique = normalizeAndValidateTableNames(tableNames);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(16 * 1024);
            XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(outputStream, StandardCharsets.UTF_8.name());

            writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            writer.writeStartElement("export");
            writer.writeAttribute("generatedAt", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            for (String tableName : unique) {
                writeTableViaDbmsXmlGen(writer, tableName);
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();

            return outputStream.toByteArray();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Failed to generate XML export", e);
        }
    }

    private void writeTableViaDbmsXmlGen(XMLStreamWriter writer, String tableName) throws XMLStreamException {
        assertTableExists(tableName);

        writer.writeStartElement("table");
        writer.writeAttribute("name", tableName);

        String tableXml = fetchOracleTableXml(tableName);
        copyXmlFragment(writer, tableXml);

        writer.writeEndElement();
    }

    /**
     * Uses Oracle DBMS_XMLGEN to generate XML for {@code SELECT * FROM <table>}.
     * <p>
     * The input {@code tableName} is validated before calling this method.
     */
    private String fetchOracleTableXml(String tableName) {
        // DBMS_XMLGEN.getxml returns a CLOB.
        String sql = "SELECT DBMS_XMLGEN.getxml('SELECT * FROM " + tableName + "') AS XML FROM dual";
        Clob clob = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getClob("XML"));
        if (clob == null) {
            return "";
        }

        try {
            long length = clob.length();
            if (length <= 0) {
                return "";
            }
            int max = (int) Math.min(length, 25_000_000L);
            return clob.getSubString(1, max);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read XML CLOB for table: " + tableName, e);
        }
    }

    /**
     * Copies an XML fragment into the current output stream without escaping it.
     * Skips START_DOCUMENT/END_DOCUMENT events if present.
     */
    private void copyXmlFragment(XMLStreamWriter writer, String xmlFragment) throws XMLStreamException {
        if (xmlFragment == null || xmlFragment.isBlank()) {
            return;
        }

        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        // Harden parser: DBMS_XMLGEN output should not require DTD/entities.
        try {
            inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        } catch (IllegalArgumentException ignored) {
            // Factory might not support this property.
        }
        try {
            inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        } catch (IllegalArgumentException ignored) {
            // Factory might not support this property.
        }
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(xmlFragment));
        try {
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamReader.START_ELEMENT -> {
                        String localName = reader.getLocalName();
                        String namespaceUri = reader.getNamespaceURI();

                        if (namespaceUri != null && !namespaceUri.isBlank()) {
                            String prefix = reader.getPrefix();
                            if (prefix == null) {
                                prefix = "";
                            }
                            writer.writeStartElement(prefix, localName, namespaceUri);
                        } else {
                            writer.writeStartElement(localName);
                        }

                        for (int i = 0; i < reader.getNamespaceCount(); i++) {
                            String nsPrefix = reader.getNamespacePrefix(i);
                            String nsUri = reader.getNamespaceURI(i);
                            if (nsPrefix == null) {
                                writer.writeDefaultNamespace(nsUri);
                            } else {
                                writer.writeNamespace(nsPrefix, nsUri);
                            }
                        }

                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String attrNamespace = reader.getAttributeNamespace(i);
                            String attrLocalName = reader.getAttributeLocalName(i);
                            String attrValue = reader.getAttributeValue(i);

                            if (attrNamespace != null && !attrNamespace.isBlank()) {
                                String attrPrefix = reader.getAttributePrefix(i);
                                if (attrPrefix == null) {
                                    attrPrefix = "";
                                }
                                writer.writeAttribute(attrPrefix, attrNamespace, attrLocalName, attrValue);
                            } else {
                                writer.writeAttribute(attrLocalName, attrValue);
                            }
                        }
                    }
                    case XMLStreamReader.CHARACTERS, XMLStreamReader.CDATA -> writer.writeCharacters(reader.getText());
                    case XMLStreamReader.END_ELEMENT -> writer.writeEndElement();
                    default -> {
                        // Ignore: comments, processing instructions, etc.
                    }
                }
            }
        } finally {
            try {
                reader.close();
            } catch (Exception ignored) {
                // no-op
            }
        }
    }

    private Set<String> normalizeAndValidateTableNames(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            throw new IllegalArgumentException("Query parameter 'tables' is required");
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String raw : tableNames) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String trimmed = raw.trim();
            if (!trimmed.matches(TABLE_NAME_PATTERN)) {
                throw new IllegalArgumentException("Invalid table name: " + trimmed);
            }

            // Normalize to the database's common convention (Oracle stores unquoted identifiers uppercased)
            unique.add(trimmed.toUpperCase(Locale.ROOT));
        }

        if (unique.isEmpty()) {
            throw new IllegalArgumentException("Query parameter 'tables' is required");
        }

        return unique;
    }

    private void assertTableExists(String tableName) {
        try {
            Boolean exists = jdbcTemplate.execute((ConnectionCallback<Boolean>) (conn) -> {
                DatabaseMetaData meta = conn.getMetaData();

                String schema = null;
                String name = tableName;
                int dot = tableName.indexOf('.');
                if (dot > 0) {
                    schema = tableName.substring(0, dot);
                    name = tableName.substring(dot + 1);
                }

                // Try provided schema, otherwise try current schema/user.
                if (schema == null || schema.isBlank()) {
                    schema = conn.getSchema();
                    if (schema == null || schema.isBlank()) {
                        schema = meta.getUserName();
                    }
                }

                // Some drivers are picky about case; use uppercase for Oracle-style identifiers.
                String schemaPattern = schema == null ? null : schema.toUpperCase(Locale.ROOT);
                String tablePattern = name.toUpperCase(Locale.ROOT);

                try (ResultSet rs = meta.getTables(null, schemaPattern, tablePattern, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        return true;
                    }
                }

                // As a fallback, try without schema restriction.
                try (ResultSet rs = meta.getTables(null, null, tablePattern, new String[]{"TABLE"})) {
                    return rs.next();
                }
            });

            if (!Boolean.TRUE.equals(exists)) {
                throw new IllegalArgumentException("Unknown table: " + tableName);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalStateException("Failed to validate table: " + tableName, e);
        }
    }

    private String formatValue(Object value) {
        Objects.requireNonNull(value, "value");

        if (value instanceof Clob clob) {
            try {
                long length = clob.length();
                if (length == 0) {
                    return "";
                }
                // Guard against extremely large CLOBs.
                int max = (int) Math.min(length, 1_000_000L);
                return clob.getSubString(1, max);
            } catch (SQLException e) {
                return String.valueOf(value);
            }
        }

        // Covers numbers, timestamps, dates, enums, etc.
        return String.valueOf(value);
    }
}

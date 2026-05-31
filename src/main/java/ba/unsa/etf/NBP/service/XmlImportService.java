package ba.unsa.etf.NBP.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Types;

@Service
public class XmlImportService {

    private final JdbcTemplate jdbcTemplate;

    public XmlImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String importXml(byte[] xmlBytes) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new IllegalArgumentException("XML input is empty");
        }

        String xmlString = new String(xmlBytes, StandardCharsets.UTF_8);

        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("{call IMPORT_TABLES_FROM_XML(?,?)}")) {
                Clob clob = conn.createClob();
                clob.setString(1, xmlString);
                cs.setClob(1, clob);
                cs.registerOutParameter(2, Types.VARCHAR);
                cs.execute();
                String result = cs.getString(2);
                clob.free();
                return result;
            }
        });
    }
}

-- Merges rows from an export-format XML document into NBP tables.
-- Expects the same structure produced by GET /export/xml (DBMS_XMLGEN ROWSET/ROW fragments).
--
-- Merge semantics: INSERT when PK is new, UPDATE when PK already exists.
-- Tables are processed in FK-safe order; unrecognised tables are processed last with a warning.
--
-- Limitations:
--   - Does not import NBP.NBP_USER rows; professor/student rows require an existing USER_ID.
--   - Match is on primary key only.

CREATE OR REPLACE PROCEDURE IMPORT_TABLES_FROM_XML (
  p_xml    IN  CLOB,
  p_result OUT VARCHAR2
)
AUTHID CURRENT_USER
AS
  TYPE t_table_map IS TABLE OF XMLTYPE INDEX BY VARCHAR2(200);

  c_table_order CONSTANT SYS.ODCIVARCHAR2LIST := SYS.ODCIVARCHAR2LIST(
    'NBP_ROOM',
    'NBP_DEPARTMENT',
    'NBP_STUDY_PROGRAM',
    'NBP_PROFESSOR',
    'NBP_STUDENT',
    'NBP_COURSE',
    'NBP_TIMETABLE',
    'NBP_COURSE_SESSION',
    'NBP_ENROLLMENT',
    'NBP_ATTENDANCE',
    'NBP_ABSENCE_EXCUSE',
    'NBP_NOTIFICATION',
    'NBP_USER_SESSION',
    'NBP_REPORT'
  );

  c_name_pattern CONSTANT VARCHAR2(200) := '^[A-Z][A-Z0-9_]*(\.[A-Z][A-Z0-9_]*)?$';

  v_tables        t_table_map;
  v_table_key     VARCHAR2(200);
  v_rows          NUMBER;
  v_total_rows    NUMBER := 0;
  v_summary       VARCHAR2(32767) := '';
  v_warnings      VARCHAR2(4000) := '';
  v_owner         VARCHAR2(128);
  v_table         VARCHAR2(128);
  v_qualified     VARCHAR2(256);
  v_xml_doc       XMLTYPE;
  v_processed     NUMBER := 0;

  ---------------------------------------------------------------------------
  PROCEDURE append_summary(p_line VARCHAR2) IS
  BEGIN
    IF v_summary IS NOT NULL AND LENGTH(v_summary) > 0 THEN
      v_summary := v_summary || CHR(10);
    END IF;
    v_summary := v_summary || p_line;
  END append_summary;

  ---------------------------------------------------------------------------
  PROCEDURE parse_qualified_name(
    p_qualified IN     VARCHAR2,
    p_owner     OUT    VARCHAR2,
    p_table     OUT    VARCHAR2,
    p_full      OUT    VARCHAR2
  ) IS
    v_dot PLS_INTEGER;
    v_raw VARCHAR2(200) := UPPER(TRIM(p_qualified));
  BEGIN
    IF v_raw IS NULL OR LENGTH(v_raw) = 0 THEN
      RAISE_APPLICATION_ERROR(-20010, 'Table name is required');
    END IF;

    IF NOT REGEXP_LIKE(v_raw, c_name_pattern) THEN
      RAISE_APPLICATION_ERROR(-20011, 'Invalid table name: ' || p_qualified);
    END IF;

    v_dot := INSTR(v_raw, '.');
    IF v_dot > 0 THEN
      p_owner := SUBSTR(v_raw, 1, v_dot - 1);
      p_table := SUBSTR(v_raw, v_dot + 1);
    ELSE
      p_owner := USER;
      p_table := v_raw;
    END IF;

    p_full := p_owner || '.' || p_table;
  END parse_qualified_name;

  ---------------------------------------------------------------------------
  PROCEDURE assert_table_exists(p_owner VARCHAR2, p_table VARCHAR2) IS
    v_cnt NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_cnt
      FROM all_tables
     WHERE owner = p_owner
       AND table_name = p_table;

    IF v_cnt = 0 THEN
      RAISE_APPLICATION_ERROR(-20012, 'Unknown table: ' || p_owner || '.' || p_table);
    END IF;
  END assert_table_exists;

  ---------------------------------------------------------------------------
  PROCEDURE merge_table_fragment(
    p_qualified IN VARCHAR2,
    p_owner     IN VARCHAR2,
    p_table     IN VARCHAR2,
    p_fragment  IN XMLTYPE
  ) IS
    v_xmltable_cols  VARCHAR2(32767) := '';
    v_insert_cols    VARCHAR2(32767) := '';
    v_insert_vals    VARCHAR2(32767) := '';
    v_update_set     VARCHAR2(32767) := '';
    v_on_clause      VARCHAR2(32767) := '';
    v_merge_sql      VARCHAR2(32767);

    TYPE t_str_set IS TABLE OF VARCHAR2(128) INDEX BY VARCHAR2(128);
    v_pk_cols t_str_set;

    -- Map Oracle column type to a safe XMLTABLE extraction type.
    -- NUMBER stays NUMBER; CLOBs stay CLOB; everything else becomes VARCHAR2(4000)
    -- so Oracle's implicit conversion handles DATE/TIMESTAMP/CHAR/etc.
    FUNCTION col_xml_type(p_type IN VARCHAR2) RETURN VARCHAR2 IS
    BEGIN
      IF    p_type = 'NUMBER'            THEN RETURN 'NUMBER';
      ELSIF p_type IN ('CLOB', 'NCLOB') THEN RETURN 'CLOB';
      ELSE                                    RETURN 'VARCHAR2(4000)';
      END IF;
    END col_xml_type;

  BEGIN
    IF p_fragment IS NULL THEN
      append_summary(p_qualified || ': skipped (empty fragment)');
      RETURN;
    END IF;

    IF p_fragment.existsNode('//ROW') = 0 THEN
      append_summary(p_qualified || ': skipped (no rows)');
      RETURN;
    END IF;

    -- Collect PK column names for the MERGE ON clause
    FOR pk IN (
      SELECT acc.column_name
        FROM all_constraints ac
        JOIN all_cons_columns acc
          ON acc.owner = ac.owner AND acc.constraint_name = ac.constraint_name
       WHERE ac.owner = p_owner AND ac.table_name = p_table AND ac.constraint_type = 'P'
       ORDER BY acc.position
    ) LOOP
      v_pk_cols(pk.column_name) := pk.column_name;
    END LOOP;

    -- Build MERGE components from column metadata
    FOR col IN (
      SELECT column_name, data_type
        FROM all_tab_columns
       WHERE owner = p_owner AND table_name = p_table
       ORDER BY column_id
    ) LOOP
      v_xmltable_cols := v_xmltable_cols
        || col.column_name || ' ' || col_xml_type(col.data_type)
        || ' PATH ''' || col.column_name || ''', ';
      v_insert_cols := v_insert_cols || col.column_name || ', ';
      v_insert_vals := v_insert_vals || 's.' || col.column_name || ', ';

      IF v_pk_cols.EXISTS(col.column_name) THEN
        v_on_clause := v_on_clause
          || 't.' || col.column_name || ' = s.' || col.column_name || ' AND ';
      ELSE
        v_update_set := v_update_set
          || 't.' || col.column_name || ' = s.' || col.column_name || ', ';
      END IF;
    END LOOP;

    -- RTRIM is safe for ', ' (chars not in column names).
    -- For ' AND ' we use SUBSTR because RTRIM treats its 2nd arg as a character SET,
    -- which would corrupt column names containing A, N, or D (e.g. "ID" → "I").
    v_xmltable_cols := RTRIM(v_xmltable_cols, ', ');
    v_insert_cols   := RTRIM(v_insert_cols,   ', ');
    v_insert_vals   := RTRIM(v_insert_vals,   ', ');
    v_update_set    := RTRIM(v_update_set,    ', ');
    IF v_on_clause IS NOT NULL AND LENGTH(v_on_clause) >= 5 THEN
      v_on_clause := SUBSTR(v_on_clause, 1, LENGTH(v_on_clause) - 5); -- strip trailing ' AND '
    END IF;

    -- Dynamic MERGE with XMLTABLE avoids the ORA-01400 NULL bug in DBMS_XMLSTORE
    -- (XMLSTORE loses text-node values for XMLType fragments extracted from a parent doc).
    IF v_on_clause IS NULL OR LENGTH(v_on_clause) = 0 THEN
      -- No PK found: plain INSERT SELECT
      EXECUTE IMMEDIATE
        'INSERT INTO ' || p_qualified
        || ' (' || v_insert_cols || ') '
        || 'SELECT ' || v_insert_cols
        || ' FROM XMLTABLE(''//ROW'' PASSING :xml COLUMNS ' || v_xmltable_cols || ')'
        USING p_fragment;
    ELSE
      v_merge_sql :=
        'MERGE INTO ' || p_qualified || ' t '
        || 'USING (SELECT ' || v_insert_cols
        || ' FROM XMLTABLE(''//ROW'' PASSING :xml COLUMNS ' || v_xmltable_cols || ')) s '
        || 'ON (' || v_on_clause || ') ';

      IF v_update_set IS NOT NULL THEN
        v_merge_sql := v_merge_sql
          || 'WHEN MATCHED THEN UPDATE SET ' || v_update_set || ' ';
      END IF;

      v_merge_sql := v_merge_sql
        || 'WHEN NOT MATCHED THEN INSERT (' || v_insert_cols
        || ') VALUES (' || v_insert_vals || ')';

      EXECUTE IMMEDIATE v_merge_sql USING p_fragment;
    END IF;

    v_rows := SQL%ROWCOUNT;
    v_total_rows := v_total_rows + NVL(v_rows, 0);
    append_summary(p_qualified || ': merged ' || NVL(v_rows, 0) || ' row(s)');
  END merge_table_fragment;

  ---------------------------------------------------------------------------
  PROCEDURE process_table(p_qualified VARCHAR2) IS
  BEGIN
    v_table_key := UPPER(TRIM(p_qualified));
    IF NOT v_tables.EXISTS(v_table_key) THEN
      RETURN;
    END IF;

    parse_qualified_name(v_table_key, v_owner, v_table, v_qualified);
    assert_table_exists(v_owner, v_table);
    merge_table_fragment(v_qualified, v_owner, v_table, v_tables(v_table_key));
    v_tables.DELETE(v_table_key);
    v_processed := v_processed + 1;
  END process_table;

BEGIN
  p_result := NULL;

  IF p_xml IS NULL OR DBMS_LOB.GETLENGTH(p_xml) = 0 THEN
    RAISE_APPLICATION_ERROR(-20001, 'XML input is empty');
  END IF;

  v_xml_doc := XMLTYPE(p_xml);

  IF v_xml_doc.existsNode('/export') = 0 THEN
    RAISE_APPLICATION_ERROR(-20002, 'Root element must be <export>');
  END IF;

  FOR rec IN (
    SELECT UPPER(TRIM(x.table_name)) AS table_name,
           x.table_xml
      FROM XMLTABLE(
             '/export/table'
             PASSING v_xml_doc
             COLUMNS
               table_name VARCHAR2(200) PATH '@name',
               table_xml  XMLTYPE PATH 'ROWSET'
           ) x
     WHERE x.table_name IS NOT NULL
  ) LOOP
    parse_qualified_name(rec.table_name, v_owner, v_table, v_qualified);
    assert_table_exists(v_owner, v_table);

    IF v_tables.EXISTS(rec.table_name) THEN
      RAISE_APPLICATION_ERROR(-20013, 'Duplicate table in XML: ' || rec.table_name);
    END IF;

    v_tables(rec.table_name) := rec.table_xml;
  END LOOP;

  IF v_tables.COUNT = 0 THEN
    RAISE_APPLICATION_ERROR(-20003, 'No <table> elements found in XML');
  END IF;

  FOR i IN 1 .. c_table_order.COUNT LOOP
    process_table(c_table_order(i));
  END LOOP;

  v_table_key := v_tables.FIRST;
  WHILE v_table_key IS NOT NULL LOOP
    v_warnings := v_warnings || v_table_key || ', ';
    process_table(v_table_key);
    v_table_key := v_tables.FIRST;
  END LOOP;

  IF v_warnings IS NOT NULL AND LENGTH(v_warnings) > 0 THEN
    v_warnings := RTRIM(v_warnings, ', ');
    append_summary('Warning: processed unordered table(s): ' || v_warnings);
  END IF;

  append_summary('Total rows merged: ' || v_total_rows);
  append_summary('Tables processed: ' || v_processed);

  COMMIT;
  p_result := v_summary;

EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
END IMPORT_TABLES_FROM_XML;
/

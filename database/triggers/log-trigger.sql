-- Goes through all tables owned by our team and attaches a log trigger
BEGIN
    FOR t IN (
        SELECT table_name, owner
        FROM ALL_TABLES
        WHERE owner = 'NBPT3')
        LOOP
            EXECUTE IMMEDIATE '
        CREATE OR REPLACE TRIGGER NBP_LOG_' || t.table_name || '
        AFTER INSERT OR UPDATE OR DELETE ON ' || t.owner || '.' || t.table_name || '
        FOR EACH ROW
        DECLARE
            v_action VARCHAR2(10);
        BEGIN
            IF INSERTING THEN
                v_action := ''INSERT'';
            ELSIF UPDATING THEN
                v_action := ''UPDATE'';
            ELSIF DELETING THEN
                v_action := ''DELETE'';
            END IF;

            INSERT INTO NBP.NBP_LOG (
                ACTION_NAME,
                TABLE_NAME,
                DATE_TIME,
                DB_USER
            )
            VALUES (
                v_action,
                ''' || t.table_name || ''',
                SYSDATE,
                USER
            );
        END;';
        END LOOP;
END;
/

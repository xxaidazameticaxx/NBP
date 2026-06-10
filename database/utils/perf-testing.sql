-- Insert into USER_SESSION. We will do performance testing on USER_ID queries, so we insert three different
-- types of sessions to showcase how indexes work on common and uncommon items.

-- A single user session for user_id 195
INSERT INTO NBP_USER_SESSION (SESSION_ID, USER_ID, CREATED_AT, EXPIRES_AT)
VALUES (
           SYS_GUID(),
           195,
           SYSTIMESTAMP,
           SYSTIMESTAMP + INTERVAL '1' HOUR
       );

-- 100 user sessions for user_id 196
INSERT INTO NBP_USER_SESSION (SESSION_ID, USER_ID, CREATED_AT, EXPIRES_AT)
SELECT
    SYS_GUID(),
    196,
    SYSTIMESTAMP,
    SYSTIMESTAMP + INTERVAL '1' HOUR
FROM dual
CONNECT BY LEVEL <= 100;

-- 10000 user sessions for user_id 197
INSERT INTO NBP_USER_SESSION (SESSION_ID, USER_ID, CREATED_AT, EXPIRES_AT)
SELECT
    SYS_GUID(),
    197,
    SYSTIMESTAMP,
    SYSTIMESTAMP + INTERVAL '1' HOUR
FROM dual
CONNECT BY LEVEL <= 10000;

COMMIT;

-- A procedure for measuring query performance
CREATE OR REPLACE PROCEDURE benchmark_query(
    p_sql IN CLOB
)
    IS
    v_start TIMESTAMP;
    v_end   TIMESTAMP;
    v_diff  INTERVAL DAY TO SECOND;
    v_ms    NUMBER;

    c       SYS_REFCURSOR;

    -- generic fetch target
    v_dummy VARCHAR2(4000);
BEGIN
    -- Show plan
    EXECUTE IMMEDIATE 'EXPLAIN PLAN FOR ' || p_sql;
    FOR r IN (
        SELECT PLAN_TABLE_OUTPUT
        FROM TABLE(DBMS_XPLAN.DISPLAY())
        ) LOOP
            DBMS_OUTPUT.PUT_LINE(r.PLAN_TABLE_OUTPUT);
        END LOOP;


    -- Execute the query
    v_start := SYSTIMESTAMP;

    OPEN c FOR p_sql;

    LOOP
        FETCH c INTO v_dummy;
        EXIT WHEN c%NOTFOUND;
    END LOOP;

    CLOSE c;

    v_end := SYSTIMESTAMP;

    v_diff := v_end - v_start;

    v_ms :=
            EXTRACT(DAY FROM v_diff) * 86400000 +
            EXTRACT(HOUR FROM v_diff) * 3600000 +
            EXTRACT(MINUTE FROM v_diff) * 60000 +
            EXTRACT(SECOND FROM v_diff) * 1000;

    DBMS_OUTPUT.PUT_LINE('Elapsed ms = ' || v_ms);
END;
/

-- Start with the user_id=195 case. Indexes matter the most here because only one row has this value.
-- Runs: 0.181ms, 0.172ms, 0.202ms, 0.173ms, 0.187ms
-- Average: 0.183ms
BEGIN
    benchmark_query(
            'SELECT SESSION_ID FROM NBP_USER_SESSION WHERE USER_ID = 195'
    );
END;
/

-- Runs: 0.673ms, 0.742ms, 0.722ms, 0.689ms, 0.675ms
-- Average: 0.700ms
BEGIN
    benchmark_query(
            'SELECT /*+ FULL(NBP_USER_SESSION) */ SESSION_ID FROM NBP_USER_SESSION WHERE USER_ID = 195'
    );
END;
/


-- The user_id=196 case. Indexes should matter about the same.
-- Runs: 0.178ms, 0.193ms, 0.253ms, 0.195ms, 0.203ms
-- Average: 0.204ms
BEGIN
    benchmark_query(
            'SELECT SESSION_ID FROM NBP_USER_SESSION WHERE USER_ID = 196'
    );
END;
/

-- Runs: 0.734ms, 0.739ms, 0.765ms, 0.673ms, 0.688ms
-- Average: 0.719ms
BEGIN
    benchmark_query(
            'SELECT /*+ FULL(NBP_USER_SESSION) */ SESSION_ID FROM NBP_USER_SESSION WHERE USER_ID = 196'
    );
END;
/

-- User_id=197 has thousands of records to return. Indexing should not be very useful.
-- Runs: 40ms, 40ms, 55ms, 49ms, 39ms
-- Average: 44.6ms
BEGIN
    benchmark_query(
            'SELECT SESSION_ID FROM NBP_USER_SESSION WHERE USER_ID = 197'
    );
END;
/

-- Runs: 35ms, 40ms, 35ms, 41ms, 39ms
-- Average: 38ms
BEGIN
    benchmark_query(
            'SELECT /*+ FULL(NBP_USER_SESSION) */ SESSION_ID FROM NBP_USER_SESSION WHERE USER_ID = 197'
    );
END;
/

-- Cleanup test data
DELETE FROM NBP_USER_SESSION WHERE USER_ID IN (195, 196, 197);
COMMIT;

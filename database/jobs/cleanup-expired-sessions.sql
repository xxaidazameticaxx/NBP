-- Erase use session from NBP_USER_SESSION.

CREATE OR REPLACE PROCEDURE CLEANUP_EXPIRED_SESSIONS AS
    v_deleted NUMBER;
BEGIN
    DELETE FROM NBP_USER_SESSION
    WHERE EXPIRES_AT < SYSDATE;

    v_deleted := SQL%ROWCOUNT;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('Deleted expired sessions: ' || v_deleted);
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END CLEANUP_EXPIRED_SESSIONS;


-- Job: Start procedure every 6 hours
BEGIN
    DBMS_SCHEDULER.CREATE_JOB(
        job_name        => 'JOB_CLEANUP_EXPIRED_SESSIONS',
        job_type        => 'STORED_PROCEDURE',
        job_action      => 'CLEANUP_EXPIRED_SESSIONS',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=HOURLY;INTERVAL=6',
        enabled         => TRUE,
        comments        => 'Deleted expired sessions from NBP_USER_SESSION every 6 hours'
    );
END;
/
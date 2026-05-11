-- For each professor that has PENDING excuses older then 3 days, 
-- insert a notification if they haven't received one today.

CREATE OR REPLACE PROCEDURE NOTIFY_PENDING_EXCUSES AS
BEGIN
    INSERT INTO NBP_NOTIFICATION (
        USER_ID,
        TITLE,
        MESSAGE,
        IS_READ,
        CREATED_AT,
        NOTIFICATION_TYPE,
        COURSE_SESSION_ID
    )
    SELECT
        p.USER_ID,
        'Pending Excuse Requests',
        'You have ' || COUNT(*) || ' excuse requests waiting for approval that are older than 3 days.',
        0,
        SYSTIMESTAMP,
        'EXCUSE_REMINDER',
        NULL
    FROM NBP_ABSENCE_EXCUSE ae
    JOIN NBP_COURSE_SESSION cs ON ae.COURSE_SESSION_ID = cs.ID
    JOIN NBP_COURSE          c  ON cs.COURSE_ID        = c.ID
    JOIN NBP_PROFESSOR        p  ON c.PROFESSOR_ID     = p.ID
    WHERE ae.STATUS      = 'PENDING'
      AND ae.SUBMITTED_AT < SYSDATE - 3
      AND NOT EXISTS (
              SELECT 1
              FROM NBP_NOTIFICATION n
              WHERE n.USER_ID           = p.USER_ID
                AND n.NOTIFICATION_TYPE = 'EXCUSE_REMINDER'
                AND TRUNC(n.CREATED_AT) = TRUNC(SYSDATE)
          )
    GROUP BY p.USER_ID;

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END NOTIFY_PENDING_EXCUSES;


-- Job: Start procedure every day at 08:00.
BEGIN
    DBMS_SCHEDULER.CREATE_JOB(
        job_name        => 'JOB_NOTIFY_PENDING_EXCUSES',
        job_type        => 'STORED_PROCEDURE',
        job_action      => 'NOTIFY_PENDING_EXCUSES',
        start_date      => TRUNC(SYSTIMESTAMP) + INTERVAL '8' HOUR,
        repeat_interval => 'FREQ=DAILY;BYHOUR=8;BYMINUTE=0;BYSECOND=0',
        enabled         => TRUE,
        comments        => 'Notifies professors about excuse requests waiting for approval for more than 3 days'
    );
END;
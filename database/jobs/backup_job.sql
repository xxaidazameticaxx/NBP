BEGIN
  DBMS_SCHEDULER.CREATE_JOB(
    job_name        => 'NBPT3.BACKUP_JOB',
    job_type        => 'STORED_PROCEDURE',
    job_action      => 'NBPT3.BACKUP_TABELE',
    start_date      => SYSTIMESTAMP,
    repeat_interval => 'FREQ=DAILY;BYHOUR=12;BYMINUTE=0;BYSECOND=0',
    enabled         => TRUE,
    comments        => 'Daily backup of NBP_COURSE, NBP_PROFESSOR, NBP_STUDENT tables'
  );
END;
/
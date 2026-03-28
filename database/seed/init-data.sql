-- Initializes dummy data used for testing.

DECLARE
    -- ID holders for FK relationships
    v_dept_id        NBPT3.NBP_DEPARTMENT.ID%TYPE;
    v_prog_id        NBPT3.NBP_STUDY_PROGRAM.ID%TYPE;
    v_room_id        NBPT3.NBP_ROOM.ID%TYPE;
    v_course_id      NBPT3.NBP_COURSE.ID%TYPE;
    v_session_id     NBPT3.NBP_COURSE_SESSION.ID%TYPE;
    v_enroll_id      NBPT3.NBP_ENROLLMENT.ID%TYPE;
    v_user_id        NBP.NBP_USER.ID%TYPE;
    v_prof_id        NBPT3.NBP_PROFESSOR.ID%TYPE;
    v_student_id     NBPT3.NBP_STUDENT.ID%TYPE;
    v_timetable_id   NBPT3.NBP_TIMETABLE.ID%TYPE;

    -- Arrays to store multiple IDs
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    dept_ids         t_ids;
    prog_ids         t_ids;
    prof_ids         t_ids;
    room_ids         t_ids;
    course_ids       t_ids;
    session_ids      t_ids;
    student_ids      t_ids;
    student_user_ids t_ids;
    timetable_ids    t_ids;

BEGIN
    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_DEPARTMENT(NAME, CODE, DESCRIPTION)
            VALUES (CASE i
                        WHEN 1 THEN 'Computer Science'
                        WHEN 2 THEN 'Mathematics'
                        WHEN 3 THEN 'Physics'
                        WHEN 4 THEN 'Chemistry'
                        WHEN 5 THEN 'Biology'
                        WHEN 6 THEN 'History'
                        WHEN 7 THEN 'Economics'
                        WHEN 8 THEN 'Philosophy'
                        WHEN 9 THEN 'Engineering'
                        WHEN 10 THEN 'Law'
                        END,
                    'D' || i,
                    'Department ' || i)
            RETURNING ID INTO v_dept_id;
            dept_ids(i) := v_dept_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_STUDY_PROGRAM(NAME, CODE, DEPARTMENT_ID, DURATION_YEARS, DEGREE_TYPE)
            VALUES (CASE i
                        WHEN 1 THEN 'BSc Computer Science'
                        WHEN 2 THEN 'MSc Computer Science'
                        WHEN 3 THEN 'BSc Mathematics'
                        WHEN 4 THEN 'MSc Mathematics'
                        WHEN 5 THEN 'BSc Physics'
                        WHEN 6 THEN 'MSc Physics'
                        WHEN 7 THEN 'BSc Chemistry'
                        WHEN 8 THEN 'MSc Chemistry'
                        WHEN 9 THEN 'BSc Biology'
                        WHEN 10 THEN 'MSc Biology'
                        END,
                    'P' || i,
                    dept_ids(ceil(i / 2)),
                    CASE WHEN MOD(i, 2) = 1 THEN 3 ELSE 2 END,
                    CASE WHEN MOD(i, 2) = 1 THEN 'BSc' ELSE 'MSc' END)
            RETURNING ID INTO v_prog_id;
            prog_ids(i) := v_prog_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_ROOM(NAME, BUILDING)
            VALUES ('Room ' || i, 'Building ' || (MOD(i - 1, 3) + 1))
            RETURNING ID INTO v_room_id;
            room_ids(i) := v_room_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBP.NBP_USER(FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, USERNAME, ROLE_ID)
            VALUES ('ProfFirst' || TO_CHAR(i), 'ProfLast' || TO_CHAR(i), 'prof' || TO_CHAR(i) || '@university.edu',
                    'pass' || TO_CHAR(i), 'prof' || TO_CHAR(i), 2)
            RETURNING ID INTO v_user_id;

            INSERT INTO NBPT3.NBP_PROFESSOR(USER_ID, TITLE, DEPARTMENT_ID, OFFICE_LOCATION)
            VALUES (v_user_id, 'Professor ' || i, dept_ids(i), 'Office ' || i)
            RETURNING ID INTO v_prof_id;

            prof_ids(i) := v_prof_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBP.NBP_USER(FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, USERNAME, ROLE_ID)
            VALUES ('StudentFirst' || i, 'StudentLast' || i, 'student' || i || '@university.edu', 'pass' || i,
                    'student' || i, 1)
            RETURNING ID INTO v_user_id;
            student_user_ids(i) := v_user_id;

            INSERT INTO NBPT3.NBP_STUDENT(USER_ID, INDEX_NUMBER, STUDY_PROGRAM_ID, ENROLLMENT_YEAR)
            VALUES (v_user_id, '2026' || LPAD(i, 3, '0'), prog_ids(i), 2026)
            RETURNING ID INTO v_student_id;
            student_ids(i) := v_student_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_COURSE(NAME, CODE, PROFESSOR_ID, DEPARTMENT_ID, ACADEMIC_YEAR, SEMESTER, CREDITS)
            VALUES (CASE i
                        WHEN 1 THEN 'Intro to CS'
                        WHEN 2 THEN 'Data Structures'
                        WHEN 3 THEN 'Calculus I'
                        WHEN 4 THEN 'Linear Algebra'
                        WHEN 5 THEN 'Physics I'
                        WHEN 6 THEN 'Physics II'
                        WHEN 7 THEN 'Organic Chemistry'
                        WHEN 8 THEN 'Inorganic Chemistry'
                        WHEN 9 THEN 'Biology I'
                        WHEN 10 THEN 'Genetics'
                        END,
                    'C' || TO_CHAR(i),
                    prof_ids(i),
                    dept_ids(ceil(i / 2)),
                    '2026',
                    CASE WHEN MOD(i, 2) = 1 THEN 1 ELSE 2 END,
                    6)
            RETURNING ID INTO v_course_id;
            course_ids(i) := v_course_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_TIMETABLE(COURSE_ID,
                                      ROOM_ID,
                                      DAY_OF_WEEK,
                                      START_TIME,
                                      END_TIME,
                                      VALID_FROM,
                                      VALID_TO)
            VALUES (course_ids(i),
                    room_ids(i),
                    CASE MOD(i - 1, 5)
                        WHEN 0 THEN 'Monday'
                        WHEN 1 THEN 'Tuesday'
                        WHEN 2 THEN 'Wednesday'
                        WHEN 3 THEN 'Thursday'
                        WHEN 4 THEN 'Friday'
                        END,
                    SYSDATE + i / 24,
                    SYSDATE + (i + 2) / 24,
                    SYSDATE,
                    SYSDATE + 365)
            RETURNING ID INTO v_timetable_id;
            timetable_ids(i) := v_timetable_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_COURSE_SESSION(COURSE_ID, SESSION_START_TIME, SESSION_END_TIME, SESSION_CODE, ROOM_ID,
                                           TIMETABLE_ID, SESSION_TYPE)
            VALUES (course_ids(i), SYSDATE + i / 24, SYSDATE + (i + 1) / 24, 'S' || i, room_ids(i), timetable_ids(i),
                    'Lecture')
            RETURNING ID INTO v_session_id;
            session_ids(i) := v_session_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_ENROLLMENT(STUDENT_ID, COURSE_ID, ENROLLMENT_DATE)
            VALUES (student_ids(i), course_ids(i), SYSDATE)
            RETURNING ID INTO v_enroll_id;
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_ATTENDANCE(STUDENT_ID, COURSE_SESSION_ID, IS_PRESENT, MARKED_AT, NOTES)
            VALUES (student_ids(i), session_ids(i), CASE WHEN MOD(i, 2) = 0 THEN 1 ELSE 0 END, SYSDATE, 'Note ' || i);
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_ABSENCE_EXCUSE(STUDENT_ID, COURSE_SESSION_ID, REASON, SUBMITTED_AT, STATUS, REVIEWED_BY)
            VALUES (student_ids(i), session_ids(i), 'Reason ' || i, SYSDATE,
                    CASE WHEN MOD(i, 2) = 0 THEN 'Approved' ELSE 'Pending' END, NULL);
        END LOOP;

    FOR i IN 1..10
        LOOP
            INSERT INTO NBPT3.NBP_NOTIFICATION(USER_ID, TITLE, MESSAGE, IS_READ, CREATED_AT, NOTIFICATION_TYPE,
                                         COURSE_SESSION_ID)
            VALUES (student_user_ids(i), 'Title ' || i, 'Message ' || i, 0, SYSDATE, 'Info', session_ids(i));
        END LOOP;

    COMMIT;
END;
/

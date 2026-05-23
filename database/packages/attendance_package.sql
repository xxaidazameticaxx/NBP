CREATE OR REPLACE PACKAGE NBPT3.ATTENDANCE_PKG AS

    PROCEDURE OPEN_SESSION(
        p_course_id    IN NBP_COURSE_SESSION.COURSE_ID%TYPE,
        p_timetable_id IN NBP_COURSE_SESSION.TIMETABLE_ID%TYPE,
        p_room_id      IN NBP_COURSE_SESSION.ROOM_ID%TYPE,
        p_session_id   OUT NBP_COURSE_SESSION.ID%TYPE,
        p_session_code OUT NBP_COURSE_SESSION.SESSION_CODE%TYPE
    );

    PROCEDURE REGISTER_ATTENDANCE(
        p_session_code IN NBP_COURSE_SESSION.SESSION_CODE%TYPE,
        p_student_id   IN NBP_ATTENDANCE.STUDENT_ID%TYPE
    );

    PROCEDURE CLOSE_SESSION(
        p_session_id IN NBP_COURSE_SESSION.ID%TYPE
    );

END ATTENDANCE_PKG;
/


CREATE OR REPLACE PACKAGE BODY NBPT3.ATTENDANCE_PKG AS

    FUNCTION generate_session_code RETURN VARCHAR2 IS
        v_code VARCHAR2(50);
        v_count NUMBER;
    BEGIN
        LOOP
            v_code := 'ATT-' || LPAD(TRUNC(DBMS_RANDOM.VALUE(0, 999999)), 6, '0');

            SELECT COUNT(*)
            INTO v_count
            FROM NBP_COURSE_SESSION
            WHERE SESSION_CODE = v_code
              AND SESSION_END_TIME IS NULL; -- samo aktivne sesije

            EXIT WHEN v_count = 0;
        END LOOP;

        RETURN v_code;
    END generate_session_code;

    PROCEDURE OPEN_SESSION(
        p_course_id    IN NBP_COURSE_SESSION.COURSE_ID%TYPE,
        p_timetable_id IN NBP_COURSE_SESSION.TIMETABLE_ID%TYPE,
        p_room_id      IN NBP_COURSE_SESSION.ROOM_ID%TYPE,
        p_session_id   OUT NBP_COURSE_SESSION.ID%TYPE,
        p_session_code OUT NBP_COURSE_SESSION.SESSION_CODE%TYPE
    ) IS
        v_course_count  NUMBER;
        v_room_count    NUMBER;
        v_active_count  NUMBER;
        v_session_type  NBP_COURSE_SESSION.SESSION_TYPE%TYPE;
    BEGIN
        SELECT COUNT(*) INTO v_course_count
        FROM NBP_COURSE
        WHERE ID = p_course_id;

        IF v_course_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Kurs sa ID=' || p_course_id || ' ne postoji.');
        END IF;

        SELECT COUNT(*) INTO v_room_count
        FROM NBP_ROOM
        WHERE ID = p_room_id;

        IF v_room_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20002, 'Soba sa ID=' || p_room_id || ' ne postoji.');
        END IF;

        SELECT COUNT(*) INTO v_active_count
        FROM NBP_COURSE_SESSION
        WHERE COURSE_ID = p_course_id
          AND SESSION_END_TIME IS NULL;

        IF v_active_count > 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'Kurs vec ima aktivnu sesiju koja nije zatvorena.');
        END IF;

        IF p_timetable_id IS NOT NULL THEN
            v_session_type := 'Lecture';
        ELSE
            v_session_type := 'Extra';
        END IF;

        p_session_code := generate_session_code();

        INSERT INTO NBP_COURSE_SESSION (
            COURSE_ID,
            SESSION_START_TIME,
            SESSION_END_TIME,
            SESSION_CODE,
            ROOM_ID,
            TIMETABLE_ID,
            SESSION_TYPE
        ) VALUES (
            p_course_id,
            SYSTIMESTAMP,
            NULL,
            p_session_code,
            p_room_id,
            p_timetable_id,
            v_session_type
        ) RETURNING ID INTO p_session_id;

        COMMIT;

        DBMS_OUTPUT.PUT_LINE('Sesija otvorena. ID: ' || p_session_id || ', Kod: ' || p_session_code);

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            RAISE;
    END OPEN_SESSION;

    PROCEDURE REGISTER_ATTENDANCE(
        p_session_code IN NBP_COURSE_SESSION.SESSION_CODE%TYPE,
        p_student_id   IN NBP_ATTENDANCE.STUDENT_ID%TYPE
    ) IS
        v_session_id    NBP_COURSE_SESSION.ID%TYPE;
        v_course_id     NBP_COURSE_SESSION.COURSE_ID%TYPE;
        v_enrolled      NUMBER;
        v_already_marked NUMBER;
        v_student_user_id NBP.NBP_USER.ID%TYPE;
    BEGIN
        BEGIN
            SELECT ID, COURSE_ID
            INTO v_session_id, v_course_id
            FROM NBP_COURSE_SESSION
            WHERE SESSION_CODE = p_session_code
              AND SESSION_END_TIME IS NULL;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20010, 'Sesijski kod nije validan ili je sesija vec zatvorena.');
        END;

        BEGIN
            SELECT USER_ID INTO v_student_user_id
            FROM NBP_STUDENT
            WHERE ID = p_student_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20011, 'Student sa ID=' || p_student_id || ' ne postoji.');
        END;

        SELECT COUNT(*) INTO v_enrolled
        FROM NBP_ENROLLMENT
        WHERE STUDENT_ID = p_student_id
          AND COURSE_ID = v_course_id;

        IF v_enrolled = 0 THEN
            RAISE_APPLICATION_ERROR(-20012, 'Student nije upisan na kurs za ovu sesiju.');
        END IF;

        SELECT COUNT(*) INTO v_already_marked
        FROM NBP_ATTENDANCE
        WHERE STUDENT_ID = p_student_id
          AND COURSE_SESSION_ID = v_session_id;

        IF v_already_marked > 0 THEN
            RAISE_APPLICATION_ERROR(-20013, 'Student je vec prijavio prisustvo za ovu sesiju.');
        END IF;

        INSERT INTO NBP_ATTENDANCE (
            STUDENT_ID,
            COURSE_SESSION_ID,
            IS_PRESENT,
            MARKED_AT,
            NOTES
        ) VALUES (
            p_student_id,
            v_session_id,
            1,
            SYSTIMESTAMP,
            NULL
        );

        INSERT INTO NBP_NOTIFICATION (
            USER_ID,
            TITLE,
            MESSAGE,
            IS_READ,
            CREATED_AT,
            NOTIFICATION_TYPE,
            COURSE_SESSION_ID
        ) VALUES (
            v_student_user_id,
            'Prisustvo evidentirano',
            'Vase prisustvo je uspjesno evidentirano za sesiju ID=' || v_session_id || '.',
            0,
            SYSTIMESTAMP,
            'ATTENDANCE',
            v_session_id
        );

        COMMIT;

        DBMS_OUTPUT.PUT_LINE('Prisustvo evidentirano za studenta ID=' || p_student_id);

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            RAISE;
    END REGISTER_ATTENDANCE;


    PROCEDURE CLOSE_SESSION(
        p_session_id IN NBP_COURSE_SESSION.ID%TYPE
    ) IS
        v_course_id     NBP_COURSE_SESSION.COURSE_ID%TYPE;
        v_end_time      NBP_COURSE_SESSION.SESSION_END_TIME%TYPE;
        v_absent_count  NUMBER := 0;
    BEGIN
        BEGIN
            SELECT COURSE_ID, SESSION_END_TIME
            INTO v_course_id, v_end_time
            FROM NBP_COURSE_SESSION
            WHERE ID = p_session_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20020, 'Sesija sa ID=' || p_session_id || ' ne postoji.');
        END;

        IF v_end_time IS NOT NULL THEN
            RAISE_APPLICATION_ERROR(-20021, 'Sesija je vec zatvorena.');
        END IF;

        INSERT INTO NBP_ATTENDANCE (
            STUDENT_ID,
            COURSE_SESSION_ID,
            IS_PRESENT,
            MARKED_AT,
            NOTES
        )
        SELECT
            e.STUDENT_ID,
            p_session_id,
            0,
            SYSTIMESTAMP,
            'Automatski evidentiran izostanak pri zatvaranju sesije'
        FROM NBP_ENROLLMENT e
        WHERE e.COURSE_ID = v_course_id
          AND NOT EXISTS (
              SELECT 1
              FROM NBP_ATTENDANCE a
              WHERE a.STUDENT_ID = e.STUDENT_ID
                AND a.COURSE_SESSION_ID = p_session_id
          );

        v_absent_count := SQL%ROWCOUNT;

        UPDATE NBP_COURSE_SESSION
        SET SESSION_END_TIME = SYSTIMESTAMP,
            SESSION_CODE     = NULL
        WHERE ID = p_session_id;

        COMMIT;

        DBMS_OUTPUT.PUT_LINE('Sesija zatvorena. Automatski evidentirani izostanci: ' || v_absent_count);

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            RAISE;
    END CLOSE_SESSION;

END ATTENDANCE_PKG;

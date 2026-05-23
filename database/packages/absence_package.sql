CREATE OR REPLACE PACKAGE NBPT3.EXCUSE_PKG AS

    PROCEDURE SUBMIT_EXCUSE(
        p_student_id       IN NBP_ABSENCE_EXCUSE.STUDENT_ID%TYPE,
        p_session_id       IN NBP_ABSENCE_EXCUSE.COURSE_SESSION_ID%TYPE,
        p_reason           IN NBP_ABSENCE_EXCUSE.REASON%TYPE,
        p_document_name    IN NBP_ABSENCE_EXCUSE.DOCUMENT_NAME%TYPE,
        p_excuse_id        OUT NBP_ABSENCE_EXCUSE.ID%TYPE
    );

    PROCEDURE APPROVE_EXCUSE(
        p_excuse_id        IN NBP_ABSENCE_EXCUSE.ID%TYPE,
        p_professor_id     IN NBP_PROFESSOR.ID%TYPE
    );

    PROCEDURE REJECT_EXCUSE(
        p_excuse_id        IN NBP_ABSENCE_EXCUSE.ID%TYPE,
        p_professor_id     IN NBP_PROFESSOR.ID%TYPE
    );

END EXCUSE_PKG;

CREATE OR REPLACE PACKAGE BODY NBPT3.EXCUSE_PKG AS

    PROCEDURE SUBMIT_EXCUSE(
        p_student_id       IN NBP_ABSENCE_EXCUSE.STUDENT_ID%TYPE,
        p_session_id       IN NBP_ABSENCE_EXCUSE.COURSE_SESSION_ID%TYPE,
        p_reason           IN NBP_ABSENCE_EXCUSE.REASON%TYPE,
        p_document_name    IN NBP_ABSENCE_EXCUSE.DOCUMENT_NAME%TYPE,
        p_excuse_id        OUT NBP_ABSENCE_EXCUSE.ID%TYPE
    ) IS
        v_session_count    NUMBER;
        v_student_count    NUMBER;
        v_attendance_count NUMBER;
        v_is_present       NUMBER;
        v_existing_excuse  NUMBER;
        v_student_user_id  NBP.NBP_USER.ID%TYPE;
        v_course_id        NBP_COURSE_SESSION.COURSE_ID%TYPE;
        v_professor_user_id NBP.NBP_USER.ID%TYPE;
    BEGIN
        SELECT COUNT(*) INTO v_session_count
        FROM NBP_COURSE_SESSION
        WHERE ID = p_session_id;

        IF v_session_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20030, 'Sesija sa ID=' || p_session_id || ' ne postoji.');
        END IF;

        SELECT COUNT(*) INTO v_student_count
        FROM NBP_STUDENT
        WHERE ID = p_student_id;

        IF v_student_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20031, 'Student sa ID=' || p_student_id || ' ne postoji.');
        END IF;

        SELECT COUNT(*), MAX(IS_PRESENT)
        INTO v_attendance_count, v_is_present
        FROM NBP_ATTENDANCE
        WHERE STUDENT_ID = p_student_id
          AND COURSE_SESSION_ID = p_session_id;

        IF v_attendance_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20032, 'Nema evidencije prisustva za ovog studenta na ovoj sesiji.');
        END IF;

        IF v_is_present = 1 THEN
            RAISE_APPLICATION_ERROR(-20033, 'Student je bio prisutan na ovoj sesiji, opravdanje nije potrebno.');
        END IF;

        SELECT COUNT(*) INTO v_existing_excuse
        FROM NBP_ABSENCE_EXCUSE
        WHERE STUDENT_ID = p_student_id
          AND COURSE_SESSION_ID = p_session_id;

        IF v_existing_excuse > 0 THEN
            RAISE_APPLICATION_ERROR(-20034, 'Student je vec podnio opravdanje za ovu sesiju.');
        END IF;

        INSERT INTO NBP_ABSENCE_EXCUSE (
            STUDENT_ID,
            COURSE_SESSION_ID,
            REASON,
            SUBMITTED_AT,
            STATUS,
            REVIEWED_BY,
            DOCUMENT_NAME
        ) VALUES (
            p_student_id,
            p_session_id,
            p_reason,
            SYSTIMESTAMP,
            'PENDING',
            NULL,
            p_document_name
        ) RETURNING ID INTO p_excuse_id;

        SELECT USER_ID INTO v_student_user_id
        FROM NBP_STUDENT
        WHERE ID = p_student_id;

        SELECT cs.COURSE_ID INTO v_course_id
        FROM NBP_COURSE_SESSION cs
        WHERE cs.ID = p_session_id;

        SELECT u.ID INTO v_professor_user_id
        FROM NBP_COURSE c
        JOIN NBP_PROFESSOR p ON c.PROFESSOR_ID = p.ID
        JOIN NBP.NBP_USER u ON p.USER_ID = u.ID
        WHERE c.ID = v_course_id;

        INSERT INTO NBP_NOTIFICATION (
            USER_ID,
            TITLE,
            MESSAGE,
            IS_READ,
            CREATED_AT,
            NOTIFICATION_TYPE,
            COURSE_SESSION_ID
        ) VALUES (
            v_professor_user_id,
            'Novo opravdanje',
            'Student ID=' || p_student_id || ' je podnio opravdanje za sesiju ID=' || p_session_id || '.',
            0,
            SYSTIMESTAMP,
            'EXCUSE_SUBMITTED',
            p_session_id
        );

        COMMIT;

        DBMS_OUTPUT.PUT_LINE('Opravdanje upisano. ID: ' || p_excuse_id);

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            RAISE;
    END SUBMIT_EXCUSE;

    PROCEDURE APPROVE_EXCUSE(
        p_excuse_id        IN NBP_ABSENCE_EXCUSE.ID%TYPE,
        p_professor_id     IN NBP_PROFESSOR.ID%TYPE
    ) IS
        v_status           NBP_ABSENCE_EXCUSE.STATUS%TYPE;
        v_session_id       NBP_ABSENCE_EXCUSE.COURSE_SESSION_ID%TYPE;
        v_student_id       NBP_ABSENCE_EXCUSE.STUDENT_ID%TYPE;
        v_course_id        NBP_COURSE_SESSION.COURSE_ID%TYPE;
        v_prof_course_count NUMBER;
        v_student_user_id  NBP.NBP_USER.ID%TYPE;
        v_reviewed_by_user NBP.NBP_USER.ID%TYPE;
    BEGIN
        BEGIN
            SELECT STATUS, COURSE_SESSION_ID, STUDENT_ID
            INTO v_status, v_session_id, v_student_id
            FROM NBP_ABSENCE_EXCUSE
            WHERE ID = p_excuse_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20040, 'Opravdanje sa ID=' || p_excuse_id || ' ne postoji.');
        END;

        IF v_status != 'PENDING' THEN
            RAISE_APPLICATION_ERROR(-20041, 'Opravdanje je vec obradjeno. Status: ' || v_status);
        END IF;

        SELECT COURSE_ID INTO v_course_id
        FROM NBP_COURSE_SESSION
        WHERE ID = v_session_id;

        SELECT COUNT(*) INTO v_prof_course_count
        FROM NBP_COURSE
        WHERE ID = v_course_id
          AND PROFESSOR_ID = p_professor_id;

        IF v_prof_course_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20042, 'Profesor nije vlasnik ovog kursa.');
        END IF;

        SELECT USER_ID INTO v_reviewed_by_user
        FROM NBP_PROFESSOR
        WHERE ID = p_professor_id;

        UPDATE NBP_ABSENCE_EXCUSE
        SET STATUS      = 'APPROVED',
            REVIEWED_BY = v_reviewed_by_user
        WHERE ID = p_excuse_id;

        UPDATE NBP_ATTENDANCE
        SET IS_PRESENT = 1,
            NOTES      = 'Opravdano - excuse ID=' || p_excuse_id
        WHERE STUDENT_ID        = v_student_id
          AND COURSE_SESSION_ID = v_session_id;

        SELECT USER_ID INTO v_student_user_id
        FROM NBP_STUDENT
        WHERE ID = v_student_id;

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
            'Opravdanje odobreno',
            'Vase opravdanje za sesiju ID=' || v_session_id || ' je odobreno.',
            0,
            SYSTIMESTAMP,
            'EXCUSE_APPROVED',
            v_session_id
        );

        COMMIT;

        DBMS_OUTPUT.PUT_LINE('Opravdanje ID=' || p_excuse_id || ' odobreno.');

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            RAISE;
    END APPROVE_EXCUSE;

    PROCEDURE REJECT_EXCUSE(
        p_excuse_id        IN NBP_ABSENCE_EXCUSE.ID%TYPE,
        p_professor_id     IN NBP_PROFESSOR.ID%TYPE
    ) IS
        v_status           NBP_ABSENCE_EXCUSE.STATUS%TYPE;
        v_session_id       NBP_ABSENCE_EXCUSE.COURSE_SESSION_ID%TYPE;
        v_student_id       NBP_ABSENCE_EXCUSE.STUDENT_ID%TYPE;
        v_course_id        NBP_COURSE_SESSION.COURSE_ID%TYPE;
        v_prof_course_count NUMBER;
        v_student_user_id  NBP.NBP_USER.ID%TYPE;
        v_reviewed_by_user NBP.NBP_USER.ID%TYPE;
    BEGIN
        BEGIN
            SELECT STATUS, COURSE_SESSION_ID, STUDENT_ID
            INTO v_status, v_session_id, v_student_id
            FROM NBP_ABSENCE_EXCUSE
            WHERE ID = p_excuse_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20050, 'Opravdanje sa ID=' || p_excuse_id || ' ne postoji.');
        END;

        IF v_status != 'PENDING' THEN
            RAISE_APPLICATION_ERROR(-20051, 'Opravdanje je vec obradjeno. Status: ' || v_status);
        END IF;

        SELECT COURSE_ID INTO v_course_id
        FROM NBP_COURSE_SESSION
        WHERE ID = v_session_id;

        SELECT COUNT(*) INTO v_prof_course_count
        FROM NBP_COURSE
        WHERE ID = v_course_id
          AND PROFESSOR_ID = p_professor_id;

        IF v_prof_course_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20052, 'Profesor nije vlasnik ovog kursa.');
        END IF;

        SELECT USER_ID INTO v_reviewed_by_user
        FROM NBP_PROFESSOR
        WHERE ID = p_professor_id;

        UPDATE NBP_ABSENCE_EXCUSE
        SET STATUS      = 'REJECTED',
            REVIEWED_BY = v_reviewed_by_user
        WHERE ID = p_excuse_id;

        SELECT USER_ID INTO v_student_user_id
        FROM NBP_STUDENT
        WHERE ID = v_student_id;

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
            'Opravdanje odbijeno',
            'Vase opravdanje za sesiju ID=' || v_session_id || ' je odbijeno.',
            0,
            SYSTIMESTAMP,
            'EXCUSE_REJECTED',
            v_session_id
        );

        COMMIT;

        DBMS_OUTPUT.PUT_LINE('Opravdanje ID=' || p_excuse_id || ' odbijeno.');

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            RAISE;
    END REJECT_EXCUSE;

END EXCUSE_PKG;
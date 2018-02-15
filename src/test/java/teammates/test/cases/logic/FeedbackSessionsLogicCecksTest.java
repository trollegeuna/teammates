package teammates.test.cases.logic;

import org.testng.annotations.Test;


import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;

import teammates.common.exception.EntityDoesNotExistException;

import teammates.logic.core.FeedbackQuestionsLogic;
import teammates.logic.core.FeedbackResponsesLogic;
import teammates.logic.core.FeedbackSessionsLogic;


/**
 * SUT: {@link FeedbackSessionsLogic}.
 */
public class FeedbackSessionsLogicChecksTest extends BaseLogicTest {
    private static FeedbackSessionsLogic fsLogic = FeedbackSessionsLogic.inst();
    private static FeedbackQuestionsLogic fqLogic = FeedbackQuestionsLogic.inst();
    private static FeedbackResponsesLogic frLogic = FeedbackResponsesLogic.inst();

    @Override
    protected void prepareTestData() {
        dataBundle = loadDataBundle("/FeedbackSessionsLogicTest.json");
        removeAndRestoreDataBundle(dataBundle);
    }

    @Test
    public void testAll() throws Exception {

        testIsFeedbackSessionViewableToStudents();
        testIsFeedbackSessionFullyCompletedByStudent();
        testIsFeedbackSessionCompletedByStudent();
        testIsFeedbackSessionCompletedByInstructor();
        testIsFeedbackSessionHasQuestionForStudents();

    }


    private void testIsFeedbackSessionViewableToStudents() {
        ______TS("Session with questions for students to answer");
        FeedbackSessionAttributes session = dataBundle.feedbackSessions.get("session1InCourse1");
        assertTrue(fsLogic.isFeedbackSessionViewableToStudents(session));

        ______TS("Session without questions for students, but with visible responses");
        session = dataBundle.feedbackSessions.get("archiveCourse.session1");
        assertTrue(fsLogic.isFeedbackSessionViewableToStudents(session));

        session = dataBundle.feedbackSessions.get("session2InCourse2");
        assertTrue(fsLogic.isFeedbackSessionViewableToStudents(session));

        ______TS("private session");
        session = dataBundle.feedbackSessions.get("session1InCourse2");
        assertFalse(fsLogic.isFeedbackSessionViewableToStudents(session));

        ______TS("empty session");
        session = dataBundle.feedbackSessions.get("empty.session");
        assertFalse(fsLogic.isFeedbackSessionViewableToStudents(session));
    }







    private void testIsFeedbackSessionFullyCompletedByStudent() throws Exception {

        FeedbackSessionAttributes fs = dataBundle.feedbackSessions.get("session1InCourse1");
        StudentAttributes student1OfCourse1 = dataBundle.students.get("student1InCourse1");
        StudentAttributes student3OfCourse1 = dataBundle.students.get("student3InCourse1");

        ______TS("failure: non-existent feedback session for student");

        try {
            fsLogic.isFeedbackSessionFullyCompletedByStudent("nonExistentFSName", fs.getCourseId(), "random.student@email");
            signalFailureToDetectException();
        } catch (EntityDoesNotExistException edne) {
            assertEquals("Trying to check a non-existent feedback session: "
                            + fs.getCourseId() + "/" + "nonExistentFSName",
                    edne.getMessage());
        }

        ______TS("success case: fully done by student 1");
        assertTrue(fsLogic.isFeedbackSessionFullyCompletedByStudent(fs.getFeedbackSessionName(), fs.getCourseId(),
                student1OfCourse1.email));

        ______TS("success case: partially done by student 3");
        assertFalse(fsLogic.isFeedbackSessionFullyCompletedByStudent(fs.getFeedbackSessionName(), fs.getCourseId(),
                student3OfCourse1.email));
    }



    private void testIsFeedbackSessionCompletedByStudent() {

        ______TS("success: empty session");

        FeedbackSessionAttributes fs = dataBundle.feedbackSessions.get("empty.session");
        StudentAttributes student = dataBundle.students.get("student2InCourse1");

        assertTrue(fsLogic.isFeedbackSessionCompletedByStudent(fs, student.email));
    }



    private void testIsFeedbackSessionCompletedByInstructor() throws Exception {

        ______TS("success: empty session");

        FeedbackSessionAttributes fs = dataBundle.feedbackSessions.get("empty.session");
        InstructorAttributes instructor = dataBundle.instructors.get("instructor2OfCourse1");

        assertTrue(fsLogic.isFeedbackSessionCompletedByInstructor(fs, instructor.email));
    }


    private void testIsFeedbackSessionHasQuestionForStudents() throws Exception {
        // no need to removeAndRestoreTypicalDataInDatastore() as the previous test does not change the db

        FeedbackSessionAttributes sessionWithStudents = dataBundle.feedbackSessions.get("gracePeriodSession");
        FeedbackSessionAttributes sessionWithoutStudents = dataBundle.feedbackSessions.get("closedSession");

        ______TS("non-existent session/courseId");

        try {
            fsLogic.isFeedbackSessionHasQuestionForStudents("nOnEXistEnT session", "someCourse");
            signalFailureToDetectException();
        } catch (EntityDoesNotExistException edne) {
            assertEquals("Trying to check a non-existent feedback session: "
                            + "someCourse" + "/" + "nOnEXistEnT session",
                    edne.getMessage());
        }

        ______TS("session contains students");

        assertTrue(fsLogic.isFeedbackSessionHasQuestionForStudents(sessionWithStudents.getFeedbackSessionName(),
                sessionWithStudents.getCourseId()));

        ______TS("session does not contain students");

        assertFalse(fsLogic.isFeedbackSessionHasQuestionForStudents(sessionWithoutStudents.getFeedbackSessionName(),
                sessionWithoutStudents.getCourseId()));
    }
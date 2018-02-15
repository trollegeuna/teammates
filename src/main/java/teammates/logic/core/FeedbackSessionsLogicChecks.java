package teammates.logic.core;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.FeedbackSessionType;
import teammates.common.datatransfer.UserRole;
import teammates.common.datatransfer.attributes.*;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.util.Const;
import teammates.storage.api.FeedbackSessionsDb;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FeedbackSessionsLogicChecks {

    private static final String ERROR_NON_EXISTENT_FS_STRING_FORMAT = "Trying to %s a non-existent feedback session: ";
    private static final String ERROR_NON_EXISTENT_FS_GET = String.format(ERROR_NON_EXISTENT_FS_STRING_FORMAT, "get");
    private static final String ERROR_NON_EXISTENT_FS_UPDATE = String.format(ERROR_NON_EXISTENT_FS_STRING_FORMAT, "update");
    private static final String ERROR_NON_EXISTENT_FS_CHECK = String.format(ERROR_NON_EXISTENT_FS_STRING_FORMAT, "check");
    private static final String ERROR_NON_EXISTENT_FS_VIEW = String.format(ERROR_NON_EXISTENT_FS_STRING_FORMAT, "view");
    private static final FeedbackSessionsLogic fb = FeedbackSessionsLogic.inst();
    private static final FeedbackResponsesLogic frLogic = FeedbackResponsesLogic.inst();
    private static final FeedbackSessionsDb fsDb = new FeedbackSessionsDb();
    private static final FeedbackQuestionsLogic fqLogic = FeedbackQuestionsLogic.inst();


    private static boolean isResponseVisibleForUser(String userEmail,
                                             UserRole role, StudentAttributes student,
                                             Set<String> studentsEmailInTeam,
                                             FeedbackResponseAttributes response,
                                             FeedbackQuestionAttributes relatedQuestion, InstructorAttributes instructor) {

        boolean isVisibleResponse = false;
        if (isInstructor(role) && relatedQuestion.isResponseVisibleTo(FeedbackParticipantType.INSTRUCTORS)
                || response.recipient.equals(userEmail)
                && relatedQuestion.isResponseVisibleTo(FeedbackParticipantType.RECEIVER)
                || response.giver.equals(userEmail)
                || isStudent(role) && relatedQuestion.isResponseVisibleTo(FeedbackParticipantType.STUDENTS)) {
            isVisibleResponse = true;
        } else if (studentsEmailInTeam != null && isStudent(role)) {
            if (relatedQuestion.recipientType == FeedbackParticipantType.TEAMS
                    && relatedQuestion.isResponseVisibleTo(FeedbackParticipantType.RECEIVER)
                    && response.recipient.equals(student.team)) {
                isVisibleResponse = true;
            } else if (relatedQuestion.giverType == FeedbackParticipantType.TEAMS
                    && studentsEmailInTeam.contains(response.giver)) {
                isVisibleResponse = true;
            } else if (relatedQuestion.isResponseVisibleTo(FeedbackParticipantType.OWN_TEAM_MEMBERS)
                    && studentsEmailInTeam.contains(response.giver)) {
                isVisibleResponse = true;
            } else if (relatedQuestion.isResponseVisibleTo(FeedbackParticipantType.RECEIVER_TEAM_MEMBERS)
                    && studentsEmailInTeam.contains(response.recipient)) {
                isVisibleResponse = true;
            }
        }
        if (isVisibleResponse && instructor != null) {
            boolean isGiverSectionRestricted =
                    !instructor.isAllowedForPrivilege(response.giverSection,
                            response.feedbackSessionName,
                            Const.ParamsNames.INSTRUCTOR_PERMISSION_VIEW_SESSION_IN_SECTIONS);
            // If instructors are not restricted to view the giver's section,
            // they are allowed to view responses to GENERAL, subject to visibility options
            boolean isRecipientSectionRestricted =
                    relatedQuestion.recipientType != FeedbackParticipantType.NONE
                            && !instructor.isAllowedForPrivilege(response.recipientSection,
                            response.feedbackSessionName,
                            Const.ParamsNames.INSTRUCTOR_PERMISSION_VIEW_SESSION_IN_SECTIONS);

            boolean isNotAllowedForInstructor = isGiverSectionRestricted || isRecipientSectionRestricted;
            if (isNotAllowedForInstructor) {
                isVisibleResponse = false;
            }
        }
        return isVisibleResponse;
    }

    private static boolean isStudent(UserRole role) {
        return role == UserRole.STUDENT;
    }

    private static boolean isInstructor(UserRole role) {
        return role == UserRole.INSTRUCTOR;
    }

    public boolean isCreatorOfSession(String feedbackSessionName, String courseId, String userEmail) {
        FeedbackSessionAttributes fs = fb.getFeedbackSession(feedbackSessionName, courseId);
        return fs.getCreatorEmail().equals(userEmail);
    }

    public static boolean isFeedbackSessionExists(String feedbackSessionName, String courseId) {
        return fsDb.getFeedbackSession(courseId, feedbackSessionName) != null;
    }

    public boolean isFeedbackSessionHasQuestionForStudents(
            String feedbackSessionName,
            String courseId) throws EntityDoesNotExistException {
        if (!isFeedbackSessionExists(feedbackSessionName, courseId)) {
            throw new EntityDoesNotExistException(ERROR_NON_EXISTENT_FS_CHECK + courseId + "/" + feedbackSessionName);
        }

        List<FeedbackQuestionAttributes> allQuestions =
                fqLogic.getFeedbackQuestionsForStudents(feedbackSessionName,
                        courseId);

        return !allQuestions.isEmpty();
    }

    public boolean isFeedbackSessionCompletedByStudent(FeedbackSessionAttributes fsa, String userEmail) {
        if (fsa.getRespondingStudentList().contains(userEmail)) {
            return true;
        }

        String feedbackSessionName = fsa.getFeedbackSessionName();
        String courseId = fsa.getCourseId();
        List<FeedbackQuestionAttributes> allQuestions =
                fqLogic.getFeedbackQuestionsForStudents(feedbackSessionName, courseId);
        // if there is no question for students, session is complete
        return allQuestions.isEmpty();
    }

    public boolean isFeedbackSessionCompletedByInstructor(FeedbackSessionAttributes fsa, String userEmail)
            throws EntityDoesNotExistException {
        if (fsa.getRespondingInstructorList().contains(userEmail)) {
            return true;
        }

        String feedbackSessionName = fsa.getFeedbackSessionName();
        String courseId = fsa.getCourseId();
        List<FeedbackQuestionAttributes> allQuestions =
                fqLogic.getFeedbackQuestionsForInstructor(feedbackSessionName, courseId, userEmail);
        // if there is no question for instructor, session is complete
        return allQuestions.isEmpty();
    }


    public static boolean isFeedbackSessionFullyCompletedByStudent(
            String feedbackSessionName,
            String courseId, String userEmail)
            throws EntityDoesNotExistException {

        if (!isFeedbackSessionExists(feedbackSessionName, courseId)) {
            throw new EntityDoesNotExistException(ERROR_NON_EXISTENT_FS_CHECK + courseId + "/" + feedbackSessionName);
        }

        List<FeedbackQuestionAttributes> allQuestions =
                fqLogic.getFeedbackQuestionsForStudents(feedbackSessionName,
                        courseId);

        for (FeedbackQuestionAttributes question : allQuestions) {
            if (!fqLogic.isQuestionFullyAnsweredByUser(question, userEmail)) {
                // If any question is not completely answered, session is not
                // completed
                return false;
            }
        }
        return true;
    }

    private static boolean isFeedbackSessionFullyCompletedByInstructor(
            String feedbackSessionName,
            String courseId, String userEmail)
            throws EntityDoesNotExistException {

        if (!isFeedbackSessionExists(feedbackSessionName, courseId)) {
            throw new EntityDoesNotExistException(ERROR_NON_EXISTENT_FS_CHECK + courseId + "/" + feedbackSessionName);
        }

        List<FeedbackQuestionAttributes> allQuestions =
                fqLogic.getFeedbackQuestionsForInstructor(feedbackSessionName,
                        courseId,
                        userEmail);

        for (FeedbackQuestionAttributes question : allQuestions) {
            if (!fqLogic.isQuestionFullyAnsweredByUser(question, userEmail)) {
                // If any question is not completely answered, session is not
                // completed
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the feedback session is viewable to the specified user.
     */
    private static boolean isFeedbackSessionViewableTo(
            FeedbackSessionAttributes session,
            String userEmail,
            boolean isInstructorOfCourse) {

        // If the session is a private session created by the same user, it is viewable to the user
        if (session.getFeedbackSessionType() == FeedbackSessionType.PRIVATE) {
            return session.getCreatorEmail().equals(userEmail);
        }

        // Allow all instructors to view always
        if (isInstructorOfCourse) {
            return true;
        }

        // Allow viewing if session is viewable to students
        return isFeedbackSessionViewableToStudents(session);
    }

    public static boolean isFeedbackSessionViewableToStudents(
            FeedbackSessionAttributes session) {
        // Allow students to view the feedback session if there are questions for them
        List<FeedbackQuestionAttributes> questionsToAnswer =
                fqLogic.getFeedbackQuestionsForStudents(
                        session.getFeedbackSessionName(), session.getCourseId());

        if (session.isVisible() && !questionsToAnswer.isEmpty()) {
            return true;
        }

        // Allow students to view the feedback session
        // if there are any questions for instructors to answer
        // where the responses of the questions are visible to the students
        List<FeedbackQuestionAttributes> questionsWithVisibleResponses = new ArrayList<>();
        List<FeedbackQuestionAttributes> questionsForInstructors =
                fqLogic.getFeedbackQuestionsForCreatorInstructor(session);
        for (FeedbackQuestionAttributes question : questionsForInstructors) {
            if (frLogic.isResponseOfFeedbackQuestionVisibleToStudent(question)) {
                questionsWithVisibleResponses.add(question);
            }
        }

        return session.isVisible() && !questionsWithVisibleResponses.isEmpty();
    }

    /**
     * Returns true if there are any questions for students to answer.
     */
    public static boolean isFeedbackSessionForStudentsToAnswer(FeedbackSessionAttributes session) {

        List<FeedbackQuestionAttributes> questionsToAnswer =
                fqLogic.getFeedbackQuestionsForStudents(
                        session.getFeedbackSessionName(), session.getCourseId());

        return session.isVisible() && !questionsToAnswer.isEmpty();
    }

}

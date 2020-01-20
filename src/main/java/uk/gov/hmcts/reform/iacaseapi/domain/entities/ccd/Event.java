package uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Event {

    START_APPEAL("startAppeal"),
    EDIT_APPEAL("editAppeal"),
    SUBMIT_APPEAL("submitAppeal"),
    SEND_DIRECTION("sendDirection"),
    REQUEST_RESPONDENT_EVIDENCE("requestRespondentEvidence"),
    UPLOAD_RESPONDENT_EVIDENCE("uploadRespondentEvidence"),
    UPLOAD_HOME_OFFICE_BUNDLE("uploadHomeOfficeBundle"),
    BUILD_CASE("buildCase"),
    SUBMIT_CASE("submitCase"),
    REQUEST_CASE_EDIT("requestCaseEdit"),
    REQUEST_RESPONDENT_REVIEW("requestRespondentReview"),
    ADD_APPEAL_RESPONSE("addAppealResponse"),
    UPLOAD_HOME_OFFICE_APPEAL_RESPONSE("uploadHomeOfficeAppealResponse"),
    REQUEST_HEARING_REQUIREMENTS("requestHearingRequirements"),
    REQUEST_HEARING_REQUIREMENTS_FEATURE("requestHearingRequirementsFeature"),
    REVIEW_HEARING_REQUIREMENTS("reviewHearingRequirements"),
    DRAFT_HEARING_REQUIREMENTS("draftHearingRequirements"),
    CHANGE_DIRECTION_DUE_DATE("changeDirectionDueDate"),
    UPLOAD_ADDITIONAL_EVIDENCE("uploadAdditionalEvidence"),
    UPLOAD_ADDENDUM_EVIDENCE("uploadAddendumEvidence"),
    LIST_CASE("listCase"),
    LIST_CASE_WITHOUT_HEARING_REQUIREMENTS("listCaseWithoutHearingRequirements"),
    CREATE_CASE_SUMMARY("createCaseSummary"),
    REVERT_STATE_TO_AWAITING_RESPONDENT_EVIDENCE("revertStateToAwaitingRespondentEvidence"),
    GENERATE_HEARING_BUNDLE("generateHearingBundle"),
    DECISION_AND_REASONS_STARTED("decisionAndReasonsStarted"),
    GENERATE_DECISION_AND_REASONS("generateDecisionAndReasons"),
    SEND_DECISION_AND_REASONS("sendDecisionAndReasons"),
    ADD_CASE_NOTE("addCaseNote"),
    EDIT_CASE_LISTING("editCaseListing"),
    RECORD_APPLICATION("recordApplication"),
    RECORD_ATTENDEES_AND_DURATION("recordAttendeesAndDuration"),
    END_APPEAL("endAppeal"),
    REQUEST_CASE_BUILDING("requestCaseBuilding"),
    UPLOAD_ADDITIONAL_EVIDENCE_HOME_OFFICE("uploadAdditionalEvidenceHomeOffice"),
    REQUEST_RESPONSE_REVIEW("requestResponseReview"),
    REQUEST_RESPONSE_AMEND("requestResponseAmend"),
    UPLOAD_ADDENDUM_EVIDENCE_LEGAL_REP("uploadAddendumEvidenceLegalRep"),
    UPLOAD_ADDENDUM_EVIDENCE_HOME_OFFICE("uploadAddendumEvidenceHomeOffice"),
    SUBMIT_HEARING_REQUIREMENTS("submitHearingRequirements"),

    REMOVE_APPEAL_FROM_ONLINE("removeAppealFromOnline"),

    SHARE_A_CASE("shareACase"),

    @JsonEnumDefaultValue
    UNKNOWN("unknown");

    @JsonValue
    private final String id;

    Event(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}

package uk.gov.hmcts.reform.iacaseapi.domain.entities;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.*;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.HoursAndMinutes;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.Document;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.IdValue;

public class PreviousHearingTest {

    private final Optional<String> allocatedJudge = Optional.of("Judge Joe");
    private final String appellantNameForDisplay = "Joe Bloggs";
    private final String respondentRepresentative = "Mr Cliff Evans";
    private final Optional<HoursAndMinutes> actualCaseHearingLength = Optional.of(new HoursAndMinutes("4", "30"));
    private final String ariaListingReference = "123456";
    private final HearingCentre listCaseHearingCentre = HearingCentre.TAYLOR_HOUSE;
    private final String listCaseHearingDate = "13/10/2020";
    private final String listCaseHearingLength = "6 hours";
    private final String appealDecision = "Dismissed";

    private final Document doc = new Document(
        "documentUrl",
        "binaryUrl",
        "documentFilename");

    private final HearingRecordingDocument hearingRecordingDocument1 = new HearingRecordingDocument(
        doc,
        "some description");

    private final DocumentWithMetadata decisionAndReasonsDocument = new DocumentWithMetadata(
        new Document(
            "documentUrl",
            "binaryUrl",
            "documentFilename"),
        "description",
        "dateUploaded",
        DocumentTag.FINAL_DECISION_AND_REASONS_PDF
    );

    private final DocumentWithMetadata hearingRequirementsDocument = new DocumentWithMetadata(
        new Document(
            "documentUrl",
            "binaryUrl",
            "documentFilename"),
        "description",
        "dateUploaded",
        DocumentTag.HEARING_REQUIREMENTS
    );

    private final List<IdValue<HearingRecordingDocument>> allHearingRecordingDocuments = asList(
        new IdValue<>(
            "1",
            hearingRecordingDocument1
        )
    );

    private final List<IdValue<DocumentWithMetadata>> allFinalDecisionAndReasonsDocuments = asList(
        new IdValue<DocumentWithMetadata>(
            "1",
            decisionAndReasonsDocument
        )
    );

    private final List<IdValue<DocumentWithMetadata>> allHearingRequirementsDocuments = asList(
        new IdValue<DocumentWithMetadata>(
            "1",
            hearingRequirementsDocument
        )
    );

    private PreviousHearing previousHearing = new PreviousHearing(
        allocatedJudge,
        appellantNameForDisplay,
        respondentRepresentative,
        actualCaseHearingLength,
        ariaListingReference,
        listCaseHearingCentre,
        listCaseHearingDate,
        listCaseHearingLength,
        allHearingRecordingDocuments,
        appealDecision,
        allFinalDecisionAndReasonsDocuments,
        allHearingRequirementsDocuments
    );

    @Test
    public void should_hold_onto_values() {
        Assert.assertEquals(allocatedJudge, previousHearing.getAllocatedJudge());
        Assert.assertEquals(appellantNameForDisplay, previousHearing.getAppellantNameForDisplay());
        Assert.assertEquals(respondentRepresentative, previousHearing.getRespondentRepresentative());
        Assert.assertEquals(actualCaseHearingLength, previousHearing.getActualCaseHearingLength());
        Assert.assertEquals(ariaListingReference, previousHearing.getAriaListingReference());
        Assert.assertEquals(listCaseHearingCentre, previousHearing.getListCaseHearingCentre());
        Assert.assertEquals(listCaseHearingDate, previousHearing.getListCaseHearingDate());
        Assert.assertEquals(listCaseHearingLength, previousHearing.getListCaseHearingLength());
        Assert.assertEquals(allHearingRecordingDocuments, previousHearing.getHearingRecordingDocuments());
        Assert.assertEquals(appealDecision, previousHearing.getAppealDecision());
        Assert.assertEquals(allFinalDecisionAndReasonsDocuments, previousHearing.getFinalDecisionAndReasonsDocuments());
        Assert.assertEquals(allHearingRequirementsDocuments, previousHearing.getHearingRequirements());
    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            null,
            respondentRepresentative,
            actualCaseHearingLength,
            ariaListingReference,
            listCaseHearingCentre,
            listCaseHearingDate,
            listCaseHearingLength,
            allHearingRecordingDocuments,
            appealDecision,
            allFinalDecisionAndReasonsDocuments,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            null,
            actualCaseHearingLength,
            ariaListingReference,
            listCaseHearingCentre,
            listCaseHearingDate,
            listCaseHearingLength,
            allHearingRecordingDocuments,
            appealDecision,
            allFinalDecisionAndReasonsDocuments,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            respondentRepresentative,
            actualCaseHearingLength,
            null,
            listCaseHearingCentre,
            listCaseHearingDate,
            listCaseHearingLength,
            allHearingRecordingDocuments,
            appealDecision,
            allFinalDecisionAndReasonsDocuments,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            respondentRepresentative,
            actualCaseHearingLength,
            ariaListingReference,
            null,
            listCaseHearingDate,
            listCaseHearingLength,
            allHearingRecordingDocuments,
            appealDecision,
            allFinalDecisionAndReasonsDocuments,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            respondentRepresentative,
            actualCaseHearingLength,
            ariaListingReference,
            listCaseHearingCentre,
            null,
            listCaseHearingLength,
            allHearingRecordingDocuments,
            appealDecision,
            allFinalDecisionAndReasonsDocuments,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            respondentRepresentative,
            actualCaseHearingLength,
            ariaListingReference,
            listCaseHearingCentre,
            listCaseHearingDate,
            null,
            allHearingRecordingDocuments,
            appealDecision,
            allFinalDecisionAndReasonsDocuments,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            respondentRepresentative,
            actualCaseHearingLength,
            ariaListingReference,
            listCaseHearingCentre,
            listCaseHearingDate,
            listCaseHearingLength,
            null,
            appealDecision,
            allFinalDecisionAndReasonsDocuments,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            respondentRepresentative,
            actualCaseHearingLength,
            ariaListingReference,
            listCaseHearingCentre,
            listCaseHearingDate,
            listCaseHearingLength,
            allHearingRecordingDocuments,
            null,
            allFinalDecisionAndReasonsDocuments,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            respondentRepresentative,
            actualCaseHearingLength,
            ariaListingReference,
            listCaseHearingCentre,
            listCaseHearingDate,
            listCaseHearingLength,
            allHearingRecordingDocuments,
            appealDecision,
            null,
            allHearingRequirementsDocuments))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PreviousHearing(
            allocatedJudge,
            appellantNameForDisplay,
            respondentRepresentative,
            actualCaseHearingLength,
            ariaListingReference,
            listCaseHearingCentre,
            listCaseHearingDate,
            listCaseHearingLength,
            allHearingRecordingDocuments,
            appealDecision,
            allFinalDecisionAndReasonsDocuments,
            null))
            .isExactlyInstanceOf(NullPointerException.class);
    }
}

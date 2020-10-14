package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;


import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.*;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.IdValue;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.iacaseapi.domain.service.Appender;


@Component
public class CustomiseHearingBundlePreparer implements PreSubmitCallbackHandler<AsylumCase> {

    private final Appender<DocumentWithDescription> documentWithDescriptionAppender;

    public CustomiseHearingBundlePreparer(Appender<DocumentWithDescription> documentWithDescriptionAppender) {
        this.documentWithDescriptionAppender = documentWithDescriptionAppender;
    }



    @Override
    public boolean canHandle(PreSubmitCallbackStage callbackStage, Callback<AsylumCase> callback) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_START
                && callback.getEvent() == Event.CUSTOMISE_HEARING_BUNDLE;
    }

    @Override
    public PreSubmitCallbackResponse<AsylumCase> handle(PreSubmitCallbackStage callbackStage, Callback<AsylumCase> callback) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        AsylumCase asylumCase = callback.getCaseDetails().getCaseData();

        Optional<YesOrNo> maybeCaseFlagSetAsideReheardExists = asylumCase.read(CASE_FLAG_SET_ASIDE_REHEARD_EXISTS,YesOrNo.class);

        boolean isCaseReheard = maybeCaseFlagSetAsideReheardExists.isPresent()
                && maybeCaseFlagSetAsideReheardExists.get() == YesOrNo.YES;

        if (isCaseReheard) {
            prepareCustomDocuments(asylumCase, FTPA_APPELLANT_EVIDENCE_DOCUMENTS, CUSTOM_FTPA_APPELLANT_EVIDENCE_DOCS);
            prepareCustomDocuments(asylumCase, FTPA_RESPONDENT_EVIDENCE_DOCUMENTS, CUSTOM_FTPA_RESPONDENT_EVIDENCE_DOCS);
            prepareCustomDocuments(asylumCase, FTPA_APPELLANT_DOCUMENTS, CUSTOM_FTPA_APPELLANT_DOCS);
            prepareCustomDocuments(asylumCase, FTPA_RESPONDENT_DOCUMENTS, CUSTOM_FTPA_RESPONDENT_DOCS);
            prepareCustomDocuments(asylumCase, FINAL_DECISION_AND_REASONS_DOCUMENTS, CUSTOM_FINAL_DECISION_AND_REASONS_DOCS);
            prepareCustomDocuments(asylumCase, REHEARD_HEARING_DOCUMENTS, CUSTOM_REHEARD_HEARING_DOCS);
            prepareCustomDocuments(asylumCase, ADDENDUM_EVIDENCE_DOCUMENTS, CUSTOM_APP_ADDENDUM_EVIDENCE_DOCS);
            prepareCustomDocuments(asylumCase, ADDENDUM_EVIDENCE_DOCUMENTS, CUSTOM_RESP_ADDENDUM_EVIDENCE_DOCS);

            if (asylumCase.read(ADDENDUM_EVIDENCE_DOCUMENTS).isPresent()) {
                List<IdValue<DocumentWithMetadata>> test1 = getIdValues(asylumCase,ADDENDUM_EVIDENCE_DOCUMENTS,"The appellant");
                asylumCase.write(APPELLANT_ADDENDUM_EVIDENCE_DOCS,test1);
                List<IdValue<DocumentWithMetadata>> test2 = getIdValues(asylumCase,ADDENDUM_EVIDENCE_DOCUMENTS,"The respondent");
                asylumCase.write(RESPONDENT_ADDENDUM_EVIDENCE_DOCS,test2);
            }

        } else {

            prepareCustomDocuments(asylumCase, HEARING_DOCUMENTS, CUSTOM_HEARING_DOCUMENTS);
            prepareCustomDocuments(asylumCase, LEGAL_REPRESENTATIVE_DOCUMENTS, CUSTOM_LEGAL_REP_DOCUMENTS);
            prepareCustomDocuments(asylumCase, ADDITIONAL_EVIDENCE_DOCUMENTS, CUSTOM_ADDITIONAL_EVIDENCE_DOCUMENTS);
            prepareCustomDocuments(asylumCase, RESPONDENT_DOCUMENTS, CUSTOM_RESPONDENT_DOCUMENTS);
        }

        return new PreSubmitCallbackResponse<>(asylumCase);
    }

    public void prepareCustomDocuments(AsylumCase asylumCase, AsylumCaseFieldDefinition sourceField, AsylumCaseFieldDefinition targetField) {
        if (!asylumCase.read(sourceField).isPresent()) {
            return;
        }

        if (sourceField == FTPA_RESPONDENT_EVIDENCE_DOCUMENTS || sourceField == FTPA_APPELLANT_EVIDENCE_DOCUMENTS) {
            prepareFtpaRespondentEvidenceDocuments(asylumCase,sourceField,targetField);
            return;
        }

        Optional<List<IdValue<DocumentWithMetadata>>> maybeDocuments =
                asylumCase.read(sourceField);

        List<IdValue<DocumentWithMetadata>> documents =
                maybeDocuments.orElse(emptyList());

        List<IdValue<DocumentWithDescription>> customDocuments = new ArrayList<>();

        for (IdValue<DocumentWithMetadata> documentWithMetadata : documents) {
            DocumentWithDescription newDocumentWithDescription =
                    new DocumentWithDescription(documentWithMetadata.getValue().getDocument(),
                            documentWithMetadata.getValue().getDescription());

            if (sourceField == LEGAL_REPRESENTATIVE_DOCUMENTS) {
                if (documentWithMetadata.getValue().getTag() == DocumentTag.APPEAL_SUBMISSION
                        || documentWithMetadata.getValue().getTag() == DocumentTag.CASE_ARGUMENT) {
                    customDocuments = documentWithDescriptionAppender.append(newDocumentWithDescription, customDocuments);
                }
            } else if (targetField == CUSTOM_APP_ADDENDUM_EVIDENCE_DOCS) {
                if ("The appellant".equals(documentWithMetadata.getValue().getSuppliedBy())) {
                    customDocuments = documentWithDescriptionAppender.append(newDocumentWithDescription, customDocuments);
                }
            } else if (targetField == CUSTOM_RESP_ADDENDUM_EVIDENCE_DOCS) {
                if ("The respondent".equals(documentWithMetadata.getValue().getSuppliedBy())) {
                    customDocuments = documentWithDescriptionAppender.append(newDocumentWithDescription, customDocuments);
                }
            } else {
                customDocuments = documentWithDescriptionAppender.append(newDocumentWithDescription, customDocuments);
            }
        }

        asylumCase.clear(targetField);
        asylumCase.write(targetField, customDocuments);
    }

    private List<IdValue<DocumentWithMetadata>> getIdValues(
        AsylumCase asylumCase,
        AsylumCaseFieldDefinition fieldDefinition,String suppliedBy
    ) {

        if (asylumCase != null) {
            Optional<List<IdValue<DocumentWithMetadata>>> maybeIdValues = asylumCase
                .read(fieldDefinition);
            if (maybeIdValues.isPresent()) {
                return maybeIdValues.get()
                    .stream()
                    .filter(it -> it.getValue().getSuppliedBy().equals(suppliedBy))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    public void prepareFtpaRespondentEvidenceDocuments(AsylumCase asylumCase, AsylumCaseFieldDefinition sourceField, AsylumCaseFieldDefinition targetField) {
        if (!asylumCase.read(sourceField).isPresent()) {
            return;
        }

        Optional<List<IdValue<DocumentWithDescription>>> maybeDocuments =
                asylumCase.read(sourceField);

        List<IdValue<DocumentWithDescription>> documents =
                maybeDocuments.orElse(emptyList());

        List<IdValue<DocumentWithDescription>> customDocuments = new ArrayList<>();

        for (IdValue<DocumentWithDescription> documentWithMetadata : documents) {
            customDocuments = documentWithDescriptionAppender.append(documentWithMetadata.getValue(), customDocuments);
        }

        asylumCase.clear(targetField);
        asylumCase.write(targetField, customDocuments);
    }
}

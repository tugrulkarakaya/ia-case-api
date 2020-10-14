package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.DateProvider;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.*;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.Document;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.IdValue;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.em.Bundle;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.iacaseapi.domain.service.Appender;
import uk.gov.hmcts.reform.iacaseapi.infrastructure.clients.BundleRequestExecutor;


@Component
public class CustomiseHearingBundleHandler implements PreSubmitCallbackHandler<AsylumCase> {
    private final BundleRequestExecutor bundleRequestExecutor;
    private final Appender<DocumentWithMetadata> documentWithMetadataAppender;
    private final Appender<DocumentWithDescription> documentWithDescriptionAppender;
    private final DateProvider dateProvider;
    private final String emBundlerUrl;
    private final String emBundlerStitchUri;
    private final ObjectMapper objectMapper;
    private final String missingDocumentExceptionMessage = "Document cannot be null";

    public CustomiseHearingBundleHandler(
            @Value("${emBundler.url}") String emBundlerUrl,
            @Value("${emBundler.stitch.uri}") String emBundlerStitchUri,
            BundleRequestExecutor bundleRequestExecutor,
            Appender<DocumentWithMetadata> documentWithMetadataAppender,
            Appender<DocumentWithDescription> documentWithDescriptionAppender,
            DateProvider dateProvider,
            ObjectMapper objectMapper
    ) {
        this.emBundlerUrl = emBundlerUrl;
        this.emBundlerStitchUri = emBundlerStitchUri;
        this.bundleRequestExecutor = bundleRequestExecutor;
        this.documentWithMetadataAppender = documentWithMetadataAppender;
        this.documentWithDescriptionAppender = documentWithDescriptionAppender;
        this.dateProvider = dateProvider;
        this.objectMapper = objectMapper;

    }

    public boolean canHandle(
            PreSubmitCallbackStage callbackStage,
            Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT
                && callback.getEvent() == Event.CUSTOMISE_HEARING_BUNDLE;
    }

    public PreSubmitCallbackResponse<AsylumCase> handle(
            PreSubmitCallbackStage callbackStage,
            Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final AsylumCase asylumCase =
                callback
                        .getCaseDetails()
                        .getCaseData();
        asylumCase.clear(AsylumCaseFieldDefinition.HMCTS);
        asylumCase.write(AsylumCaseFieldDefinition.HMCTS, "[userImage:hmcts.png]");
        asylumCase.clear(AsylumCaseFieldDefinition.CASE_BUNDLES);

        Optional<YesOrNo> maybeCaseFlagSetAsideReheardExists = asylumCase.read(CASE_FLAG_SET_ASIDE_REHEARD_EXISTS,YesOrNo.class);

        if (maybeCaseFlagSetAsideReheardExists.isPresent()
                && maybeCaseFlagSetAsideReheardExists.get() == YesOrNo.YES) {
            asylumCase.write(AsylumCaseFieldDefinition.BUNDLE_CONFIGURATION, "iac-reheard-hearing-bundle-config.yaml");
        } else {
            asylumCase.write(AsylumCaseFieldDefinition.BUNDLE_CONFIGURATION, "iac-hearing-bundle-config.yaml");
        }

        asylumCase.write(AsylumCaseFieldDefinition.BUNDLE_FILE_NAME_PREFIX, getBundlePrefix(asylumCase));

        //deep copy the case
        AsylumCase asylumCaseCopy;
        try {
            asylumCaseCopy = objectMapper
                    .readValue(objectMapper.writeValueAsString(asylumCase), AsylumCase.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot make a deep copy of the case");
        }

        boolean isReheardCase = maybeCaseFlagSetAsideReheardExists.isPresent()
                && maybeCaseFlagSetAsideReheardExists.get() == YesOrNo.YES;

        if (isReheardCase) {
            prepareDocuments(asylumCaseCopy, CUSTOM_FTPA_APPELLANT_EVIDENCE_DOCS, FTPA_APPELLANT_EVIDENCE_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_FTPA_RESPONDENT_EVIDENCE_DOCS, FTPA_RESPONDENT_EVIDENCE_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_FTPA_APPELLANT_DOCS, FTPA_APPELLANT_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_FTPA_RESPONDENT_DOCS, FTPA_RESPONDENT_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_FINAL_DECISION_AND_REASONS_DOCS, FINAL_DECISION_AND_REASONS_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_REHEARD_HEARING_DOCS, REHEARD_HEARING_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_APP_ADDENDUM_EVIDENCE_DOCS, APPELLANT_ADDENDUM_EVIDENCE_DOCS);
            prepareDocuments(asylumCaseCopy, CUSTOM_RESP_ADDENDUM_EVIDENCE_DOCS, RESPONDENT_ADDENDUM_EVIDENCE_DOCS);
        } else {
            prepareDocuments(asylumCaseCopy, CUSTOM_HEARING_DOCUMENTS, HEARING_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_LEGAL_REP_DOCUMENTS, LEGAL_REPRESENTATIVE_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_ADDITIONAL_EVIDENCE_DOCUMENTS, ADDITIONAL_EVIDENCE_DOCUMENTS);
            prepareDocuments(asylumCaseCopy, CUSTOM_RESPONDENT_DOCUMENTS, RESPONDENT_DOCUMENTS);
        }

        final PreSubmitCallbackResponse<AsylumCase> response = bundleRequestExecutor.post(
                new Callback<>(
                        new CaseDetails<>(
                                callback.getCaseDetails().getId(),
                                callback.getCaseDetails().getJurisdiction(),
                                callback.getCaseDetails().getState(),
                                asylumCaseCopy,
                                callback.getCaseDetails().getCreatedDate()
                        ),
                        callback.getCaseDetailsBefore(),
                        callback.getEvent()
                ),
                emBundlerUrl + emBundlerStitchUri);

        final AsylumCase responseData = response.getData();

        restoreFolders(asylumCase, asylumCaseCopy,isReheardCase);
        restoreAddendumEvidence(asylumCase,asylumCaseCopy);

        Optional<List<IdValue<Bundle>>> maybeCaseBundles = responseData.read(AsylumCaseFieldDefinition.CASE_BUNDLES);
        asylumCase.write(AsylumCaseFieldDefinition.CASE_BUNDLES, maybeCaseBundles);

        final List<Bundle> caseBundles = maybeCaseBundles
                .orElseThrow(() -> new IllegalStateException("caseBundle is not present"))
                .stream()
                .map(IdValue::getValue)
                .collect(Collectors.toList());

        if (caseBundles.size() != 1) {
            throw new IllegalStateException("case bundles size is not 1 and is : " + caseBundles.size());
        }

        //stitchStatusflags -  NEW, IN_PROGRESS, DONE, FAILED
        final String stitchStatus = caseBundles.get(0).getStitchStatus().orElse("");

        asylumCase.write(AsylumCaseFieldDefinition.STITCHING_STATUS, stitchStatus);

        return new PreSubmitCallbackResponse<>(asylumCase);
    }



    private Optional<IdValue<DocumentWithDescription>> isFtpaRespondentEvidenceDocumentPresent(
            List<IdValue<DocumentWithDescription>> idValueList,
            IdValue<DocumentWithDescription> documentWithDescription
    ) {

        IdValue<DocumentWithDescription> documentWithDescriptionIdValue = null;

        for (IdValue<DocumentWithDescription> doc : idValueList) {
            Optional<Document> documentWithDescription1 = doc.getValue().getDocument();
            if (documentWithDescription1.isPresent()) {
                Document document = documentWithDescription.getValue().getDocument().orElseThrow(() -> new IllegalStateException(missingDocumentExceptionMessage));
                if (documentWithDescription1.get().getDocumentBinaryUrl().equals(document.getDocumentBinaryUrl())) {
                    documentWithDescriptionIdValue = doc;
                }
            }
        }

        return Optional.ofNullable(documentWithDescriptionIdValue);
    }

    private boolean isDocumentPresent(
            List<IdValue<DocumentWithDescription>> existingDocuments,
            IdValue<DocumentWithDescription> documentWithMetadata
    ) {

        if (!documentWithMetadata.getValue().getDocument().isPresent()) {
            return false;
        }

        boolean found = false;

        for (IdValue<DocumentWithDescription> doc : existingDocuments) {
            Optional<Document> maybeDocument = doc.getValue().getDocument();
            if (maybeDocument.isPresent()) {
                Document document = maybeDocument.get();
                Document passedDocument = documentWithMetadata.getValue().getDocument().orElseThrow(() -> new IllegalStateException(missingDocumentExceptionMessage));
                if (passedDocument != null  && passedDocument.getDocumentBinaryUrl().equals(document.getDocumentBinaryUrl())) {
                    found = true;
                }
            }
        }

        return found;
    }

    private Optional<IdValue<DocumentWithMetadata>> isDocumentWithDescriptionPresent(
            List<IdValue<DocumentWithMetadata>> idValueList,
            IdValue<DocumentWithDescription> documentWithDescription
    ) {

        IdValue<DocumentWithMetadata> documentWithMetadataIdValue = null;

        for (IdValue<DocumentWithMetadata> doc : idValueList) {
            Document legalDocument = doc.getValue().getDocument();
            Document document = documentWithDescription.getValue().getDocument().orElseThrow(() -> new IllegalStateException(missingDocumentExceptionMessage));
            if (legalDocument.getDocumentBinaryUrl().equals(document.getDocumentBinaryUrl())) {
                documentWithMetadataIdValue = doc;
            }
        }

        return Optional.ofNullable(documentWithMetadataIdValue);
    }

    private void restoreAddendumEvidence(AsylumCase asylumCase, AsylumCase asylumCaseBefore) {

        //add to addendum and not clear the list -- create a new method just to deal with one
        Optional<List<IdValue<DocumentWithMetadata>>> currentAppellantAddendumEvidenceDocs = asylumCase.read(APPELLANT_ADDENDUM_EVIDENCE_DOCS);
        Optional<List<IdValue<DocumentWithMetadata>>> currentRespondentAddendumEvidenceDocs = asylumCase.read(RESPONDENT_ADDENDUM_EVIDENCE_DOCS);

        Optional<List<IdValue<DocumentWithMetadata>>> maybeAddendumEvidenceDocs = asylumCaseBefore.read(ADDENDUM_EVIDENCE_DOCUMENTS);

        List<IdValue<DocumentWithMetadata>> beforeDocuments = new ArrayList<>();

        if (maybeAddendumEvidenceDocs.isPresent()) {
            beforeDocuments = getIdValuesBefore(asylumCaseBefore, ADDENDUM_EVIDENCE_DOCUMENTS);
        }
        //filter any document missing from the current list of document
        List<IdValue<DocumentWithMetadata>> missingAppellantDocuments = beforeDocuments
            .stream()
            .filter(document -> !contains(currentAppellantAddendumEvidenceDocs.orElse(emptyList()), document,"The appellant"))
            .collect(Collectors.toList());

        List<IdValue<DocumentWithMetadata>> allAppellantDocuments = currentAppellantAddendumEvidenceDocs.orElse(emptyList());

        for (IdValue<DocumentWithMetadata> documentWithMetadata : missingAppellantDocuments) {
            allAppellantDocuments = documentWithMetadataAppender.append(documentWithMetadata.getValue(), allAppellantDocuments);
        }

        List<IdValue<DocumentWithMetadata>> missingRespondentDocuments = beforeDocuments
            .stream()
            .filter(document -> !contains(currentRespondentAddendumEvidenceDocs.orElse(emptyList()), document,"The respondent"))
            .collect(Collectors.toList());

        List<IdValue<DocumentWithMetadata>> allRespondentDocuments = currentRespondentAddendumEvidenceDocs.orElse(emptyList());
        for (IdValue<DocumentWithMetadata> documentWithMetadata : missingRespondentDocuments) {
            allRespondentDocuments = documentWithMetadataAppender.append(documentWithMetadata.getValue(), allRespondentDocuments);
        }

        //add the 2 list
        List<IdValue<DocumentWithMetadata>> allDocuments = new ArrayList<>();

        for (IdValue<DocumentWithMetadata> documentWithMetadata : allAppellantDocuments) {
            allDocuments = documentWithMetadataAppender.append(documentWithMetadata.getValue(), allDocuments);
        }

        for (IdValue<DocumentWithMetadata> documentWithMetadata : allRespondentDocuments) {
            allDocuments = documentWithMetadataAppender.append(documentWithMetadata.getValue(), allDocuments);
        }

        asylumCase.clear(ADDENDUM_EVIDENCE_DOCUMENTS);
        asylumCase.write(ADDENDUM_EVIDENCE_DOCUMENTS, allDocuments);

    }

    private void restoreFolders(
            AsylumCase asylumCase,
            AsylumCase asylumCaseBefore,
            boolean isReheardCase
    ) {
        getFieldDefinitions(isReheardCase).forEach(field -> {
            if (field == FTPA_RESPONDENT_EVIDENCE_DOCUMENTS || field == FTPA_APPELLANT_EVIDENCE_DOCUMENTS) {
                restoreFtpaRespondentEvidenceFolders(asylumCase,asylumCaseBefore,field);
            } else {

                Optional<List<IdValue<DocumentWithMetadata>>> currentIdValues = asylumCase.read(field);
                Optional<List<IdValue<DocumentWithMetadata>>> beforeIdValues = asylumCaseBefore.read(field);

                List<IdValue<DocumentWithMetadata>> beforeDocuments = new ArrayList<>();

                if (beforeIdValues.isPresent()) {
                    beforeDocuments = getIdValuesBefore(asylumCaseBefore, field);
                }
                //filter any document missing from the current list of document
                List<IdValue<DocumentWithMetadata>> missingDocuments = beforeDocuments
                        .stream()
                        .filter(document -> !contains(currentIdValues.orElse(emptyList()), document))
                        .collect(Collectors.toList());

                List<IdValue<DocumentWithMetadata>> allDocuments = currentIdValues.orElse(emptyList());
                for (IdValue<DocumentWithMetadata> documentWithMetadata : missingDocuments) {
                    allDocuments = documentWithMetadataAppender.append(documentWithMetadata.getValue(), allDocuments);
                }

                asylumCase.clear(field);
                asylumCase.write(field, allDocuments);
            }
        });
    }

    private void restoreFtpaRespondentEvidenceFolders(
            AsylumCase asylumCase,
            AsylumCase asylumCaseBefore,
            AsylumCaseFieldDefinition field
    ) {

        Optional<List<IdValue<DocumentWithDescription>>> currentIdValues = asylumCase.read(field);
        Optional<List<IdValue<DocumentWithDescription>>> beforeIdValues = asylumCaseBefore.read(field);

        List<IdValue<DocumentWithDescription>> beforeDocuments = new ArrayList<>();

        if (beforeIdValues.isPresent()) {
            beforeDocuments = getDocumentWithDescriptionFromBefore(asylumCaseBefore, field);
        }
        //filter any document missing from the current list of document
        List<IdValue<DocumentWithDescription>> missingDocuments = beforeDocuments
                .stream()
                .filter(document -> !isDocumentPresent(currentIdValues.orElse(emptyList()), document))
                .collect(Collectors.toList());

        List<IdValue<DocumentWithDescription>> allDocuments = currentIdValues.orElse(emptyList());
        for (IdValue<DocumentWithDescription> documentWithMetadata : missingDocuments) {
            allDocuments = documentWithDescriptionAppender.append(documentWithMetadata.getValue(), allDocuments);
        }

        asylumCase.clear(field);
        asylumCase.write(field, allDocuments);

    }

    private boolean contains(
            List<IdValue<DocumentWithMetadata>> existingDocuments,
            IdValue<DocumentWithMetadata> documentWithMetadata
    ) {

        boolean found = false;

        for (IdValue<DocumentWithMetadata> doc : existingDocuments) {
            Document legalDocument = doc.getValue().getDocument();
            Document document = documentWithMetadata.getValue().getDocument();
            if (legalDocument.getDocumentBinaryUrl().equals(document.getDocumentBinaryUrl())) {
                found = true;
            }
        }

        return found;
    }

    private boolean contains(
            List<IdValue<DocumentWithMetadata>> existingDocuments,
            IdValue<DocumentWithMetadata> documentWithMetadata,String suppliedBy
    ) {

        boolean found = false;

        for (IdValue<DocumentWithMetadata> doc : existingDocuments) {
            Document legalDocument = doc.getValue().getDocument();
            Document document = documentWithMetadata.getValue().getDocument();
            if (legalDocument.getDocumentBinaryUrl().equals(document.getDocumentBinaryUrl())
                && doc.getValue().getSuppliedBy().equals(suppliedBy)) {
                found = true;
            }
        }

        return found;
    }

    private List<AsylumCaseFieldDefinition> getFieldDefinitions(boolean isReheardCase) {
        if (isReheardCase) {
            return Arrays.asList(
                    REHEARD_HEARING_DOCUMENTS,
                    FTPA_APPELLANT_EVIDENCE_DOCUMENTS,
                    FTPA_RESPONDENT_EVIDENCE_DOCUMENTS,
                    FTPA_APPELLANT_DOCUMENTS,
                    FTPA_RESPONDENT_DOCUMENTS,
                    FINAL_DECISION_AND_REASONS_DOCUMENTS
                    );
        } else {
            return Arrays.asList(
                HEARING_DOCUMENTS,
                LEGAL_REPRESENTATIVE_DOCUMENTS,
                ADDITIONAL_EVIDENCE_DOCUMENTS,
                RESPONDENT_DOCUMENTS);
        }
    }

    private List<IdValue<DocumentWithMetadata>> getIdValuesBefore(
            AsylumCase asylumCaseBefore,
            AsylumCaseFieldDefinition fieldDefinition
    ) {

        if (asylumCaseBefore != null) {
            Optional<List<IdValue<DocumentWithMetadata>>> idValuesBeforeOptional = asylumCaseBefore
                    .read(fieldDefinition);
            if (idValuesBeforeOptional.isPresent()) {
                return idValuesBeforeOptional.get();
            }
        }

        return Collections.emptyList();
    }

    private List<IdValue<DocumentWithDescription>> getDocumentWithDescriptionFromBefore(
            AsylumCase asylumCaseBefore,
            AsylumCaseFieldDefinition fieldDefinition
    ) {

        if (asylumCaseBefore != null) {
            Optional<List<IdValue<DocumentWithDescription>>> idValuesBeforeOptional = asylumCaseBefore
                    .read(fieldDefinition);
            if (idValuesBeforeOptional.isPresent()) {
                return idValuesBeforeOptional.get();
            }
        }

        return Collections.emptyList();
    }

    private String getBundlePrefix(AsylumCase asylumCase) {

        final String appealReferenceNumber =
                asylumCase
                        .read(AsylumCaseFieldDefinition.APPEAL_REFERENCE_NUMBER, String.class)
                        .orElseThrow(() -> new IllegalStateException("appealReferenceNumber is not present"));

        final String appellantFamilyName =
                asylumCase
                        .read(AsylumCaseFieldDefinition.APPELLANT_FAMILY_NAME, String.class)
                        .orElseThrow(() -> new IllegalStateException("appellantFamilyName is not present"));

        return appealReferenceNumber.replace("/", " ")
                + "-" + appellantFamilyName;
    }

    private void prepareFtpaRespondentEvidenceDocuments(AsylumCase asylumCase, AsylumCaseFieldDefinition sourceField, AsylumCaseFieldDefinition targetField) {
        if (!asylumCase.read(sourceField).isPresent()) {
            return;
        }
        List<IdValue<DocumentWithDescription>> targetDocuments = getDocumentWithDescriptionFromBefore(asylumCase, targetField);

        Optional<List<IdValue<DocumentWithDescription>>> maybeDocuments = asylumCase.read(sourceField);

        List<IdValue<DocumentWithDescription>> documents = maybeDocuments.orElse(emptyList());

        List<IdValue<DocumentWithDescription>> customDocuments = new ArrayList<>();
        if (documents.size() > 0) {
            for (IdValue<DocumentWithDescription> documentWithDescriptionIdValue : documents) {
                //if the any document is missing the tag, add the appropriate tag to it.
                Optional<IdValue<DocumentWithDescription>> maybeDocument = isFtpaRespondentEvidenceDocumentPresent(targetDocuments, documentWithDescriptionIdValue);
                if (maybeDocument.isPresent()) {
                    customDocuments = documentWithDescriptionAppender.append(maybeDocument.get().getValue(), customDocuments);
                } else {
                    customDocuments = documentWithDescriptionAppender.append(documentWithDescriptionIdValue.getValue(), customDocuments);
                }

            }
        }
        if (!customDocuments.isEmpty()) {
            asylumCase.clear(targetField);
            asylumCase.write(targetField, customDocuments);
        }

    }

    private void prepareDocuments(AsylumCase asylumCase, AsylumCaseFieldDefinition sourceField, AsylumCaseFieldDefinition targetField) {
        if (targetField == FTPA_RESPONDENT_EVIDENCE_DOCUMENTS || targetField == FTPA_APPELLANT_EVIDENCE_DOCUMENTS) {
            prepareFtpaRespondentEvidenceDocuments(asylumCase,sourceField,targetField);
            return;
        }

        if (!asylumCase.read(sourceField).isPresent()) {
            return;
        }

        List<IdValue<DocumentWithMetadata>> targetDocuments = getIdValuesBefore(asylumCase, targetField);

        Optional<List<IdValue<DocumentWithDescription>>> maybeDocuments =
                asylumCase.read(sourceField);

        List<IdValue<DocumentWithDescription>> documents =
                maybeDocuments.orElse(emptyList());

        List<IdValue<DocumentWithMetadata>> customDocuments = new ArrayList<>();

        if (documents != null && documents.size() > 0) {

            for (IdValue<DocumentWithDescription> documentWithDescription : documents) {
                //if the any document is missing the tag, add the appropriate tag to it.
                Optional<IdValue<DocumentWithMetadata>> maybeDocument = isDocumentWithDescriptionPresent(targetDocuments, documentWithDescription);
                Document document = documentWithDescription.getValue().getDocument().orElseThrow(() -> new IllegalStateException(missingDocumentExceptionMessage));

                DocumentWithMetadata newDocumentWithMetadata = null;
                if (maybeDocument.isPresent()) {
                    newDocumentWithMetadata = new DocumentWithMetadata(document,
                            documentWithDescription.getValue().getDescription().orElse(""),
                            dateProvider.now().toString(),
                            maybeDocument.get().getValue().getTag(),
                            "");
                } else {
                    switch (sourceField) {
                        case CUSTOM_HEARING_DOCUMENTS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.HEARING_NOTICE,
                                    "");
                            break;
                        case CUSTOM_REHEARD_HEARING_DOCS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.REHEARD_HEARING_NOTICE,
                                    "");
                            break;
                        case CUSTOM_LEGAL_REP_DOCUMENTS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.CASE_ARGUMENT,
                                    "");
                            break;
                        case CUSTOM_ADDITIONAL_EVIDENCE_DOCUMENTS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.ADDITIONAL_EVIDENCE,
                                    "");
                            break;
                        case CUSTOM_RESPONDENT_DOCUMENTS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.RESPONDENT_EVIDENCE,
                                    "");
                            break;
                        case CUSTOM_FTPA_APPELLANT_DOCS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.FTPA_APPELLANT,
                                    "");
                            break;
                        case CUSTOM_FTPA_RESPONDENT_EVIDENCE_DOCS:
                        case CUSTOM_FTPA_RESPONDENT_DOCS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.FTPA_RESPONDENT,
                                    "");
                            break;
                        case CUSTOM_FINAL_DECISION_AND_REASONS_DOCS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.FTPA_DECISION_AND_REASONS,
                                    "");
                            break;
                        case CUSTOM_APP_ADDENDUM_EVIDENCE_DOCS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.ADDENDUM_EVIDENCE,
                                    "The appellant");
                            break;
                        case CUSTOM_RESP_ADDENDUM_EVIDENCE_DOCS:
                            newDocumentWithMetadata = new DocumentWithMetadata(document,
                                    documentWithDescription.getValue().getDescription().orElse(""),
                                    dateProvider.now().toString(),
                                    DocumentTag.ADDENDUM_EVIDENCE,
                                    "The respondent");
                            break;
                        default:break;
                    }
                }
                customDocuments = documentWithMetadataAppender.append(newDocumentWithMetadata, customDocuments);
            }
        }

        asylumCase.clear(targetField);
        asylumCase.write(targetField, customDocuments);
    }
}

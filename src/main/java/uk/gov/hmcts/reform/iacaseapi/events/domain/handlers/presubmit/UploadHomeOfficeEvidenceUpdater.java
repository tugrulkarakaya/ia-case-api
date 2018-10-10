package uk.gov.hmcts.reform.iacaseapi.events.domain.handlers.presubmit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.shared.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.shared.domain.entities.DocumentWithMetadata;
import uk.gov.hmcts.reform.iacaseapi.events.domain.entities.Callback;
import uk.gov.hmcts.reform.iacaseapi.events.domain.entities.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.events.domain.entities.CallbackStage;
import uk.gov.hmcts.reform.iacaseapi.shared.domain.entities.ccd.EventId;
import uk.gov.hmcts.reform.iacaseapi.events.domain.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.iacaseapi.events.domain.service.DocumentAppender;
import uk.gov.hmcts.reform.iacaseapi.events.domain.service.SentDirectionCompleter;

@Component
public class UploadHomeOfficeEvidenceUpdater implements PreSubmitCallbackHandler<AsylumCase> {

    private final DocumentAppender documentAppender;
    private final SentDirectionCompleter sentDirectionCompleter;

    public UploadHomeOfficeEvidenceUpdater(
        @Autowired DocumentAppender documentAppender,
        @Autowired SentDirectionCompleter sentDirectionCompleter
    ) {
        this.documentAppender = documentAppender;
        this.sentDirectionCompleter = sentDirectionCompleter;
    }

    public boolean canHandle(
        CallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        return callbackStage == CallbackStage.ABOUT_TO_SUBMIT
               && callback.getEventId() == EventId.UPLOAD_HOME_OFFICE_EVIDENCE;
    }

    public PreSubmitCallbackResponse<AsylumCase> handle(
        CallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle ccd event");
        }

        AsylumCase asylumCase =
            callback
                .getCaseDetails()
                .getCaseData();

        PreSubmitCallbackResponse<AsylumCase> preSubmitResponse =
            new PreSubmitCallbackResponse<>(asylumCase);

        DocumentWithMetadata homeOfficeEvidence =
            asylumCase
                .getHomeOfficeEvidence()
                .orElseThrow(() -> new IllegalStateException("homeOfficeEvidence not present"));

        documentAppender.append(asylumCase, homeOfficeEvidence);

        sentDirectionCompleter.tryMarkAsComplete(asylumCase, "homeOfficeEvidence");

        asylumCase.clearHomeOfficeEvidence();

        return preSubmitResponse;
    }
}

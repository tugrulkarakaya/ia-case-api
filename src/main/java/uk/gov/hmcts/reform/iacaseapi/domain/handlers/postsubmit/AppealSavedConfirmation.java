package uk.gov.hmcts.reform.iacaseapi.domain.handlers.postsubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.APPEAL_TYPE;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.EA_HU_APPEAL_TYPE_PAYMENT_OPTION;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.PA_APPEAL_TYPE_PAYMENT_OPTION;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AppealType;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PostSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PostSubmitCallbackHandler;

@Component
public class AppealSavedConfirmation implements PostSubmitCallbackHandler<AsylumCase> {

    public boolean canHandle(
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callback, "callback must not be null");

        return callback.getEvent() == Event.START_APPEAL || callback.getEvent() == Event.EDIT_APPEAL;
    }

    public PostSubmitCallbackResponse handle(
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        PostSubmitCallbackResponse postSubmitResponse =
            new PostSubmitCallbackResponse();

        final AsylumCase asylumCase = callback.getCaseDetails().getCaseData();

        String paymentOption = null;

        AppealType appealType = asylumCase.read(APPEAL_TYPE, AppealType.class)
            .orElseThrow(() -> new IllegalStateException("AppealType is not present"));

        if (appealType == AppealType.EA || appealType == AppealType.HU) {
            paymentOption = asylumCase
                .read(EA_HU_APPEAL_TYPE_PAYMENT_OPTION, String.class)
                .orElse("");
        } else if (appealType == AppealType.PA) {
            paymentOption = asylumCase
                .read(PA_APPEAL_TYPE_PAYMENT_OPTION, String.class)
                .orElse("");
        }

        String submitPaymentAppealUrl = "";
        String payOrSubmitLabel = "";

        if (paymentOption != null && paymentOption.equals("payNow")) {
            submitPaymentAppealUrl = "/trigger/payAndSubmitAppeal";
            payOrSubmitLabel = "pay for and submit your appeal";
        } else {
            submitPaymentAppealUrl = "/trigger/submitAppeal";
            payOrSubmitLabel = "submit your appeal";
        }

        postSubmitResponse.setConfirmationHeader("# Your appeal details have been saved\n# You still need to submit it");
        postSubmitResponse.setConfirmationBody(
            "### Do this next\n\n"
            + "If you're ready to proceed [" + payOrSubmitLabel + "](/case/IA/Asylum/"
            + callback.getCaseDetails().getId() +  submitPaymentAppealUrl + ").\n\n"
            + "#### Not ready to submit yet?\n"
            + "You can return to the case details to make changes."
        );

        return postSubmitResponse;
    }
}

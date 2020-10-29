package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.PaymentStatus.PAYMENT_PENDING;

import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AppealType;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.RemissionType;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.PaymentStatus;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.iacaseapi.domain.service.FeePayment;

@Component
public class FeePaymentHandler implements PreSubmitCallbackHandler<AsylumCase> {

    private final FeePayment<AsylumCase> feePayment;
    private final boolean isfeePaymentEnabled;

    public FeePaymentHandler(
        @Value("${featureFlag.isfeePaymentEnabled}") boolean isfeePaymentEnabled,
        FeePayment<AsylumCase> feePayment
    ) {
        this.feePayment = feePayment;
        this.isfeePaymentEnabled = isfeePaymentEnabled;
    }

    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {

        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return (callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT)
               && Arrays.asList(
            Event.START_APPEAL,
            Event.EDIT_APPEAL,
            Event.PAYMENT_APPEAL
        ).contains(callback.getEvent())
               && isfeePaymentEnabled;
    }

    public PreSubmitCallbackResponse<AsylumCase> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        AsylumCase asylumCase =
            callback
                .getCaseDetails()
                .getCaseData();

        AppealType appealType = asylumCase.read(APPEAL_TYPE, AppealType.class)
            .orElseThrow(() -> new IllegalStateException("Appeal type is not present"));

        YesOrNo isRemissionsEnabled = asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class)
            .orElse(YesOrNo.NO);

        switch (appealType) {
            case EA:
            case HU:
            case PA:
                Optional<RemissionType> optRemissionType = asylumCase.read(REMISSION_TYPE, RemissionType.class);
                if (isRemissionsEnabled == YesOrNo.YES
                    && optRemissionType.isPresent()
                    && optRemissionType.get() == RemissionType.HO_WAIVER_REMISSION) {

                    setFeeRemissionTypeDetails(asylumCase);
                } else {
                    asylumCase = feePayment.aboutToSubmit(callback);
                    setFeePaymentDetails(asylumCase, appealType);
                }
                asylumCase.clear(RP_DC_APPEAL_HEARING_OPTION);
                break;

            case DC:
            case RP:
                String hearingOption = asylumCase.read(RP_DC_APPEAL_HEARING_OPTION, String.class)
                    .orElseThrow(() -> new IllegalStateException("Appeal hearing option is not present"));
                asylumCase.write(DECISION_HEARING_FEE_OPTION, hearingOption);
                asylumCase.clear(HEARING_DECISION_SELECTED);
                asylumCase.clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
                asylumCase.clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
                asylumCase.clear(PAYMENT_STATUS);
                asylumCase.clear(FEE_REMISSION_TYPE);
                asylumCase.clear(REMISSION_TYPE);
                asylumCase.clear(REMISSION_CLAIM);
                break;

            default:
                break;
        }

        return new PreSubmitCallbackResponse<>(asylumCase);
    }

    private void setFeeRemissionTypeDetails(AsylumCase asylumCase) {

        String remissionClaim = asylumCase.read(REMISSION_CLAIM, String.class)
            .orElse("");
        switch (remissionClaim) {
            case "asylumSupport":
                asylumCase.write(FEE_REMISSION_TYPE, "Asylum support");
                asylumCase.clear(LEGAL_AID_ACCOUNT_NUMBER);
                break;

            case "legalAid":
                asylumCase.write(FEE_REMISSION_TYPE, "Legal Aid");
                asylumCase.clear(ASYLUM_SUPPORT_REFERENCE);
                asylumCase.clear(ASYLUM_SUPPORT_DOCUMENT);
                break;

            default:
                break;
        }

        asylumCase.clear(DECISION_HEARING_FEE_OPTION);
        asylumCase.clear(HEARING_DECISION_SELECTED);
        asylumCase.clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        asylumCase.clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        asylumCase.clear(PAYMENT_STATUS);
    }

    private void setFeePaymentDetails(AsylumCase asylumCase, AppealType appealType) {

        asylumCase.write(AsylumCaseFieldDefinition.IS_FEE_PAYMENT_ENABLED,
            isfeePaymentEnabled ? YesOrNo.YES : YesOrNo.NO);

        if (!asylumCase.read(PAYMENT_STATUS, PaymentStatus.class).isPresent()) {
            asylumCase.write(PAYMENT_STATUS, PAYMENT_PENDING);
        }

        if (appealType == AppealType.PA) {
            asylumCase.clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        } else {
            asylumCase.clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        }
        asylumCase.clear(FEE_REMISSION_TYPE);
        asylumCase.clear(REMISSION_TYPE);
        asylumCase.clear(REMISSION_CLAIM);
    }
}

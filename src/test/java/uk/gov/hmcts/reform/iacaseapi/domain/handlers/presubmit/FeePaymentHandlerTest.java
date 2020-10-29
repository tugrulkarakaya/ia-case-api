package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.PAYMENT_STATUS;

import java.util.Arrays;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AppealType;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.RemissionType;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.PaymentStatus;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;
import uk.gov.hmcts.reform.iacaseapi.domain.service.FeePayment;


@RunWith(JUnitParamsRunner.class)
@SuppressWarnings("unchecked")
public class FeePaymentHandlerTest {

    @Mock private FeePayment<AsylumCase> feePayment;
    @Mock private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private AsylumCase asylumCase;

    private FeePaymentHandler feePaymentHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        feePaymentHandler =
            new FeePaymentHandler(true, feePayment);
    }

    @Test
    public void should_make_feePayment_and_update_the_case() {

        Arrays.asList(
            Event.PAYMENT_APPEAL
        ).forEach(event -> {

            when(callback.getEvent()).thenReturn(event);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(asylumCase);
            when(feePayment.aboutToSubmit(callback)).thenReturn(asylumCase);
            when(asylumCase.read(APPEAL_TYPE, AppealType.class))
                .thenReturn(Optional.of(AppealType.PA));

            PreSubmitCallbackResponse<AsylumCase> callbackResponse =
                feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

            assertNotNull(callbackResponse);
            assertEquals(asylumCase, callbackResponse.getData());

            verify(feePayment, times(1)).aboutToSubmit(callback);

            reset(callback);
            reset(feePayment);
        });
    }

    @Test
    public void should_clear_other_when_pa_offline_payment() {

        Arrays.asList(
            Event.START_APPEAL
        ).forEach(event -> {

            when(callback.getEvent()).thenReturn(event);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(asylumCase);
            when(feePayment.aboutToSubmit(callback)).thenReturn(asylumCase);
            when(asylumCase.read(APPEAL_TYPE,
                AppealType.class)).thenReturn(Optional.of(AppealType.PA));

            PreSubmitCallbackResponse<AsylumCase> callbackResponse =
                feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

            assertNotNull(callbackResponse);
            assertEquals(asylumCase, callbackResponse.getData());

            verify(feePayment, times(1)).aboutToSubmit(callback);
            verify(asylumCase, times(1))
                .write(PAYMENT_STATUS, PaymentStatus.PAYMENT_PENDING);
            verify(asylumCase, times(1))
                .write(IS_FEE_PAYMENT_ENABLED, YesOrNo.YES);
            verify(asylumCase, times(1))
                .clear(ASYLUM_SUPPORT_REFERENCE);
            verify(asylumCase, times(1))
                .clear(ASYLUM_SUPPORT_DOCUMENT);
            verify(asylumCase, times(1))
                .clear(LEGAL_AID_ACCOUNT_NUMBER);
            verify(asylumCase, times(1))
                .clear(LEGAL_AID_ACCOUNT_NUMBER);
            verify(asylumCase, times(1))
                .clear(SECTION17_DOCUMENT);
            verify(asylumCase, times(1))
                .clear(SECTION20_DOCUMENT);
            verify(asylumCase, times(1))
                .clear(HOME_OFFICE_WAIVER_DOCUMENT);
            verify(asylumCase, times(1))
                .clear(RP_DC_APPEAL_HEARING_OPTION);
            verify(asylumCase, times(1))
                .clear(REMISSION_CLAIM);
            verify(asylumCase, times(1))
                .clear(FEE_REMISSION_TYPE);
            reset(callback);
            reset(feePayment);
        });
    }

    @Test
    public void should_clear_other_when_hu_offline_payment() {

        Arrays.asList(
            Event.START_APPEAL
        ).forEach(event -> {

            when(callback.getEvent()).thenReturn(event);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(asylumCase);
            when(feePayment.aboutToSubmit(callback)).thenReturn(asylumCase);
            when(asylumCase.read(APPEAL_TYPE,
                AppealType.class)).thenReturn(Optional.of(AppealType.HU));

            PreSubmitCallbackResponse<AsylumCase> callbackResponse =
                feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

            assertNotNull(callbackResponse);
            assertEquals(asylumCase, callbackResponse.getData());

            verify(feePayment, times(1)).aboutToSubmit(callback);
            verify(asylumCase, times(1))
                .write(PAYMENT_STATUS, PaymentStatus.PAYMENT_PENDING);
            verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
            reset(callback);
            reset(feePayment);
        });
    }

    @Test
    public void should_clear_other_when_ea_offline_payment() {

        Arrays.asList(
            Event.START_APPEAL
        ).forEach(event -> {

            when(callback.getEvent()).thenReturn(event);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(asylumCase);
            when(feePayment.aboutToSubmit(callback)).thenReturn(asylumCase);
            when(asylumCase.read(APPEAL_TYPE,
                AppealType.class)).thenReturn(Optional.of(AppealType.EA));

            PreSubmitCallbackResponse<AsylumCase> callbackResponse =
                feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

            assertNotNull(callbackResponse);
            assertEquals(asylumCase, callbackResponse.getData());

            verify(feePayment, times(1)).aboutToSubmit(callback);
            verify(asylumCase, times(1))
                .write(PAYMENT_STATUS, PaymentStatus.PAYMENT_PENDING);
            verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
            reset(callback);
            reset(feePayment);
        });
    }

    @Test
    @Parameters({ "DC", "RP" })
    public void should_clear_all_payment_details_for_non_payment_appeal_type(String type) {

        Arrays.asList(
            Event.START_APPEAL
        ).forEach(event -> {

            when(callback.getEvent()).thenReturn(event);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(asylumCase);
            when(feePayment.aboutToSubmit(callback)).thenReturn(asylumCase);
            when(asylumCase.read(APPEAL_TYPE,
                AppealType.class)).thenReturn(Optional.of(AppealType.valueOf(type)));
            when(asylumCase.read(RP_DC_APPEAL_HEARING_OPTION, String.class))
                .thenReturn(Optional.of("decisionWithoutHearing"));

            PreSubmitCallbackResponse<AsylumCase> callbackResponse =
                feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

            assertNotNull(callbackResponse);
            assertEquals(asylumCase, callbackResponse.getData());

            verify(asylumCase, times(1))
                .write(DECISION_HEARING_FEE_OPTION, "decisionWithoutHearing");
            verify(asylumCase, times(1))
                .clear(HEARING_DECISION_SELECTED);
            verify(asylumCase, times(1))
                .clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
            verify(asylumCase, times(1))
                .clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
            verify(asylumCase, times(1))
                .clear(PAYMENT_STATUS);
            verify(asylumCase, times(1)).clear(FEE_REMISSION_TYPE);
            verify(asylumCase, times(1)).clear(REMISSION_TYPE);
            verify(asylumCase, times(1)).clear(REMISSION_CLAIM);
            verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_REFERENCE);
            verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_DOCUMENT);
            verify(asylumCase, times(1)).clear(LEGAL_AID_ACCOUNT_NUMBER);
            verify(asylumCase, times(1)).clear(SECTION17_DOCUMENT);
            verify(asylumCase, times(1)).clear(SECTION20_DOCUMENT);
            verify(asylumCase, times(1)).clear(HOME_OFFICE_WAIVER_DOCUMENT);
            reset(callback);
            reset(feePayment);
        });
    }

    @Test
    public void should_return_remission_for_asylum_support() {

        when(callback.getEvent()).thenReturn(Event.EDIT_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE, AppealType.class))
            .thenReturn(Optional.of(AppealType.EA));
        when(asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class))
            .thenReturn(Optional.of(YesOrNo.YES));
        when(asylumCase.read(REMISSION_TYPE, RemissionType.class))
            .thenReturn(Optional.of(RemissionType.HO_WAIVER_REMISSION));
        when(asylumCase.read(REMISSION_CLAIM, String.class))
            .thenReturn(Optional.of("asylumSupport"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(feePayment, times(0)).aboutToSubmit(callback);
        verify(asylumCase, times(1)).write(FEE_REMISSION_TYPE, "Asylum support");
        verify(asylumCase, times(1)).clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(LEGAL_AID_ACCOUNT_NUMBER);
    }

    @Test
    public void should_return_remission_for_legal_aid() {

        when(callback.getEvent()).thenReturn(Event.EDIT_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE, AppealType.class))
            .thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class))
            .thenReturn(Optional.of(YesOrNo.YES));
        when(asylumCase.read(REMISSION_TYPE, RemissionType.class))
            .thenReturn(Optional.of(RemissionType.HO_WAIVER_REMISSION));
        when(asylumCase.read(REMISSION_CLAIM, String.class))
            .thenReturn(Optional.of("legalAid"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(feePayment, times(0)).aboutToSubmit(callback);
        verify(asylumCase, times(1)).write(FEE_REMISSION_TYPE, "Legal Aid");
        verify(asylumCase, times(1)).clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_REFERENCE);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_DOCUMENT);
    }

    @Test
    public void should_not_return_remission_for_remissions_not_enabled() {
        when(callback.getEvent()).thenReturn(Event.EDIT_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE, AppealType.class))
            .thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class))
            .thenReturn(Optional.of(YesOrNo.NO));
        when(feePayment.aboutToSubmit(callback)).thenReturn(asylumCase);

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(feePayment, times(1)).aboutToSubmit(callback);
        verify(asylumCase, times(0)).write(FEE_REMISSION_TYPE, "Legal Aid");
        verify(asylumCase, times(1)).clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(0)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(RP_DC_APPEAL_HEARING_OPTION);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_REFERENCE);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_DOCUMENT);
    }

    @Test
    public void it_cannot_handle_callback_if_feepayment_not_enabled() {

        FeePaymentHandler feePaymentHandlerWithDisabledPayment =
            new FeePaymentHandler(
                false,
                feePayment
            );

        assertThatThrownBy(() -> feePaymentHandlerWithDisabledPayment.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void handling_should_throw_if_cannot_actually_handle() {

        assertThatThrownBy(() -> feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_START, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);

        when(callback.getEvent()).thenReturn(Event.SEND_DIRECTION);
        assertThatThrownBy(() -> feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void it_can_handle_callback() {

        for (Event event : Event.values()) {

            when(callback.getEvent()).thenReturn(event);

            for (PreSubmitCallbackStage callbackStage : PreSubmitCallbackStage.values()) {

                boolean canHandle = feePaymentHandler.canHandle(callbackStage, callback);

                if ((callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT)
                    && (callback.getEvent() == Event.START_APPEAL
                        || callback.getEvent() == Event.EDIT_APPEAL
                        || callback.getEvent() == Event.PAYMENT_APPEAL)) {

                    assertTrue(canHandle);
                } else {
                    assertFalse(canHandle);
                }
            }

            reset(callback);
        }
    }

    @Test
    public void it_cannot_handle_callback_if_feePayment_not_enabled() {

        feePaymentHandler =
            new FeePaymentHandler(false, feePayment);

        for (Event event : Event.values()) {

            when(callback.getEvent()).thenReturn(event);

            for (PreSubmitCallbackStage callbackStage : PreSubmitCallbackStage.values()) {
                boolean canHandle = feePaymentHandler.canHandle(callbackStage, callback);

                assertFalse(canHandle);
            }

            reset(callback);
        }
    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> feePaymentHandler.canHandle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> feePaymentHandler.canHandle(PreSubmitCallbackStage.ABOUT_TO_START, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> feePaymentHandler.canHandle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> feePaymentHandler.handle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_START, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    @Parameters({ "DC", "RP" })
    public void should_throw_for_missing_appeal_hearing_option(String type) {

        when(callback.getEvent()).thenReturn(Event.START_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(feePayment.aboutToSubmit(callback)).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE,
            AppealType.class)).thenReturn(Optional.of(AppealType.valueOf(type)));

        assertThatThrownBy(() -> feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Appeal hearing option is not present");
    }

    @Test
    public void should_return_data_for_valid_asylumSupport_remission_type() {

        when(callback.getEvent()).thenReturn(Event.EDIT_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE, AppealType.class))
            .thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class))
            .thenReturn(Optional.of(YesOrNo.YES));
        when(asylumCase.read(REMISSION_TYPE, RemissionType.class))
            .thenReturn(Optional.of(RemissionType.HO_WAIVER_REMISSION));
        when(asylumCase.read(REMISSION_CLAIM, String.class))
            .thenReturn(Optional.of("asylumSupport"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).write(FEE_REMISSION_TYPE, "Asylum support");
        verify(asylumCase, times(1)).clear(LEGAL_AID_ACCOUNT_NUMBER);
        verify(asylumCase, times(1)).clear(SECTION17_DOCUMENT);
        verify(asylumCase, times(1)).clear(SECTION20_DOCUMENT);
        verify(asylumCase, times(1)).clear(HOME_OFFICE_WAIVER_DOCUMENT);

        verify(asylumCase, times(1)).clear(DECISION_HEARING_FEE_OPTION);
        verify(asylumCase, times(1)).clear(HEARING_DECISION_SELECTED);
        verify(asylumCase, times(1)).clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PAYMENT_STATUS);
    }

    @Test
    public void should_return_data_for_valid_legalAid_remission_type() {

        when(callback.getEvent()).thenReturn(Event.EDIT_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE, AppealType.class))
            .thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class))
            .thenReturn(Optional.of(YesOrNo.YES));
        when(asylumCase.read(REMISSION_TYPE, RemissionType.class))
            .thenReturn(Optional.of(RemissionType.HO_WAIVER_REMISSION));
        when(asylumCase.read(REMISSION_CLAIM, String.class))
            .thenReturn(Optional.of("legalAid"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).write(FEE_REMISSION_TYPE, "Legal Aid");
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_REFERENCE);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_DOCUMENT);
        verify(asylumCase, times(1)).clear(SECTION17_DOCUMENT);
        verify(asylumCase, times(1)).clear(SECTION20_DOCUMENT);
        verify(asylumCase, times(1)).clear(HOME_OFFICE_WAIVER_DOCUMENT);

        verify(asylumCase, times(1)).clear(DECISION_HEARING_FEE_OPTION);
        verify(asylumCase, times(1)).clear(HEARING_DECISION_SELECTED);
        verify(asylumCase, times(1)).clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PAYMENT_STATUS);

    }

    @Test
    public void should_return_data_for_valid_section17_remission_type() {

        when(callback.getEvent()).thenReturn(Event.EDIT_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE, AppealType.class))
            .thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class))
            .thenReturn(Optional.of(YesOrNo.YES));
        when(asylumCase.read(REMISSION_TYPE, RemissionType.class))
            .thenReturn(Optional.of(RemissionType.HO_WAIVER_REMISSION));
        when(asylumCase.read(REMISSION_CLAIM, String.class))
            .thenReturn(Optional.of("section17"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).write(FEE_REMISSION_TYPE, "Section 17");
        verify(asylumCase, times(1)).clear(LEGAL_AID_ACCOUNT_NUMBER);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_REFERENCE);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_DOCUMENT);
        verify(asylumCase, times(1)).clear(SECTION20_DOCUMENT);
        verify(asylumCase, times(1)).clear(HOME_OFFICE_WAIVER_DOCUMENT);

        verify(asylumCase, times(1)).clear(DECISION_HEARING_FEE_OPTION);
        verify(asylumCase, times(1)).clear(HEARING_DECISION_SELECTED);
        verify(asylumCase, times(1)).clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PAYMENT_STATUS);
    }

    @Test
    public void should_return_data_for_valid_section20_remission_type() {

        when(callback.getEvent()).thenReturn(Event.EDIT_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE, AppealType.class))
            .thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class))
            .thenReturn(Optional.of(YesOrNo.YES));
        when(asylumCase.read(REMISSION_TYPE, RemissionType.class))
            .thenReturn(Optional.of(RemissionType.HO_WAIVER_REMISSION));
        when(asylumCase.read(REMISSION_CLAIM, String.class))
            .thenReturn(Optional.of("section20"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).write(FEE_REMISSION_TYPE, "Section 20");
        verify(asylumCase, times(1)).clear(LEGAL_AID_ACCOUNT_NUMBER);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_REFERENCE);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_DOCUMENT);
        verify(asylumCase, times(1)).clear(SECTION17_DOCUMENT);
        verify(asylumCase, times(1)).clear(HOME_OFFICE_WAIVER_DOCUMENT);

        verify(asylumCase, times(1)).clear(DECISION_HEARING_FEE_OPTION);
        verify(asylumCase, times(1)).clear(HEARING_DECISION_SELECTED);
        verify(asylumCase, times(1)).clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PAYMENT_STATUS);
    }

    @Test
    public void should_return_data_for_valid_homeOfficeWaiver_remission_type() {

        when(callback.getEvent()).thenReturn(Event.EDIT_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(APPEAL_TYPE, AppealType.class))
            .thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.read(IS_REMISSIONS_ENABLED, YesOrNo.class))
            .thenReturn(Optional.of(YesOrNo.YES));
        when(asylumCase.read(REMISSION_TYPE, RemissionType.class))
            .thenReturn(Optional.of(RemissionType.HO_WAIVER_REMISSION));
        when(asylumCase.read(REMISSION_CLAIM, String.class))
            .thenReturn(Optional.of("homeOfficeWaiver"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            feePaymentHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).write(FEE_REMISSION_TYPE, "Home Office waiver");
        verify(asylumCase, times(1)).clear(LEGAL_AID_ACCOUNT_NUMBER);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_REFERENCE);
        verify(asylumCase, times(1)).clear(ASYLUM_SUPPORT_DOCUMENT);
        verify(asylumCase, times(1)).clear(SECTION17_DOCUMENT);
        verify(asylumCase, times(1)).clear(SECTION20_DOCUMENT);

        verify(asylumCase, times(1)).clear(DECISION_HEARING_FEE_OPTION);
        verify(asylumCase, times(1)).clear(HEARING_DECISION_SELECTED);
        verify(asylumCase, times(1)).clear(EA_HU_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PA_APPEAL_TYPE_PAYMENT_OPTION);
        verify(asylumCase, times(1)).clear(PAYMENT_STATUS);
    }
}

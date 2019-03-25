package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage.ABOUT_TO_START;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage.ABOUT_TO_SUBMIT;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AppealType;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.service.AppealReferenceNumberGenerator;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class AppealReferenceNumberHandlerTest {

    @Mock private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private AsylumCase asylumCase;

    @Mock private AppealReferenceNumberGenerator appealReferenceNumberGenerator;

    private AppealReferenceNumberHandler appealReferenceNumberHandler;

    @Before
    public void setUp() {

        appealReferenceNumberHandler =
            new AppealReferenceNumberHandler(appealReferenceNumberGenerator);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(123L);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
    }

    @Test
    public void should_set_draft_appeal_reference_when_appeal_started() {

        when(callback.getEvent()).thenReturn(Event.START_APPEAL);

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            appealReferenceNumberHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).setAppealReferenceNumber("DRAFT");

        verifyZeroInteractions(appealReferenceNumberGenerator);
    }

    @Test
    public void should_set_next_appeal_reference_number_to_replace_draft_when_appeal_submitted() {

        when(callback.getEvent()).thenReturn(Event.SUBMIT_APPEAL);

        when(appealReferenceNumberGenerator.generate(123, AppealType.PA))
            .thenReturn("the-next-appeal-reference-number");

        when(asylumCase.getAppealType()).thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.getAppealReferenceNumber()).thenReturn(Optional.of("DRAFT"));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            appealReferenceNumberHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).setAppealReferenceNumber("the-next-appeal-reference-number");
    }

    @Test
    public void should_set_next_appeal_reference_number_if_not_present() {

        when(callback.getEvent()).thenReturn(Event.SUBMIT_APPEAL);

        when(appealReferenceNumberGenerator.generate(123, AppealType.PA))
            .thenReturn("the-next-appeal-reference-number");

        when(asylumCase.getAppealType()).thenReturn(Optional.of(AppealType.PA));
        when(asylumCase.getAppealReferenceNumber()).thenReturn(Optional.empty());

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            appealReferenceNumberHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).setAppealReferenceNumber("the-next-appeal-reference-number");
    }

    @Test
    public void should_do_nothing_if_non_draft_number_already_present() {

        Optional<String> appealReference = Optional.of("some-existing-reference-number");

        when(asylumCase.getAppealReferenceNumber()).thenReturn(appealReference);
        when(callback.getEvent()).thenReturn(Event.SUBMIT_APPEAL);

        appealReferenceNumberHandler.handle(ABOUT_TO_SUBMIT, callback);

        verifyZeroInteractions(appealReferenceNumberGenerator);
        verify(asylumCase, never()).setAppealReferenceNumber(any());
    }

    @Test
    public void it_can_handle_callback() {

        for (Event event : Event.values()) {

            when(callback.getEvent()).thenReturn(event);

            for (PreSubmitCallbackStage callbackStage : PreSubmitCallbackStage.values()) {

                boolean canHandle = appealReferenceNumberHandler.canHandle(callbackStage, callback);

                if ((event == Event.START_APPEAL
                     || event == Event.SUBMIT_APPEAL)
                    && callbackStage == ABOUT_TO_SUBMIT) {

                    assertTrue(canHandle);
                } else {
                    assertFalse(canHandle);
                }
            }

            reset(callback);
        }
    }

    @Test
    public void handling_should_throw_if_cannot_actually_handle() {

        assertThatThrownBy(() -> appealReferenceNumberHandler.handle(ABOUT_TO_START, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> appealReferenceNumberHandler.canHandle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> appealReferenceNumberHandler.canHandle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> appealReferenceNumberHandler.handle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> appealReferenceNumberHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }
}
package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit.flagcase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;

import java.util.*;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.*;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.IdValue;

@RunWith(JUnitParamsRunner.class)
public class RemoveFlagHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private AsylumCase asylumCase;
    private final CaseFlag expectedCaseFlag =
        new CaseFlag(CaseFlagType.COMPLEX_CASE, "some complex flag additional info");
    private final CaseFlag expectedCaseFlag2 =
        new CaseFlag(CaseFlagType.ANONYMITY, "some anonymity flag additional info");

    private List<IdValue<CaseFlag>> expectedList;
    private IdValue<CaseFlag> expectedIdValue;
    private RemoveFlagHandler removeFlagHandler;

    @Before
    public void setUp() {
        when(callback.getEvent()).thenReturn(Event.REMOVE_FLAG);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetails().getCaseData()).thenReturn(asylumCase);

        removeFlagHandler = new RemoveFlagHandler();
        expectedIdValue = new IdValue<>("2", expectedCaseFlag2);
        expectedList = Arrays.asList(
            new IdValue<>("1", expectedCaseFlag),
            expectedIdValue
        );
    }

    @Test
    public void removes_a_specified_flag() {
        final List<Value> expectedElements = Optional.of(expectedList)
            .orElse(Collections.emptyList())
            .stream()
            .map(idValue -> new Value(idValue.getId(), idValue.getValue().getCaseFlagType().getReadableText()))
            .collect(Collectors.toList());

        final DynamicList dynamicList = new DynamicList(expectedElements.get(0), expectedElements);
        final List<IdValue<CaseFlag>> expectedCaseFlagList = new ArrayList<>();
        expectedCaseFlagList.add(expectedIdValue);

        when(asylumCase.read(REMOVE_FLAG_TYPE_OF_FLAG, DynamicList.class)).thenReturn(Optional.of(dynamicList));
        when(asylumCase.read(CASE_FLAGS)).thenReturn(Optional.of(expectedList));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            removeFlagHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertNotNull(callbackResponse);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).read(REMOVE_FLAG_TYPE_OF_FLAG, DynamicList.class);
        verify(asylumCase, times(1)).read(CASE_FLAGS);

        verify(asylumCase, times(1)).write(CASE_FLAGS, expectedCaseFlagList);
        verify(asylumCase, times(1)).clear(REMOVE_FLAG_TYPE_OF_FLAG);
    }

    @Test
    @Parameters({
        "ANONYMITY, CASE_FLAG_ANONYMITY_EXISTS, CASE_FLAG_ANONYMITY_ADDITIONAL_INFORMATION",
        "COMPLEX_CASE, CASE_FLAG_COMPLEX_CASE_EXISTS, CASE_FLAG_COMPLEX_CASE_ADDITIONAL_INFORMATION",
        "DEPORT, CASE_FLAG_DEPORT_EXISTS, CASE_FLAG_DEPORT_ADDITIONAL_INFORMATION",
        "DETAINED_IMMIGRATION_APPEAL, CASE_FLAG_DETAINED_IMMIGRATION_APPEAL_EXISTS, CASE_FLAG_DETAINED_IMMIGRATION_APPEAL_ADDITIONAL_INFORMATION",
        "FOREIGN_NATIONAL_OFFENDER, CASE_FLAG_FOREIGN_NATIONAL_OFFENDER_EXISTS, CASE_FLAG_FOREIGN_NATIONAL_OFFENDER_ADDITIONAL_INFORMATION",
        "POTENTIALLY_VIOLENT_PERSON, CASE_FLAG_POTENTIALLY_VIOLENT_PERSON_EXISTS, CASE_FLAG_POTENTIALLY_VIOLENT_PERSON_ADDITIONAL_INFORMATION",
        "UNACCEPTABLE_CUSTOMER_BEHAVIOUR, CASE_FLAG_UNACCEPTABLE_CUSTOMER_BEHAVIOUR_EXISTS, CASE_FLAG_UNACCEPTABLE_CUSTOMER_BEHAVIOUR_ADDITIONAL_INFORMATION",
        "UNACCOMPANIED_MINOR, CASE_FLAG_UNACCOMPANIED_MINOR_EXISTS, CASE_FLAG_UNACCOMPANIED_MINOR_ADDITIONAL_INFORMATION",
    })
    public void clear_display_flags_test(CaseFlagType flagType,
                                         AsylumCaseFieldDefinition caseFlagExists,
                                         AsylumCaseFieldDefinition caseFlagAdditionalInformation) {
        removeFlagHandler.clearDisplayFlags(flagType, asylumCase);
        verify(asylumCase, times(1)).clear(caseFlagExists);
        verify(asylumCase, times(1)).clear(caseFlagAdditionalInformation);

    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> removeFlagHandler.canHandle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> removeFlagHandler.canHandle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> removeFlagHandler.handle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> removeFlagHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }
}

package uk.gov.hmcts.reform.iacaseapi.domain.handlers.postsubmit;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.Value;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PostSubmitCallbackResponse;

@RunWith(JUnitParamsRunner.class)
public class AdjournWithoutDateConfirmationTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private AsylumCase asylumCase;

    private AdjournWithoutDateConfirmation handler = new AdjournWithoutDateConfirmation();

    //@Before
    //public void setUp() {
    //
    //    when(callback.getCaseDetails()).thenReturn(caseDetails);
    //    when(caseDetails.getCaseData()).thenReturn(asylumCase);
    //}

    @Test
    @Parameters(method = "generateTestScenarios")
    public void given_callback_can_handle(TestScenario scenario) {
        given(callback.getEvent()).willReturn(scenario.event);

        boolean actualResult = handler.canHandle(callback);

        assertThat(actualResult).isEqualTo(scenario.canBeHandledExpected);
    }

    @Test
    public void should_return_notification_failed_confirmation() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);

        when(callback.getEvent()).thenReturn(Event.ADJOURN_HEARING_WITHOUT_DATE);
        when(asylumCase.read(AsylumCaseFieldDefinition.HOME_OFFICE_ADJOURN_WITHOUT_DATE_INSTRUCT_STATUS, String.class))
            .thenReturn(Optional.of("FAIL"));

        PostSubmitCallbackResponse callbackResponse =
            handler.handle(callback);

        Assert.assertNotNull(callbackResponse);
        Assertions.assertThat(callbackResponse.getConfirmationHeader()).isNotPresent();
        Assert.assertTrue(callbackResponse.getConfirmationBody().isPresent());

        Assert.assertThat(
            callbackResponse.getConfirmationBody().get(),
            containsString("![Respondent notification failed confirmation]"
                           + "(https://raw.githubusercontent.com/hmcts/ia-appeal-frontend/master/app/assets/images/respondent_notification_failed.svg)")
        );

        Assert.assertThat(
            callbackResponse.getConfirmationBody().get(),
            containsString("#### Do this next")
        );
        Assert.assertThat(
            callbackResponse.getConfirmationBody().get(),
            containsString("Contact the respondent to tell them what has changed, including any action they need to take.")
        );
    }

    private List<TestScenario> generateTestScenarios() {
        return TestScenario.testScenarioBuilder();
    }

    @Value
    private static class TestScenario {
        Event event;
        boolean canBeHandledExpected;

        public static List<TestScenario> testScenarioBuilder() {
            List<TestScenario> testScenarioList = new ArrayList<>();
            for (Event e : Event.values()) {
                TestScenario testScenario;
                if (e.equals(Event.ADJOURN_HEARING_WITHOUT_DATE)) {
                    testScenario = new TestScenario(e, true);
                } else {
                    testScenario = new TestScenario(e, false);
                }
                testScenarioList.add(testScenario);
            }
            return testScenarioList;
        }
    }

    @Test
    public void should_not_allow_null_arguments() {
        assertThatThrownBy(() -> handler.canHandle(null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> handler.handle(null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void should_return_confirmation() {
        when(callback.getEvent()).thenReturn(Event.ADJOURN_HEARING_WITHOUT_DATE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);

        PostSubmitCallbackResponse callbackResponse = handler.handle(callback);

        assertNotNull(callbackResponse);
        assertTrue(callbackResponse.getConfirmationHeader().isPresent());
        assertTrue(callbackResponse.getConfirmationBody().isPresent());

        Assert.assertThat(
            callbackResponse.getConfirmationHeader().get(),
            containsString("# The hearing has been adjourned")
        );

        Assert.assertThat(
            callbackResponse.getConfirmationBody().get(),
            containsString("#### What happens next\n\n"
                + "A new Notice of Hearing has been generated."
            )
        );
    }

    @Test
    public void handling_should_throw_if_cannot_actually_handle() {
        assertThatThrownBy(() -> handler.handle(callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }
}

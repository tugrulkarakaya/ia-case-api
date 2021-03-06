package uk.gov.hmcts.reform.iacaseapi.domain.handlers.postsubmit;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PostSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PostSubmitCallbackHandler;


@Component
public class ReviewHearingRequirementsConfirmation implements PostSubmitCallbackHandler<AsylumCase> {

    public boolean canHandle(
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callback, "callback must not be null");

        return Arrays.asList(
            Event.REVIEW_HEARING_REQUIREMENTS,
            Event.LIST_CASE_WITHOUT_HEARING_REQUIREMENTS,
            Event.UPDATE_HEARING_ADJUSTMENTS
        ).contains(callback.getEvent());
    }

    public PostSubmitCallbackResponse handle(
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        PostSubmitCallbackResponse postSubmitResponse =
            new PostSubmitCallbackResponse();

        postSubmitResponse.setConfirmationHeader("# You've recorded the agreed hearing adjustments");
        postSubmitResponse.setConfirmationBody(
            "#### What happens next\n\n"
            + "The listing team will now list the case. All parties will be notified when the Hearing Notice is available to view.<br><br>"
        );

        return postSubmitResponse;
    }
}

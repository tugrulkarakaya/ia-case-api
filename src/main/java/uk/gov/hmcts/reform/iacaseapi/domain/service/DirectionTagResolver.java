package uk.gov.hmcts.reform.iacaseapi.domain.service;

import static java.util.Objects.requireNonNull;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.DirectionTag;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;

@Service
public class DirectionTagResolver {

    public DirectionTag resolve(
        Event event
    ) {
        requireNonNull(event, "event must not be null");

        switch (event) {

            case REQUEST_CASE_EDIT:
                return DirectionTag.CASE_EDIT;

            case REQUEST_RESPONDENT_EVIDENCE:
                return DirectionTag.RESPONDENT_EVIDENCE;

            case REQUEST_RESPONDENT_REVIEW:
                return DirectionTag.RESPONDENT_REVIEW;

            default:
                return DirectionTag.NONE;
        }
    }
}
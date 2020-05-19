package uk.gov.hmcts.reform.iacaseapi.domain.service;

import uk.gov.hmcts.reform.iacaseapi.domain.entities.AppealType;

public interface AppealReferenceNumberGenerator {

    String generate(
        long caseId,
        AppealType appealType
    );

    String update(long caseId, AppealType appealType);
}

package ba.unsa.etf.NBP.dto.excuse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SubmitExcuseRequest(Long courseSessionId, String reason) {

    @JsonCreator
    public SubmitExcuseRequest(@JsonProperty("courseSessionId") Long courseSessionId,
                               @JsonProperty("reason") String reason) {
        this.courseSessionId = courseSessionId;
        this.reason = reason;
    }
}

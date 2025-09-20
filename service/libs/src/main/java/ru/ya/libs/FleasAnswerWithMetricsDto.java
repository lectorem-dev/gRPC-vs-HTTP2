package ru.ya.libs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FleasAnswerWithMetricsDto {
    private FleasAnswerDto answer;

    private long serializationTimeNs;   // client-side serialization
    private long deserializationTimeNs; // server-side deserialization
    private long networkTimeNs;         // approximate RTT
    private long totalTimeNs;           // end-to-end
}
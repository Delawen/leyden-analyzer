package tooling.leyden;

import java.util.Objects;

import org.jline.utils.AttributedString;

public record StatusMessage(Long timestamp, AttributedString message) {
    public StatusMessage {
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(message);
    }
}

package use_case.discovery;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class InteractionBoundaryTest {
    private final UUID token = UUID.randomUUID();
    private final FeedbackEvent event = FeedbackEvent.reaction(UUID.randomUUID(), UUID.randomUUID(), 2, FeedbackEvent.Type.CLOSE);

    @Test void storageFailureProducesOneRetryableReceiptWithOriginalCorrelation() {
        List<InteractionOutputData> output = new ArrayList<>();
        new RecordFeedbackInteractor((sentToken, sentEvent) -> {
            assertEquals(token, sentToken);
            assertSame(event, sentEvent);
            throw new DataAccessException("fixture unavailable");
        }, output::add).execute(new InteractionInputData(token, event));
        assertEquals(1, output.size());
        assertEquals(event.id(), output.get(0).getEventId());
        assertEquals(event.exposureId(), output.get(0).getExposureId());
        assertEquals(InteractionOutputData.Status.RETRYABLE_FAILURE, output.get(0).getStatus());
    }

    @Test void aPresenterFailureNeverRedeliversAnAcceptedInteraction() {
        AtomicInteger writes = new AtomicInteger(), presentations = new AtomicInteger();
        RecordFeedbackInteractor interactor = new RecordFeedbackInteractor((sentToken, sentEvent) -> {
            writes.incrementAndGet();
            return FeedbackResult.APPLIED;
        }, receipt -> {
            presentations.incrementAndGet();
            throw new DataAccessException("fixture presenter failure");
        });
        assertThrows(DataAccessException.class, () -> interactor.execute(new InteractionInputData(token, event)));
        assertEquals(1, writes.get());
        assertEquals(1, presentations.get());
    }
}

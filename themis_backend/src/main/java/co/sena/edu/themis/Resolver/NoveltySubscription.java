package co.sena.edu.themis.Resolver;
import co.sena.edu.themis.Entity.Novelty;
import com.netflix.graphql.dgs.DgsSubscription;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
public class NoveltySubscription {
    private final Sinks.Many<Novelty> noveltyAddedSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Novelty> noveltyUpdatedSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<String> noveltyDeletedSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Novelty> noveltyStatusChangedSink = Sinks.many().multicast().onBackpressureBuffer();

    @DgsSubscription
    public Publisher<Novelty> noveltyAdded(){
        return noveltyAddedSink.asFlux();
    }

    @DgsSubscription
    public Publisher<Novelty> noveltyUpdated(){
        return noveltyUpdatedSink.asFlux();
    }

    @DgsSubscription
    public Publisher<String> noveltyDeleted(){
        return noveltyDeletedSink.asFlux();
    }

    @DgsSubscription
    public Publisher<Novelty> noveltyStatusChanged(){
        return noveltyStatusChangedSink.asFlux();
    }

    public void publishNoveltyAdded(Novelty novelty){
        noveltyAddedSink.tryEmitNext(novelty);
    }

    public void publishNoveltyUpdated(Novelty novelty){
        noveltyUpdatedSink.tryEmitNext(novelty);
    }

    public void publishNoveltyDeleted(String noveltyId){
        noveltyDeletedSink.tryEmitNext(noveltyId);
    }

    public void publishNoveltyStatusChanged(Novelty novelty){
        noveltyStatusChangedSink.tryEmitNext(novelty);
    }
}

package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Dto.NotificationPushDto;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsSubscription;
import com.netflix.graphql.dgs.InputArgument;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@DgsComponent
public class NotificationSubscription {
    private final Sinks.Many<NotificationPushDto> notificationSink = Sinks.many().multicast().onBackpressureBuffer();

    @DgsSubscription
    public Publisher<NotificationPushDto> notificationCreated(@InputArgument Long recipientId){
        Flux<NotificationPushDto> flux = notificationSink.asFlux();
        if (recipientId == null) return flux;
        return flux.filter(dto -> dto != null && dto.getRecipientId() != null && dto.getRecipientId().equals(recipientId));
    }

    public void publishNotification(NotificationPushDto dto){
        notificationSink.tryEmitNext(dto);
    }
}

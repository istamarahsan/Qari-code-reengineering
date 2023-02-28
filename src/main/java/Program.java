import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.MessageCreateSpec;
import io.nayuki.qrcodegen.QrCode;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class Program {

    private static final String PREFIX = "!";

    public static void main(String[] args) throws Exception {
        var token = Optional.ofNullable(System.getenv("TOKEN")).orElseThrow(() -> new Exception("Bot token not found."));
        DiscordClient.create(token).withGateway((GatewayDiscordClient gateway) -> {
            var printOnLogin = gateway.on(ReadyEvent.class, event -> Mono.fromRunnable(() -> {
                var self = event.getSelf();
                System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());
            })).then();
            var qrFromLink = gateway.on(MessageCreateEvent.class, event -> {
                var message = event.getMessage();
                if (!message.getContent().startsWith(PREFIX + "qr")) return Mono.empty();
                var qr = QrCode.encodeText(message.getContent().substring(PREFIX.length()+"qr".length()+1), QrCode.Ecc.MEDIUM);
                var conversionResult = new QrToByteArrayInputStream().convert(qr);
                return conversionResult
                        .map(inputStream -> message
                                .getChannel()
                                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                                        .addFile("QR.png", inputStream)
                                        .build())))
                        .orElse(Mono.empty());
            }).then();
            return printOnLogin.and(qrFromLink);
        }).block();
    }
}

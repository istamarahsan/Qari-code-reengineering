import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.MessageCreateSpec;
import io.nayuki.qrcodegen.QrCode;
import io.vavr.control.Option;
import io.vavr.control.Try;
import reactor.core.publisher.Mono;

import java.util.*;

public class Program {

    private static final String PREFIX = "!";
    private static final int QR_SCALE = 4;
    private static final int QR_BORDER = 1;

    public static void main(String[] args) throws Exception {
        var token = Optional.ofNullable(System.getenv("TOKEN")).orElseThrow(() -> new Exception("Bot token not found."));
        var userFavoritesData = new UserFavoritesData() {

            private final Map<String, List<UserFavorite>> data = new HashMap<>();

            @Override
            public Try<UserFavorite> retrieve(String userDiscordId, String name) {
                return Option.ofOptional(Optional.ofNullable(data.getOrDefault(userDiscordId, null))).toTry(() -> new Exception("User has no favorites")).flatMap(favorites -> Option.ofOptional(favorites.stream().filter(fav -> fav.name().equals(name)).findAny()).toTry(() -> new Exception("User does not have a favorite with that name")));
            }

            @Override
            public Try<Void> store(UserFavorite userFavorite) {
                data.putIfAbsent(userFavorite.userDiscordId(), new ArrayList<>()).add(userFavorite);
                return Try.success(null);
            }
        };

        DiscordClient.create(token).withGateway((GatewayDiscordClient gateway) -> {
            var printOnLogin = gateway.on(ReadyEvent.class, event -> Mono.fromRunnable(() -> {
                var self = event.getSelf();
                System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());
            })).then();
            var handleQr = gateway.on(MessageCreateEvent.class, event -> {
                var message = event.getMessage();
                var messageParts = message.getContent().split(" ");
                var command = messageParts[0];
                if (!command.equals(PREFIX + "qr")) return Mono.empty();
                var contentToConvert = messageParts[1];
                var qr = QrCode.encodeText(contentToConvert, QrCode.Ecc.MEDIUM);
                var conversionResult = new QrToByteArrayInputStream().convert(qr, QR_SCALE, QR_BORDER, "png");
                return conversionResult.map(inputStream -> message.getChannel().flatMap(channel -> channel.createMessage(MessageCreateSpec.builder().addFile("QR.png", inputStream).build())))
                        .orElse(Mono.empty());
            }).then();
            var addFavorite = gateway.on(MessageCreateEvent.class, event -> {
                var message = event.getMessage();
                var messageParts = message.getContent().split(" ");
                var command = messageParts[0];
                if (!command.equals(PREFIX + "qrsave")) return Mono.empty();
                var contentToSave = messageParts[1];
                var nameToSaveAs = messageParts[2];
                message.getAuthor()
                        .map(author -> new UserFavorite(author.getId().asString(), nameToSaveAs, contentToSave))
                        .ifPresent(userFavoritesData::store);
                return Mono.empty();
            }).then();
            var getFavorite = gateway.on(MessageCreateEvent.class, event -> {
                var message = event.getMessage();
                var messageParts = message.getContent().split(" ");
                var command = messageParts[0];
                if (!command.equals(PREFIX + "qrload")) return Mono.empty();
                var nameToRetrieve = messageParts[1];
                return message.getAuthor()
                        .flatMap(author -> userFavoritesData.retrieve(author.getId().asString(), nameToRetrieve).toJavaOptional())
                        .map(data -> QrCode.encodeText(data.content(), QrCode.Ecc.MEDIUM))
                        .flatMap(qr -> new QrToByteArrayInputStream().convert(qr, QR_SCALE, QR_BORDER, "png"))
                        .map(stream -> message.getChannel().flatMap(channel -> channel.createMessage(MessageCreateSpec.builder().addFile(nameToRetrieve, stream).build())))
                        .orElse(Mono.empty());
            }).then();
            return printOnLogin.and(handleQr).and(addFavorite).and(getFavorite);
        }).block();
    }
}

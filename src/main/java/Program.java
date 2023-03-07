import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.MessageCreateFields;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import io.nayuki.qrcodegen.QrCode;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public class Program {
    
    private static final int QR_SCALE = 4;
    private static final int QR_BORDER = 1;

    public static void main(String[] args) throws Exception {
        var token = Optional.ofNullable(System.getenv("TOKEN"))
                .orElseThrow(() -> new Exception("Bot token not found."));

        DiscordClientBuilder.create(token)
                .build()
                .withGateway(gateway -> {
                    var printOnLogin = gateway.on(ReadyEvent.class, event -> Mono.fromRunnable(() -> {
                                var self = event.getSelf();
                                System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());
                            }))
                            .then();

                    var handleSlash = gateway.on(ChatInputInteractionEvent.class, event -> {
                                if (!event.getCommandName()
                                        .equals("qr")) return Mono.empty();
                                return event.getOption("text")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)
                                        .map(ApplicationCommandInteractionOptionValue::asString)
                                        .map(content -> QrCode.encodeText(content, QrCode.Ecc.LOW))
                                        .flatMap(qr -> new QrToByteArrayInputStream().convert(qr, QR_SCALE, QR_BORDER, "png"))
                                        .map(inputStream -> event.reply()
                                                .withFiles(MessageCreateFields.File.of("QR.png", inputStream))
                                                .then())
                                        .orElse(Mono.empty());
                            })
                            .then();
                    
                    registerCommands(gateway.getRestClient());

                    return printOnLogin.and(handleSlash);
                })
                .block();
    }

    private static void registerCommands(RestClient client) {
        final var appId = client.getApplicationId()
                .block();
        if (appId == null) return;
        var qrCommandRequest = ApplicationCommandRequest.builder()
                .name("qr")
                .description("encode some text into a QR image")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("text")
                        .description("text to encode")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();
        client.getApplicationService()
                .bulkOverwriteGlobalApplicationCommand(appId, List.of(qrCommandRequest))
                .subscribe();
    }
}

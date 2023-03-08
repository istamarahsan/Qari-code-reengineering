import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public class Program {
    
    public static void main(String[] args) throws Exception {
        var token = Optional.ofNullable(System.getenv("TOKEN"))
                .orElseThrow(() -> new Exception("Bot token not found."));
        
        var slashCommandHandler = new SlashCommandHandler(new QrToByteArrayInputStream());

        DiscordClientBuilder.create(token)
                .build()
                .withGateway(gateway -> {
                    var printOnLogin = gateway.on(ReadyEvent.class, event -> Mono.fromRunnable(() -> {
                                var self = event.getSelf();
                                System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());
                            }))
                            .then();

                    var handleSlash = gateway.on(ChatInputInteractionEvent.class, slashCommandHandler::handle)
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
        var qrSaveRequest = ApplicationCommandRequest.builder()
                .name("qrsave")
                .description("save a QR with a name")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("text")
                        .description("text to encode")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("a unique name to save your QR")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();
        var qrLoadRequest = ApplicationCommandRequest.builder()
                .name("qrload")
                .description("load a saved QR")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("the name of your saved QR")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();
        client.getApplicationService()
                .bulkOverwriteGlobalApplicationCommand(appId, List.of(qrCommandRequest, qrSaveRequest, qrLoadRequest))
                .subscribe();
    }
}

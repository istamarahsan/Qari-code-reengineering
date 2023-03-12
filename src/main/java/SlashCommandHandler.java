import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.MessageCreateFields;
import io.nayuki.qrcodegen.QrCode;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class SlashCommandHandler {
    private static final int QR_SCALE = 4;
    private static final int QR_BORDER = 1;
    private final QrData qrData = new QrData();

    // "event" contains information about the slash command that was received.
    // we will be using this information to figure out what command was sent (convert text to qr, save a qr, load a qr)
    // and also get data from the command, like the text to convert. We call this data "Options".
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        // we support three commands. They have the following names: ("qr", "qrsave" and "qrload")
        return switch (event.getCommandName()) {
            case "qr" -> handleEncode(event);
            case "qrsave" -> handleSave(event);
            case "qrload" -> handleLoad(event);
            default -> Mono.empty();
        };
    }

    // Command: qr
    // Options: text (text to turn into QR)
    private Mono<Void> handleEncode(ChatInputInteractionEvent event) {
        // retrieve the option called "text". It may not exist, so we get an Optional 
        // Optional is a native Java type representing "something or nothing". 
        // Not to be confused with "Option" which relates to the slash command.
        return event.getOption("text")
                // what we have so far: The Option called "text", or nothing.
                // try to get its value.
                // the option may not have a value, so this function (getValue) returns an Optional too.
                // thus, we use flatMap, or a "Bind" operation in FP terms.
                // basics of Bind/flatMap: 
                // takes a function F that takes in type A and returns type Optional<B>
                // Bind/flatMap converts Optional<A> into Optional<B>
                // if we have nothing(A), we don't apply the function, we end up with nothing(B).
                // if we have something(A), but the function returns nothing, we end up with nothing(B).
                // if we have something(A), and the function returns something, we end up with the new something(B).
                .flatMap(ApplicationCommandInteractionOption::getValue)
                // what we have so far: The value of the Option called "name", or nothing.
                // we take that value as a string
                // it is guaranteed to be interpretable as a string: so "asString" just returns a String
                // thus we use a normal Map operation
                // basics of Map:
                // takes a function F that takes in type A and returns type B
                // Map converts Optional<A> into Optional<B>
                // if we have nothing(A), we don't apply the function, we end up with nothing(B).
                // if we have something(A), apply the function, we end up with something(B)
                .map(ApplicationCommandInteractionOptionValue::asString)
                // what we have so far: The text to encode into QR, or nothing.
                .map(content -> QrCode.encodeText(content, QrCode.Ecc.LOW))
                // what we have so far: The text as a QR object, or nothing.
                // convert into image
                // this operation can fail. 
                // It returns an Optional.
                .flatMap(qr -> new QrToByteArrayInputStream().convert(qr, QR_SCALE, QR_BORDER, "png"))
                // what we have so far: A byte stream which is our QR image in PNG format, or nothing.
                // we now reply to the original slash command, and attach the image to our reply.
                // this operation returns a Mono<Void> (reactive stuff, not rlly important rn)
                .map(inputStream -> event.reply()
                        .withFiles(MessageCreateFields.File.of("QR.png", inputStream))
                        .then())
                // if we have nothing, then just return an empty Mono i.e. "this bot will do nothing"
                .orElse(Mono.empty());
    }

    // Command: qrsave
    // Options: text (text to save as QR), name (name to save the qr as, to refer to later)
    private Mono<Void> handleSave(ChatInputInteractionEvent event) {
        event.getOption("text")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                // what we have so far: The text to save, or nothing.
                // ifPresent will run the provided code if there is something
                .ifPresent(textToSave -> event.getOption("name")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        // what we have so far: 
                        // The text to save, as "textToSave".
                        // The name to save as, or nothing
                        .ifPresent(nameToSaveAs -> qrData.store(nameToSaveAs, textToSave)));
        
        // just reply to the slash command with "OK"
        // yes, it will reply "OK" even if the saving wasn't successful
        return event.reply()
                .withContent("OK")
                .then();
    }

    // Command: qrload
    // Options: name (name of the saved qr that was given when it was saved)
    private Mono<Void> handleLoad(ChatInputInteractionEvent event) {
        return event.getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                // what we have so far: The name of the QR to load, or nothing.
                // we can now retrieve the saved QR using its name
                // we can't guarantee a QR with this name exists, so
                // the retrieval process gives us an Optional
                // thus we use flatMap again
                // BTW, we don't actually save the *QR*, we just save the text.
                // when it's requested, we just convert it back into QR.
                // simpler this way.
                .flatMap(name -> Optional.ofNullable(qrData.savedData.getOrDefault(name, null)))
                // what we have so far: The text to encode into QR, or nothing.
                // encode the saved text into QR
                .map(content -> QrCode.encodeText(content, QrCode.Ecc.LOW))
                .flatMap(qr -> new QrToByteArrayInputStream().convert(qr, QR_SCALE, QR_BORDER, "png"))
                .map(inputStream -> event.reply()
                        .withFiles(MessageCreateFields.File.of("QR.png", inputStream))
                        .then())
                .orElse(Mono.empty());
    }
}

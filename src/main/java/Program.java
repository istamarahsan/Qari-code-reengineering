import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.MessageCreateSpec;
import io.nayuki.qrcodegen.QrCode;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
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
                var img = toImage(qr, 4, 1);

                var os = new ByteArrayOutputStream();
                try {
                    ImageIO.write(img, "png", os);
                } catch (IOException e) {
                    return Mono.error(e);
                }
                var is = new ByteArrayInputStream(os.toByteArray());

                return message
                        .getChannel()
                        .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                                .addFile("QR.png", is)
                                .build()));
            }).then();
            return printOnLogin.and(qrFromLink);
        }).block();
    }

    private static BufferedImage toImage(QrCode qr, int scale, int border) {
        return toImage(qr, scale, border, 0xFFFFFF, 0x000000);
    }


    /**
     * Returns a raster image depicting the specified QR Code, with
     * the specified module scale, border modules, and module colors.
     * <p>For example, scale=10 and border=4 means to pad the QR Code with 4 light border
     * modules on all four sides, and use 10&#xD7;10 pixels to represent each module.
     *
     * @param qr         the QR Code to render (not {@code null})
     * @param scale      the side length (measured in pixels, must be positive) of each module
     * @param border     the number of border modules to add, which must be non-negative
     * @param lightColor the color to use for light modules, in 0xRRGGBB format
     * @param darkColor  the color to use for dark modules, in 0xRRGGBB format
     * @return a new image representing the QR Code, with padding and scaling
     * @throws NullPointerException     if the QR Code is {@code null}
     * @throws IllegalArgumentException if the scale or border is out of range, or if
     *                                  {scale, border, size} cause the image dimensions to exceed Integer.MAX_VALUE
     */
    private static BufferedImage toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
        Objects.requireNonNull(qr);
        if (scale <= 0 || border < 0) throw new IllegalArgumentException("Value out of range");
        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
            throw new IllegalArgumentException("Scale or border too large");

        BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? darkColor : lightColor);
            }
        }
        return result;
    }
}

import io.nayuki.qrcodegen.QrCode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

public class QrToByteArrayInputStream {
    public QrToByteArrayInputStream() {
        
    }
    
    public Optional<ByteArrayInputStream> convert(QrCode qr, int scale, int border, String format) {
        // Handle invalid inputs
        if (qr == null) 
            throw new NullPointerException();
        if (scale <= 0 || border < 0) 
            throw new IllegalArgumentException("Value out of range");
        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
            throw new IllegalArgumentException("Scale or border too large");
        // We only support PNG here
        if (!format.equals("png"))
            throw new IllegalArgumentException();
        BufferedImage img = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                img.setRGB(x, y, color ? 0x000000 : 0xFFFFFF);
            }
        }
        // how we do this is take the image, write it into a byte array output stream, and then create a byte array input stream from it
        // idk how else to do it 💀
        
        final var os = new ByteArrayOutputStream();
        // writing the image can fail.
        try {
            ImageIO.write(img, format, os);
        } catch (IOException e) {
            return Optional.empty();
        }
        var result = new ByteArrayInputStream(os.toByteArray());
        return Optional.of(result);
    }
}

import io.nayuki.qrcodegen.QrCode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QrToByteArrayInputStream implements QrImageEncoder {
    
    public QrToByteArrayInputStream() {
        
    }
    
    @Override
    public ByteArrayInputStream convert(QrCode qr, int scale, int border, QrImgFormat ImgFormat) throws NullPointerException, IllegalArgumentException, IOException {
        if (qr == null) 
            throw new NullPointerException();
        if (scale <= 0 || border < 0) 
            throw new IllegalArgumentException("Value out of range");
        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
            throw new IllegalArgumentException("Scale or border too large");
        BufferedImage img = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                img.setRGB(x, y, color ? 0x000000 : 0xFFFFFF);
            }
        }
        final var os = new ByteArrayOutputStream();
        ImageIO.write(img, ImgFormat.toString(), os);
        return new ByteArrayInputStream(os.toByteArray());
    }
}

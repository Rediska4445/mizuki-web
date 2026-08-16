package rf.mizuka.web.application.services.color;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class ColorService
{
    public Color findMostContrastingColor(BufferedImage image0)
    {
        try {
            int RED = 1;
            int GREEN = 1;
            int BLUE = 1;

            int square = image0.getHeight() * image0.getWidth();

            for (int x = 0; x < image0.getHeight(); x++) {
                for (int y = 0; y < image0.getWidth(); y++) {
                    RED += image0.getRGB(x, y) >> 16 & 0xff;
                    GREEN += image0.getRGB(x, y) >> 8 & 0xff;
                    BLUE += image0.getRGB(x, y) & 0xff;
                }
            }

            if ((RED / square) >= 175 && (GREEN / square) >= 175 && (BLUE / square) >= 175) {
                return Color.BLACK;
            }

            return new Color(RED / square, GREEN / square, BLUE / square);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }
}

package rf.mizuka.web.application.services.color;

import org.springframework.stereotype.Service;
import rf.mizuka.web.application.services.color.policy.ColorPolicy;
import rf.mizuka.web.application.services.color.policy.impl.AverageColorPolicyImpl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@Service
public class ColorService
{
    private final HashMap<String, ColorPolicy> colorPolicyHashMap
            = new HashMap<>(Map.of("average", new AverageColorPolicyImpl()));

    public String convertColorToHex(java.awt.Color color)
    {
        int rgbWithoutAlpha = color.getRGB() & 0x00FFFFFF;
        String hexString = Integer.toHexString(rgbWithoutAlpha).toUpperCase();
        String paddedHex = String.format("%6s", hexString).replace(' ', '0');

        return "#" + paddedHex;
    }

    public Color calculateMainColorFromImage(BufferedImage image0)
    {
        return colorPolicyHashMap.get("average").calculateMainColorFromImage(image0);
    }
}
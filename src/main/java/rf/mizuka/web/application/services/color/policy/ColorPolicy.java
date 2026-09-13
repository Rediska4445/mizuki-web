package rf.mizuka.web.application.services.color.policy;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface ColorPolicy
{
    Color calculateMainColorFromImage(BufferedImage image0);
}

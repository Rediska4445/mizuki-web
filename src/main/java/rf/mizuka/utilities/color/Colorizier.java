package rf.mizuka.utilities.color;

public class Colorizier
{
    public static String convertColorToHex(java.awt.Color color)
    {
        int rgbWithoutAlpha = color.getRGB() & 0x00FFFFFF;
        String hexString = Integer.toHexString(rgbWithoutAlpha).toUpperCase();
        String paddedHex = String.format("%6s", hexString).replace(' ', '0');

        return "#" + paddedHex;
    }
}

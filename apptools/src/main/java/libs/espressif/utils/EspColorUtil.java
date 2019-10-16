package libs.espressif.utils;

public class EspColorUtil {
    /**
     * @param r [0..255]
     * @param g [0..255]
     * @param b [0..255]
     * @return HSL values, values[0] is Hue [0 ... 1), values[1] is Saturation [0...1], value[2] is Lightness [0...1]
     */
    public static double[] RGBToHSL(int r, int g, int b) {
        double H, S, L;
        double R, G, B, Max, Min, del_R, del_G, del_B, del_Max;
        R = r / 255.0;       //Where RGB values = 0 ÷ 255
        G = g / 255.0;
        B = b / 255.0;

        Min = Math.min(R, Math.min(G, B));    //Min. value of RGB
        Max = Math.max(R, Math.max(G, B));    //Max. value of RGB
        del_Max = Max - Min;        //Delta RGB value

        H = -1;
        L = -1;
        L = (Max + Min) / 2.0;

        if (del_Max == 0)           //This is a gray, no chroma...
        {
            //H = 2.0/3.0;          //Windows下S值为0时，H值始终为160（2/3*240）
            H = 0;                  //HSL results = 0 ÷ 1
            S = 0;
        } else                        //Chromatic data...
        {
            if (L < 0.5) S = del_Max / (Max + Min);
            else S = del_Max / (2 - Max - Min);

            del_R = (((Max - R) / 6.0) + (del_Max / 2.0)) / del_Max;
            del_G = (((Max - G) / 6.0) + (del_Max / 2.0)) / del_Max;
            del_B = (((Max - B) / 6.0) + (del_Max / 2.0)) / del_Max;

            if (R == Max) H = del_B - del_G;
            else if (G == Max) H = (1.0 / 3.0) + del_R - del_B;
            else if (B == Max) H = (2.0 / 3.0) + del_G - del_R;

            if (H < 0) H += 1;
            if (H > 1) H -= 1;
        }

        return new double[]{H, S, L};
    }

    /**
     * @param H [0 ... 1)
     * @param S [0...1]
     * @param L [0...1]
     * @return RGB values, values[0] is Red [0 .. 255], value[1] is Green [0 .. 255], value[2] is Blue [0 .. 255]
     */
    public static int[] HSLToRGB(double H, double S, double L) {
        double R, G, B;
        double var_1, var_2;

        if (S == 0)                       //HSL values = 0 ÷ 1
        {
            R = L * 255.0;                   //RGB results = 0 ÷ 255
            G = L * 255.0;
            B = L * 255.0;
        } else {
            if (L < 0.5) var_2 = L * (1 + S);
            else var_2 = (L + S) - (S * L);

            var_1 = 2.0 * L - var_2;

            R = 255.0 * Hue2RGB(var_1, var_2, H + (1.0 / 3.0));
            G = 255.0 * Hue2RGB(var_1, var_2, H);
            B = 255.0 * Hue2RGB(var_1, var_2, H - (1.0 / 3.0));
        }

        return new int[]{(int) Math.round(R), (int) Math.round(G), (int) Math.round(B)};
    }

    private static double Hue2RGB(double v1, double v2, double vH) {
        if (vH < 0) vH += 1;
        if (vH > 1) vH -= 1;
        if (6.0 * vH < 1) return v1 + (v2 - v1) * 6.0 * vH;
        if (2.0 * vH < 1) return v2;
        if (3.0 * vH < 2) return v1 + (v2 - v1) * ((2.0 / 3.0) - vH) * 6.0;
        return (v1);
    }
}

import ij.*;
import ij.ImagePlus;
import ij.gui.*;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.util.ArrayUtil;
import java.awt.*;
import java.lang.Math;

public class Template_Matcher implements PlugInFilter {

    public int setup(String arg, ImagePlus imp) {
        return DOES_ALL+ROI_REQUIRED+SUPPORTS_MASKING;
    }

    public void run(ImageProcessor ip) {

        int x;
        int y;

        int p;

        double block_sim = 0.0;

        /* Dimensionen des Originalbildes */
        int w = ip.getWidth();
        int h = ip.getHeight();

        /* hole ROI für späteres Template g aus Originalbild (f = h) */
        Rectangle roi = ip.getRoi();
        int[] roi_mean = {0, 0, 0};
        int[] m = new int[3];
        int dim = roi.width * roi.height;

        /* lege Arbeitskopie für Vorverarbeitung an */
        ImagePlus copy = new ImagePlus("Arbeitskopie", ip.duplicate());
        ImageProcessor copy_ip = copy.getProcessor();

        ImagePlus temp = NewImage.createRGBImage("Template", roi.width, roi.height, 1, 0);
        ImageProcessor temp_ip = temp.getProcessor();

        int[] rowbuffer = new int[roi.width];

        /* kopiere ROI-Pixel zeilenweise in Template */
        for (y = roi.y; y < (roi.y + roi.height); y++) {
            copy_ip.getRow(roi.x, y, rowbuffer, roi.width);
            temp_ip.putRow(0, y - roi.y, rowbuffer, roi.width);
        }

        /* ROI-Mittelwertsvektor berechnen */
        for (y = 0; y < roi.height; y++) {
            for (x = 0; x < roi.width; x++) {
                getRGBArray(temp_ip.getPixel(x, y), roi_mean);
            }
        }

        roi_mean[0] /= dim;
        roi_mean[1] /= dim;
        roi_mean[2] /= dim;

        for (y = 0; y < h; y++) {
            for (x = 0; x < w; x++) {
                getRGBArray(copy_ip.getPixel(x, y), m);
                block_sim = manhattanD(m, roi_mean);
                System.out.println(block_sim);
            }
        }

        // copy.show();
        // copy.updateAndDraw();
        
        // temp.show();
        // temp.updateAndDraw();
    }

    private float manhattanD(int[] u, int[] v) {
        float d = 0;

        for (int i = 0; i < u.length; i++) {
            d += Math.abs(u[i] - v[i]); 
        }

        d /= 255 * u.length;

        return d;
    }

    private void getRGBArray(int p, int[] a) {
        a[0] += (p >> 16) & 0xff;
        a[1] += (p >>  8) & 0xff;
        a[2] += (p)       & 0xff;
    }

}


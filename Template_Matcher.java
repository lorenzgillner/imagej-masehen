import ij.*;
import ij.ImagePlus;
import ij.gui.*;
import ij.gui.NewImage;
import ij.gui.ProgressBar;
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
        
        /* Schleifenzähler */
        int x, y, i, j, c;

        /* ein paar Zwischenspeicher */
        int sum_temp, sum_copy, sum_both;
        int p_temp, p_copy;

        /* Dimensionen des Originalbildes */
        int w = ip.getWidth();
        int h = ip.getHeight();
        int a = w * h;

        /* hole ROI für späteres Template g aus Originalbild (f = h) */
        Rectangle roi = ip.getRoi();

        int prog_max = a - (roi.width - roi.height);

        /* Arbeitskopie als Graustufenbild */
        ByteProcessor ip_copy = ip.convertToByteProcessor(true);
        ImagePlus copy = new ImagePlus("Arbeitskopie", ip_copy);
        
        /* übertrage ursprüngliche ROI auf Arbeitskopie */
        ip_copy.setRoi(roi); 
        
        /* Binarisierung der Arbeitskopie */
        // ip_copy.autoThreshold();

        /* extrahiere ROI aus Arbeitskopie als eigenes Bild */
        ByteProcessor ip_temp = ip_copy.crop().convertToByteProcessor(true);
        ImagePlus temp = new ImagePlus("Template", ip_temp);

        /* Korrelationsmatrix erzeugen */
        ImagePlus corr = NewImage.createByteImage("Korrelationsmatrix", w, h, 1, 1);
        ByteProcessor ip_corr = corr.getProcessor().convertToByteProcessor(true);

        sum_temp = 0;

        for (y = 0; y < roi.height; y++) {
            for (x = 0; x < roi.width; x++) {
                sum_temp += Math.pow(ip_temp.get(x, y), 2);
            }
        }

        System.out.println("--- "+(int)Math.floor((double)sum_temp/(double)(255*255*roi.width*roi.height)));

        for (y = 0; y < (h-roi.height); y++) {
            for (x = 0; x < (w-roi.width); x++) {
                sum_copy = 0;
                sum_both = 0;
                for (j = 0; j < roi.height; j++) {
                    for (i = 0; i < roi.width; i++) {
                        p_temp = ip_temp.get(i, j);
                        p_copy = ip_copy.get(x + i, y + j);
                        sum_copy += p_copy * p_copy;
                        sum_both += p_temp * p_copy;
                    }
                }
                // System.out.println(""+sum_both+" "+sum_temp+" "+sum_copy);
                c = (int)Math.floor(255 * ((double)sum_both / Math.sqrt(sum_temp * sum_copy)));
                ip_corr.putPixel(x, y, c);
            }
            IJ.showProgress(y * x, a);
        }

        /* Maxima finden */

        /* Rechteck in ROI-Dimension zeichnen */

        corr.show();
        corr.updateAndDraw();

        // template.show();
        // template.updateAndDraw();

        // copy.show();
        // copy.updateAndDraw();




































        // /* lege Arbeitskopie für Vorverarbeitung an */
        // ImagePlus copy = new ImagePlus("Arbeitskopie", ip.duplicate());
        // ImageProcessor copy_ip = copy.getProcessor();

        // ImagePlus temp = NewImage.createRGBImage("Template", roi.width, roi.height, 1, 0);
        // ImageProcessor temp_ip = temp.getProcessor();

        // int[] rowbuffer = new int[roi.width];

        // /* kopiere ROI-Pixel zeilenweise in Template */
        // for (y = roi.y; y < (roi.y + roi.height); y++) {
        //     copy_ip.getRow(roi.x, y, rowbuffer, roi.width);
        //     temp_ip.putRow(0, y - roi.y, rowbuffer, roi.width);
        // }

        // /* ROI-Mittelwertsvektor berechnen */
        // for (y = 0; y < roi.height; y++) {
        //     for (x = 0; x < roi.width; x++) {
        //         getRGBArray(temp_ip.getPixel(x, y), roi_mean);
        //     }
        // }

        // roi_mean[0] /= dim;
        // roi_mean[1] /= dim;
        // roi_mean[2] /= dim;

        // for (y = 0; y < h; y++) {
        //     for (x = 0; x < w; x++) {
        //         getRGBArray(copy_ip.getPixel(x, y), m);
        //         block_sim = manhattanD(m, roi_mean);
        //         System.out.println(block_sim);
        //     }
        // }        
        // temp.show();
        // temp.updateAndDraw();
    }

//     private float manhattanD(int[] u, int[] v) {
//         float d = 0;

//         for (int i = 0; i < u.length; i++) {
//             d += Math.abs(u[i] - v[i]); 
//         }

//         d /= 255 * u.length;

//         return d;
//     }

//     private void getRGBArray(int p, int[] a) {
//         a[0] += (p >> 16) & 0xff;
//         a[1] += (p >>  8) & 0xff;
//         a[2] += (p)       & 0xff;
//     }

}


import ij.*;
import ij.ImagePlus;
import ij.gui.*;
import ij.gui.NewImage;
import ij.gui.ProgressBar;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.*;
import ij.util.ArrayUtil;
import java.awt.*;
import java.lang.Math;

public class Template_Matcher implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL+ROI_REQUIRED+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		double TOLERANCE = 10.0;
		
		/* Schleifenzähler */
		int x, y, i, j;

		/* ein paar Zwischenspeicher */
		float sum_temp, sum_copy, sum_both;
		float p_temp, p_copy;
		float c;

		/* Dimensionen des Originalbildes */
		int w = ip.getWidth();
		int h = ip.getHeight();
		int a = w * h;

		/* hole ROI für Template g aus Originalbild (f = h) */
		Rectangle roi = ip.getRoi();

		/* Arbeitskopie als Graustufenbild */
		FloatProcessor ip_copy = ip.convertToFloatProcessor();
		ImagePlus copy = new ImagePlus("Arbeitskopie", ip_copy);
		
		/* übertrage ursprüngliche ROI auf Arbeitskopie */
		ip_copy.setRoi(roi); 
		
		/* Binarisierung der Arbeitskopie */
		// ip_copy.autoThreshold();

		/* extrahiere ROI aus Arbeitskopie als eigenes Bild */
		FloatProcessor ip_temp = ip_copy.crop().convertToFloatProcessor();

		/* Korrelationsmatrix */
		float[][] corr = new float[w - roi.width][h - roi.height];

		/* sum_temp ist immer gleich, berechne es also außerhalb der Schleife */
		sum_temp = 0;

		for (y = 0; y < roi.height; y++) {
			for (x = 0; x < roi.width; x++) {
				p_temp = ip_temp.getf(x, y);
				sum_temp += p_temp * p_temp;
			}
		}

		/* fülle die Korrelationsmatrix */
		for (y = 0; y < (h-roi.height); y++) {
			for (x = 0; x < (w-roi.width); x++) {
				sum_copy = 0;
				sum_both = 0;
				for (j = 0; j < roi.height; j++) {
					for (i = 0; i < roi.width; i++) {
						p_temp = ip_temp.getf(i, j);
						p_copy = ip_copy.getf(x + i, y + j);
						sum_copy += p_copy * p_copy;
						sum_both += p_temp * p_copy;
					}
				}
				corr[x][y] = (sum_both / (float)Math.sqrt(sum_temp * sum_copy)) * 255;
			}
			/* Fortschrittsanzeige; nur für ImageJ wichtig */
			IJ.showProgress(y * x, a);
		}

		/* Maxima finden */
		Polygon maxima = new MaximumFinder().getMaxima(new FloatProcessor(corr), TOLERANCE, false);
		
		
		/* Ausgabebild erzeugen */
		ImageProcessor ip_output = ip.duplicate();
		ImagePlus output = new ImagePlus("Template Matcher: "+maxima.npoints+" Treffer", ip_output);
		
		ip_output.setColor(0xff00ff);
		ip_output.setLineWidth(3);
		
		/* Übereistimmungen mit Template hervorheben */
		for (i = 0; i < maxima.npoints; i++) {
			ip_output.drawRect(maxima.xpoints[i], maxima.ypoints[i], roi.width, roi.height);
		}
		
		output.show();
		output.updateAndDraw();
	}

}


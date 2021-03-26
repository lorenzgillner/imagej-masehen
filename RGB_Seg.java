import ij.*;
import ij.ImagePlus;
import ij.gui.*;
import ij.gui.NewImage;
import ij.gui.GenericDialog;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.ColorThresholder;
import ij.process.*;
import ij.util.ArrayUtil;
import java.awt.*;
import java.lang.Math;
import java.util.Arrays;

public class RGB_Seg implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		return DOES_RGB+ROI_REQUIRED+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		/* Optionsdialog */
		GenericDialog gd = new GenericDialog("Optionen");
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		gd.setSize(300, 110);
		String[] opts = {"Cut", "Highlight"};
		gd.addChoice("Modus", opts, "Mask");
		gd.addNumericField("Intervallgröße", 2.0, 2);
		gd.addCheckbox("Zeige Log", false);
		gd.addCheckbox("Visualisiere Merkmalsraum", false);
		gd.setLocation((int)screensize.getWidth()/2, (int)screensize.getHeight()/2);
		gd.showDialog();
		
		/* brich Plugin-Ausführung ab, falls Dialog unterbrochen */
		if (gd.wasCanceled()) return;
		
		/* Art der Darstellung gefundener Flächen */
		int mode = gd.getNextChoiceIndex();
		
		/* Skalierungsfaktor */
		double s = gd.getNextNumber();
		
		/* sonstige Optionen */
		boolean show_log = gd.getNextBoolean();
		boolean show_vis = gd.getNextBoolean();
		
		/* Hilfsvariablen */
		int i, n, p, x, y;
		
		double t;
		int[] p_rgb = new int[3];

		/* Dimensionen des Originalbildes */
		int w = ip.getWidth();
		int h = ip.getHeight();
		int c = ip.getNChannels();	/* sollte 3 sein */
		int[] min = new int[c];
		int[] max = new int[c];
		
		/* ROI-Mittelwertvektor initialisieren */
		int[] m_roi = new int[c];
		Arrays.fill(m_roi, 0);
		
		/* Standardabweichung(en) */
		double[] σ = new double[c];
		
		/* ROI (Rechteck) */
		Rectangle roi = ip.getRoi();
		/* ROI-Maske (für sonstige Formen) */
		ImageProcessor ip_roi = ip.getMask();
		
		/* in der Hoffnung, ein paar Unebenheiten zu beseitigen ... */
		ImageProcessor ip_cpy = ip.duplicate();
		ip_cpy.filter(ImageProcessor.BLUR_MORE);
		
		/* ROI ist jetzt eine Bitmaske; Anzahl farbiger Pixel
		 * also erst im nächsten Schritt zählen */
		n = 0;
		
		/* m_roi summieren */
		for (y = 0; y < roi.height; y++) {
			for (x = 0; x < roi.width; x++) {
				/* betrachte Pixel nur, wenn maskiert */
				if ((ip_roi.get(x, y) & 0xff) > 0x00) {
					getRGB(ip_cpy.get(x + roi.x, y + roi.y), p_rgb);
					m_roi[0] += p_rgb[0];
					m_roi[1] += p_rgb[1];
					m_roi[2] += p_rgb[2];
					n++;
				}
			}
		}
		
		/* Merkmalsmittelwert und Standardabweichung berechnen */
		for (i = 0; i < c; i++) {
			m_roi[i] /= n;
			t = .0;
			for (y = 0; y < roi.height; y++) {
				for (x = 0; x < roi.width; x++) {
					if ((ip_roi.get(x, y) & 0xff) > 0x00) {
						p = (ip_cpy.get(x + roi.x, y + roi.y) >> (16 - (i * 8))) & 0xff;
						t += Math.pow(((double)p - (double)m_roi[i]), 2);
					}
				}
			}
			σ[i] = Math.sqrt(t/(n-1));
		}
		
		/* Minimum und Maximum setzen */
		min[0] = (int)Math.round(m_roi[0] - (s * σ[0]));
		min[1] = (int)Math.round(m_roi[1] - (s * σ[1]));
		min[2] = (int)Math.round(m_roi[2] - (s * σ[2]));
	
		max[0] = (int)Math.round(m_roi[0] + (s * σ[0]));
		max[1] = (int)Math.round(m_roi[1] + (s * σ[1]));
		max[2] = (int)Math.round(m_roi[2] + (s * σ[2]));
		
		/* Kopie für Ausgabe anlegen */
		ImageProcessor ip_out = ip.duplicate();
		
		/* Je nach Hervorhebungsmethode:
		 * > wenn Pixelwerte im Intervall, markieren, oder
		 * > wenn nicht im Intervall, "löschen" */
		for (i = 0; i < ip_out.getPixelCount(); i++) {
			getRGB(ip_out.get(i), p_rgb);
			if ((min[0] < p_rgb[0] && p_rgb[0] < max[0]) 
				&& (min[1] < p_rgb[1] && p_rgb[1] < max[1])
				&& (min[2] < p_rgb[2] && p_rgb[2] < max[2])
			) {
				if (mode == 1) ip_out.set(i, 0xff00ff);
			}
			else {
				if (mode == 0) ip_out.set(i, 0x000000);
			}
		}

		/* Ausgabe anzeigen */
		new ImagePlus("RGB-Segmentierung", ip_out).show();
		
		if (show_log) {
			IJ.log("min: "+Arrays.toString(min));
			IJ.log("max: "+Arrays.toString(max));
		}
		
		// if (show_vis) {
			// int pc = ip.getPixelCount();
			// int[] Y = new int[pc];
			// int[] U = new int[pc];
			// int[] V = new int[pc];
			// int[][] yuv_space = new int[255][255];
			// int[] rrr = new int[255*255];
			// rgb2yuv(ip, Y, U, V);
			// for (i = 0; i < pc; i++) {
				// yuv_space[127+V[i]/2][127+U[i]/2] = (int)ip.get(i);
			// }
			// ImageProcessor ip_yuv = ip.createProcessor(255, 255);
			// ip_yuv.setPixels(yuv_space);
			// new ImagePlus("Merkmalsraum", ip_yuv).show();
		// }
	}
	
	private void getRGB(int pixel, int[] pixel_rgb) {
		pixel_rgb[0] = (pixel >> 16) & 0xff;
		pixel_rgb[1] = (pixel >> 8) & 0xff;
		pixel_rgb[2] = pixel & 0xff;
	}
	
	private void rgb2yuv(ImageProcessor ip, int[] Y, int[] U, int[] V) {
		int y, p;
		double r, g, b;
		for (int i = 0; i < ip.getPixelCount(); i++) {
			p = (int)ip.get(i);
			r = (double)((p >> 16) & 0xff);
			g = (double)((p >> 8) & 0xff);
			b = (double)(p & 0xff);
			y = (int)Math.round(0.299 * r + 0.587 * g + 0.114 * b);
			U[i] = (int)Math.round(0.493 * (b - y));
			V[i] = (int)Math.round(0.877 * (r - y));
			Y[i] = y;
		}
	}

}


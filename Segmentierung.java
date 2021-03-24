import ij.*;
import ij.ImagePlus;
import ij.gui.*;
import ij.gui.NewImage;
import ij.gui.GenericDialog;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.util.ArrayUtil;
import java.awt.*;
import java.lang.Math;
import java.util.Arrays;

public class Segmentierung implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		return DOES_RGB+ROI_REQUIRED+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Optionen");
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		gd.setSize(300, 110);
		String[] opts = {"Cut", "Highlight"};
		gd.addChoice("Modus", opts, "Mask");
		gd.addNumericField("Intervallgröße", 2.0, 0);
		gd.setLocation((int)screensize.getWidth()/2, (int)screensize.getHeight()/2);
		gd.showDialog();
		
		/* Art der Darstellung gefundener Flächen */
		int mode = gd.getNextChoiceIndex();
		/* Skalierungsfaktor */
		double s = gd.getNextNumber();
		
		/* temporäre Variablen */
		int i, j, p;
		
		int black = 0x000000;
		int magenta = 0xff00ff;
		
		double t, pf, r, g, b;

		/* Dimensionen des Originalbildes */
		int w = ip.getWidth();
		int h = ip.getHeight();
		int c = ip.getNChannels();	/* sollte 3 sein */
		double[] min = new double[c];
		double[] max = new double[c];
		
		/* ROI-Mittelwertvektor initialisieren */
		int[] m_roi = new int[c];
		Arrays.fill(m_roi, 0);
		
		/* Standardabweichung(en) */
		double[] σ = new double[c];
		
		/* ROI (Rechteck) */
		Rectangle roi = ip.getRoi();
		ImageProcessor ip_roi = ip.crop();
		int n = ip_roi.getPixelCount();
		
		/* m_roi berechnen */
		for (i = 0; i < n; i++) {
			p = (int)ip_roi.get(i);
			m_roi[0] += (p >> 16) & 0xff;
			m_roi[1] += (p >> 8) & 0xff;
			m_roi[2] += p & 0xff;
		}
		
		/* Standardabweichung gleich mit berechnen */
		for (i = 0; i < c; i++) {
			m_roi[i] /= n;
			t = .0;
			for (j = 0; j < n; j++) {
				p = (int)ip_roi.get(j);
				pf = (double)((p >> (16 - (i * 8))) & 0xff);
				t += Math.pow((pf - (double)m_roi[i]), 2);
			}
			σ[i] = Math.sqrt(t/(n-1));
		}
		
		/* Minimum und Maximum setzen */
		min[0] = (double)m_roi[0] - s * σ[0];
		min[1] = (double)m_roi[1] - s * σ[1];
		min[2] = (double)m_roi[2] - s * σ[2];
		
		max[0] = (double)m_roi[0] + s * σ[0];
		max[1] = (double)m_roi[1] + s * σ[1];
		max[2] = (double)m_roi[2] + s * σ[2];
		
		/* Kopie für Ausgabe anlegen */
		ImageProcessor ip_out = ip.duplicate();
		
		/* Je nach Hervorhebungsmethode:
		 * > wenn Pixelwerte im Intervall, markieren, oder
		 * > wenn nicht im Intervall, "löschen" */
		for (i = 0; i < ip_out.getPixelCount(); i++) {
			p = (int)ip_out.get(i);
			r = (double)((p >> 16) & 0xff);
			g = (double)((p >> 8) & 0xff);
			b = (double)(p & 0xff);
			if ((min[0] < r && r < max[0]) 
				&& (min[1] < g && g < max[1])
				&& (min[2] < b && b < max[2])
			) {
				if (mode == 1) ip_out.set(i, magenta);
			}
			else {
				if (mode == 0) ip_out.set(i, 0x000000);
			}
		}
		
		/* Ausgabe anzeigen */
		new ImagePlus("Segmentierung", ip_out).show();
	}

}


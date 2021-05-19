import ij.*;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.MaximumFinder;
import ij.process.*;
import ij.util.ArrayUtil;
import java.awt.*;
import java.lang.Math;
import java.util.List;
import java.util.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Harris_Eckendetektor_OpenCV implements PlugInFilter {

	/* Manhattan-Distanz zur Bestimmung möglicher Nachbarn */
	public double manhattanDistance(Point a, Point b) {
		double sum = Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
		return sum;
	}

	public double getNeighborhoodMax(ImageProcessor ip, int n, int x, int y) {
		double max = 0.0;
		double tmax;
		float[] row = new float[n];
		int xs = x-(int)(n/2);
		int ys = y-(int)(n/2);
		if (xs >= 0 && ys >= 0 && xs+n <= ip.getWidth() && ys+n <= ip.getHeight()) {
			for (int i = 0; i < n; i++) {
				ip.getRow(xs, ys+i, row, n);
				tmax = new ArrayUtil(row).getMaximum();
				if (tmax > max) {
					max = tmax;
				}
			}
		}
		return max;
	}

	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Optionen");
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		gd.setSize(200, 100);
		gd.addNumericField("Empfindlichkeit: ", 0.05, 2);
		gd.addNumericField("Sigma: ", 2, 0);
		gd.addCheckbox("Zeige Gradientenbilder", false);
		gd.addCheckbox("Zeige Log", false);
                gd.addCheckbox("Ergebnisse in neuem Bild", false);
		gd.setLocation((int)screensize.getWidth()/2, (int)screensize.getHeight()/2);
		gd.showDialog();

		if (gd.wasCanceled()) return;

		/* Empfindlichkeit der Eckendetektion */
		float i = (float)gd.getNextNumber();

		/* "Gauss-Sigma" */
		double sigma = gd.getNextNumber();

		/* Flags für weitere Ausgaben */
		boolean show_grad = gd.getNextBoolean();
		boolean show_log = gd.getNextBoolean();
                boolean show_in_new_image = gd.getNextBoolean();

		/* Schleifenzähler */
		int x, y, z;

		/* Größe des Kanten-Filterkerns */
		int RAD = 1;

		/* Anzahl Nachkommastellen */
		float DEC = (float)1e5;

		float A, B, C, AB, c;

		/* Bilddimensionen */
		int w = ip.getWidth();
		int h = ip.getHeight();

		double s_x, s_y, current_pixel_value, max, mean;

		/* hier landen dann die Eck-Kandidaten */
		ArrayList<Point> corners = new ArrayList<Point>();
		ArrayList<Point> good_corners = new ArrayList<Point>();
		Point tmp;

		/* lege Graustufenkopie an */
		FloatProcessor ip_gs = ip.convertToFloatProcessor();

		/* normalisiere Pixel auf [0, 1] */
		ip_gs.multiply(1.0/255.0);

		/* etwas weichzeichnen */
		ip_gs.dilate();

		/* Gradientenbilder */
		FloatProcessor ip_gradA = new FloatProcessor(w, h);
		FloatProcessor ip_gradB = new FloatProcessor(w, h);
		FloatProcessor ip_gradC = new FloatProcessor(w, h);
		FloatProcessor ip_corners = new FloatProcessor(w, h);

		/* Algorithmus 16.1 [Nischwitz] */
		for (y = RAD+1; y < h-RAD-1; y++) {
			for (x = RAD+1; x < w-RAD-1; x++) {
				/* speichere aktuellen Pixelwert, damit wir den nicht immer wieder neu holen müssen */
				current_pixel_value = ip_gs.getPixelValue(x, y);

				/* bestimme Intensität der Abweichung an (x,y) */
				s_x = Math.abs(ip_gs.getPixelValue(x-RAD, y-RAD) - ip_gs.getPixelValue(x+RAD, y-RAD))
					+ Math.abs(ip_gs.getPixelValue(x-RAD, y) - ip_gs.getPixelValue(x+RAD, y))
					+ Math.abs(ip_gs.getPixelValue(x-RAD, y+RAD) - ip_gs.getPixelValue(x+RAD, y+RAD));
				s_x /= 3;

				s_y = Math.abs(ip_gs.getPixelValue(x-RAD, y-RAD) - ip_gs.getPixelValue(x-RAD, y+RAD))
					+ Math.abs(ip_gs.getPixelValue(x, y-RAD) - ip_gs.getPixelValue(x, y+RAD))
					+ Math.abs(ip_gs.getPixelValue(x+RAD, y-RAD) - ip_gs.getPixelValue(x+RAD, y+RAD));
				s_y /= 3;

				/* setze Pixel in Zwischenspeicher */
				ip_gradA.putPixelValue(x, y, s_x * s_x);	// A
				ip_gradB.putPixelValue(x, y, s_y * s_y);	// B
				ip_gradC.putPixelValue(x, y, s_x * s_y);	// C
			}
		}

		/* Gauss-Weichzeichner auf Gradientenbilder anwenden */
		ip_gradA.blurGaussian(sigma);
		ip_gradB.blurGaussian(sigma);
		ip_gradC.blurGaussian(sigma);

		/* Berechnung der Gütefunktion über (fast) jeden Bildpunkt */
		for (z = 0; z < ip_corners.getPixelCount(); z++) {
			A = ip_gradA.getf(z);
			B = ip_gradB.getf(z);
			C = ip_gradC.getf(z);
			AB = A + B;

			c = ((A * B) - (C * C)) - i * (AB * AB);
			c = (float) Math.round(c * DEC) / DEC;

			ip_corners.setf(z, c);
		}

		/* finde die Maxima */
		Polygon maxima = new MaximumFinder().getMaxima(ip_corners, 1/DEC, false);

		/* forme Maxima zu einer ArrayList um (zur leichteren Verarbeitung) */
		for (z = 0; z < maxima.npoints; z++) {
			x = maxima.xpoints[z];
			y = maxima.ypoints[z];
			/* füge Ecke nur hinzu, wenn sie in 6er-Nachbarschaft das Maximum ist */
			// if ((double)ip_corners.getPixelValue(x, y) >= getNeighborhoodMax(ip_corners, 6, x, y)) {
				// corners.add(new Point(x, y));
			// }
			corners.add(new Point(x, y));
		}

		/* entferne benachbarte Punkte, falls vorhanden */
		while (corners.size() > 0) {
			tmp = corners.get(0);
			good_corners.add(tmp);
			corners.remove(0);
			for (z = 0; z < corners.size(); z++) {
				if (manhattanDistance(tmp, corners.get(z)) < 6) {
					corners.remove(z);
				}
			}
		}

    ImageProcessor ip_out = (show_in_new_image) ? ip.duplicate() : ip;

		/* Ecken einkreisen */
		ip_out.setColor(0xff00ff);
		ip_out.setLineWidth(1);

		for (Point p: good_corners) {
			ip_out.drawOval((int)p.getX()-4, (int)p.getY()-4, 8, 8);
			if (show_log) {
				IJ.log("("+(int)p.getX()+","+(int)p.getY()+")");
			}
		}

    /* falls Ausgabe in neuem Fenster angezeigt werden soll, tue es hier */
    if (show_in_new_image) {
        /* Ausgabe anzeigen */
        new ImagePlus("Hier sind die Ecken!", ip_out).show();
    }

		if (show_grad) {
			new ImagePlus("X", ip_gradA).show();
			new ImagePlus("Y", ip_gradB).show();
			new ImagePlus("XY", ip_gradC).show();
			new ImagePlus("Gütefunktion", ip_corners).show();
		}
	}

}

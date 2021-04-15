import ij.*;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.util.ArrayUtil;
import java.awt.*;
import java.lang.Math;
import java.util.ArrayList;
// import java.util.Collections;

public class Harris_Eckendetektor implements PlugInFilter {
	
	/* Punkt-Klasse zur Speicherung der Eckpunkte */
	public class Point { 
		private int x;
		private int y;
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
		/* eventuell ohne this */
		public int getX() {
			return this.x;
		}
		public int getY() {
			return this.y;
		}
	}

	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		/* übliche GUI-Prozedur ... kann ich langsam mal auslagern, oder? */
		GenericDialog gd = new GenericDialog("Optionen");
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		gd.setSize(200, 100);
		gd.addNumericField("Kappa: ", 1.0, 2);
		gd.addNumericField("Tau: ", 4.0, 2);
		gd.setLocation((int)screensize.getWidth()/2, (int)screensize.getHeight()/2);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		/* Empfindlichkeit der Eckendetektion */
		// double kappa = gd.getNextNumber() / 100.0;
		double kappa = gd.getNextNumber();
		
		/* Schwellwert */
		// double tau = gd.getNextNumber() * 100000000.0;
		double tau = gd.getNextNumber();
		
		/* Schleifenzähler */
		int x, y, i, j;

		/* Bilddimensionen */
		int w = ip.getWidth();
		int h = ip.getHeight();
		
		double s_x, s_y;
		
		/* "Strukturmatrix" M
		 *		/ A	C \
		 * 		\ C	B /
		 */
		double A, B, C, c;
		
		/* hier landen dann die Eck-Kandidaten */
		ArrayList<Point> edges = new ArrayList<Point>();

		/* lege Graustufenkopie an */
		ImageProcessor ip_gs = ip.convertToFloat();
		
		/* Algorithmus 16.1 [Nischwitz] */
		for (y = 0; y < h; y++) {
			for (x = 0; x < w; x++) {
				/* bestimme partielle Ableitungen; negative Werte werden im nächsten Schritt positiv */
				s_x = (double)((ip_gs.getPixelValue(x+1, y) - ip_gs.getPixelValue(x, y)) +
							   (ip_gs.getPixelValue(x-1, y) - ip_gs.getPixelValue(x, y))) / 255.0;
				s_y = (double)((ip_gs.getPixelValue(x, y+1) - ip_gs.getPixelValue(x, y)) +
							   (ip_gs.getPixelValue(x, y-1) - ip_gs.getPixelValue(x, y))) / 255.0;
				
				/* bestimme M */
				A = s_x * s_x;
				B = s_y * s_y;
				C = s_x * s_y;
				
				c = Math.abs((A * B) - (C * C) - kappa * Math.pow(A + B, 2.0));
				
				/* falls mögliche Ecke (C(x, y) >= tau), speichere Koordinaten */
				if (c >= tau) {
					edges.add(new Point(x, y));
				}
			}
		}
		
		// TODO kacheln und lokale Schwellwerte ermitteln
		
		ImageProcessor ip_out = ip_gs.convertToRGB();
		
		/* Ecken einkreisen */
		ip_out.setColor(0xff00ff);
		ip_out.setLineWidth(1);
		
		for (Point p: edges) {
			ip_out.drawOval(p.getX()-3, p.getY()-3, 6, 6);
		}
		
		/* Ausgabe anzeigen */
		new ImagePlus("Hier sind die Ecken!", ip_out).show();
	}

}


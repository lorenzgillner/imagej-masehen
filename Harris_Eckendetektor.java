import ij.*;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.util.ArrayUtil;
import java.awt.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Collections;

public class Harris_Eckendetektor implements PlugInFilter {
	
	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		/* übliche GUI-Prozedur ... kann ich langsam mal auslagern, oder? */
		GenericDialog gd = new GenericDialog("Optionen");
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		gd.setSize(200, 100);
		gd.addNumericField("Empfindlichkeit: ", 0.5, 2);
		gd.addNumericField("Schwellwert: ", 4.0, 2);
		gd.addNumericField("Radius: ", 1, 0);
		gd.addCheckbox("Zeige Gradientenbilder", true);
		gd.setLocation((int)screensize.getWidth()/2, (int)screensize.getHeight()/2);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		/* Empfindlichkeit der Eckendetektion */
		double i = gd.getNextNumber();
		
		/* Schwellwert */
		double t = gd.getNextNumber();
	
		int RAD = (int)gd.getNextNumber();
		
		/* Flag für Gradientenbilder */
		boolean show_grad = gd.getNextBoolean();
		
		/* Schleifenzähler */
		int x, y;

		/* Bilddimensionen */
		int w = ip.getWidth();
		int h = ip.getHeight();
		
		double s_x, s_y, s_e;
		
		/* "Strukturmatrix" M
		 *		/ A	C \
		 * 		\ C	B /
		 */
		double A, B, C;
		
		/* hier landen dann die Eck-Kandidaten */
		ArrayList<Point> edges = new ArrayList<Point>();

		/* lege Graustufenkopie an */
		ImageProcessor ip_gs = ip.convertToFloat();
		
		/* Gradientenbilder */
		ByteProcessor ip_gradx = new ByteProcessor(w, h);
		ByteProcessor ip_grady = new ByteProcessor(w, h);
		ByteProcessor ip_edges = new ByteProcessor(w, h);
		
		/* Algorithmus 16.1 [Nischwitz] */
		for (y = RAD; y < h - RAD; y++) {
			for (x = RAD; x < w - RAD; x++) {
				// /* bestimme partielle Ableitungen; negative Werte werden im nächsten Schritt positiv */
				// s_x = (double)((ip_gs.getPixelValue(x+1, y) - ip_gs.getPixelValue(x, y)) +
							   // (ip_gs.getPixelValue(x-1, y) - ip_gs.getPixelValue(x, y))) / 255.0;
				// s_y = (double)((ip_gs.getPixelValue(x, y+1) - ip_gs.getPixelValue(x, y)) +
							   // (ip_gs.getPixelValue(x, y-1) - ip_gs.getPixelValue(x, y))) / 255.0;
				
				// /* bestimme M */
				// A = s_x * s_x;
				// B = s_y * s_y;
				// C = s_x * s_y;
				
				// c = Math.abs((A * B) - (C * C) - i * Math.pow(A + B, 2.0));
			
				s_x = Math.abs(ip_gs.getPixelValue(x, y) - ip_gs.getPixelValue(x-RAD, y))
					+ Math.abs(ip_gs.getPixelValue(x, y) - ip_gs.getPixelValue(x+RAD, y));
				
				// s_x /= 2;
				
				s_y = Math.abs(ip_gs.getPixelValue(x, y) - ip_gs.getPixelValue(x, y+RAD))
					+ Math.abs(ip_gs.getPixelValue(x, y) - ip_gs.getPixelValue(x, y-RAD));
				
				// s_y /= 2;
				
				ip_gradx.putPixelValue(x, y, s_x);
				ip_grady.putPixelValue(x, y, s_y);
				
				/* bestimme M */
				A = s_x * s_x;
				B = s_y * s_y;
				C = s_x * s_y;
				
				/* C(x,y) */
				s_e = (A * B) - (C * C) - i * Math.pow(A + B, 2.0);
				IJ.log("A="+A+", B="+B+", C="+C+", s_e="+s_e);
				
				ip_edges.putPixelValue(x, y, s_e);
				
				// /* falls mögliche Ecke (C(x, y) >= tau), speichere Koordinaten */
				// if (s_e >= t) {
					// edges.add(new Point(x, y));
				// }
			}
		}
		
		// TODO eventuell kacheln und lokale Schwellwerte ermitteln
		
		// ImageProcessor ip_out = ip.duplicate();
		
		// /* Ecken einkreisen */
		// ip_out.setColor(0xff00ff);
		// ip_out.setLineWidth(1);
		
		for (Point p: edges) {
			// ip_out.drawOval((int)p.getX()-3, (int)p.getY()-3, 6, 6);
			// if (ip_edges.getPixelValue((int)p.getX(), (int)p.getY()) != 0.0) {
				// IJ.log(""+ ip_edges.getPixelValue((int)p.getX(), (int)p.getY()));
			// }
		}

        // IJ.log("Harris Eckendetektor: i="+i+", t="+t+", #edges="+edges.size());
		
		// /* Ausgabe anzeigen */
		// new ImagePlus("Hier sind die Ecken!", ip_out).show();
		
		if (show_grad) {
			// new ImagePlus("Gradient X", ip_gradx).show();
			// new ImagePlus("Gradient Y", ip_grady).show();
			// new ImagePlus("Eckkandidaten", ip_edges).show();
		}
	}

}


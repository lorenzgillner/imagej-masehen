import ij.*;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.util.ArrayUtil;
import java.awt.*;
import java.lang.Math;
import java.util.*;

public class Harris_Eckendetektor implements PlugInFilter {
	
	public void toFloat(double[] a, float[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = (float)a[i];
		}
	}
	
	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Optionen");
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		gd.setSize(200, 100);
		gd.addNumericField("Empfindlichkeit: ", 0.05, 2);
		gd.addNumericField("Schwellwert: ", 0.09, 2);
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
		
		double s_x, s_y, s_e, current_pixel_value;
		
		/* "Strukturmatrix" M
		 *		/ A	C \
		 * 		\ C	B /
		 */
		double A, B, C, H;
		
		double[][] neighbors = new double[5][5];
		float[] neighborsf = new float[5];
		
		/* hier landen dann die Eck-Kandidaten */
		ArrayList<Point> corners = new ArrayList<Point>();
		ArrayList<Point> clean_corners = new ArrayList<Point>();

		/* lege Graustufenkopie an */
		FloatProcessor ip_gs = ip.convertToFloatProcessor();
		
		/* normalisiere Pixel auf [0, 1] */
		ip_gs.multiply(1.0/255.0);
		
		/* etwas weichzeichnen */
		ip_gs.dilate();
		
		/* Gradientenbilder */
		FloatProcessor ip_gradx = new FloatProcessor(w, h);
		FloatProcessor ip_grady = new FloatProcessor(w, h);
		FloatProcessor ip_corners = new FloatProcessor(w, h);
		
		/* Algorithmus 16.1 [Nischwitz] */
		for (y = RAD+1; y < h-RAD-1; y++) {
			for (x = RAD+1; x < w-RAD-1; x++) {
				/* speichere aktuellen Pixelwert, damit wir den nicht immer wieder neu holen müssen */
				current_pixel_value = ip_gs.getPixelValue(x, y);
				
				/* bestimme Intensität der Abweichung an (x,y) */
				s_x = Math.abs(ip_gs.getPixelValue(x-RAD, y-RAD) - ip_gs.getPixelValue(x, y-RAD))
					+ Math.abs(ip_gs.getPixelValue(x, y-RAD) - ip_gs.getPixelValue(x+RAD, y-RAD))
					+ Math.abs(ip_gs.getPixelValue(x-RAD-1, y) - current_pixel_value)
					+ Math.abs(current_pixel_value - ip_gs.getPixelValue(x+RAD+1, y))
					+ Math.abs(ip_gs.getPixelValue(x-RAD, y+RAD) - ip_gs.getPixelValue(x, y+RAD))
					+ Math.abs(ip_gs.getPixelValue(x, y+RAD) - ip_gs.getPixelValue(x+RAD, y+RAD));
				s_x /= 6;
				
				s_y = Math.abs(ip_gs.getPixelValue(x-RAD, y-RAD) - ip_gs.getPixelValue(x-RAD, y))
					+ Math.abs(ip_gs.getPixelValue(x-RAD, y) - ip_gs.getPixelValue(x-RAD, y+RAD))
					+ Math.abs(ip_gs.getPixelValue(x, y-RAD-1) - current_pixel_value)
					+ Math.abs(current_pixel_value - ip_gs.getPixelValue(x, y+RAD+1))
					+ Math.abs(ip_gs.getPixelValue(x+RAD, y-RAD) - ip_gs.getPixelValue(x+RAD, y))
					+ Math.abs(ip_gs.getPixelValue(x+RAD, y) - ip_gs.getPixelValue(x+RAD, y+RAD));
				s_y /= 6;
				
				ip_gradx.putPixelValue(x, y, s_x);
				ip_grady.putPixelValue(x, y, s_y);
				
				/* bestimme M */
				H = 5;
				A = (s_x * s_x) * H;
				B = (s_y * s_y) * H;
				C = (s_x * s_y) * H;
				
				/* C(x,y) */
				s_e = Math.abs(((A * B) - (C * C)) - i * Math.pow(A + B, 2.0));
				
				/* falls mögliche Ecke (s_e über Schwellwert), speichere Koordinaten */
				if (s_e >= t) {
					corners.add(new Point(x, y));
				}
				ip_corners.putPixelValue(x, y, s_e);
			}
		}

		/* Ecken der Größe nach absteigend sortieren */
		Collections.sort(corners, new Comparator<Point>() {
			@Override
			public int compare(Point p1, Point p2) {
				return (
					ip_corners.getPixelValue((int)p2.getX(), (int)p2.getY()) > ip_corners.getPixelValue((int)p1.getX(), (int)p1.getY())
				) ? 1 : -1;
			}
		});
		
		IJ.log(""+corners);
		
		for (Point p: corners) {
			int u = (int)p.getX();
			int v = (int)p.getY();
			boolean is_max = false;
			
			ip_corners.getNeighborhood(u, v, neighbors);
			
			for (int j = 0; j < neighbors.length; j++) {
				toFloat(neighbors[j], neighborsf);
				if (ip_corners.getPixelValue(u, v) / new ArrayUtil(neighborsf).getMaximum() == 1.0) {
					is_max = true;
					IJ.log(""+new ArrayUtil(neighborsf).getMaximum()+" "+ip_corners.getPixelValue(u, v)+" "+ip_corners.getPixelValue(u, v) / new ArrayUtil(neighborsf).getMaximum());
				}
			}
			
			if (is_max) {
				clean_corners.add(p);
			}
		}
		
		IJ.log(""+clean_corners);
		
		ImageProcessor ip_out = ip.duplicate();
		
		/* Ecken einkreisen */
		ip_out.setColor(0xff00ff);
		ip_out.setLineWidth(1);
		
		for (Point p: clean_corners) {
			ip_out.drawOval((int)p.getX()-3, (int)p.getY()-3, 6, 6);
			// if (ip_corners.getPixelValue((int)p.getX(), (int)p.getY()) != 0.0) {
				// IJ.log(""+ ip_corners.getPixelValue((int)p.getX(), (int)p.getY()));
			// }
		}

        // IJ.log("Harris Eckendetektor: i="+i+", t="+t+", #corners="+corners.size());
		
		// /* Ausgabe anzeigen */
		new ImagePlus("Hier sind die Ecken!", ip_out).show();
		
		if (show_grad) {
			new ImagePlus("Gradient X", ip_gradx).show();
			new ImagePlus("Gradient Y", ip_grady).show();
			new ImagePlus("Gradient XY", ip_corners).show();
		}
	}

}

// s_x = Math.abs(ip_gs.getPixelValue(x-RAD, y-RAD) - ip_gs.getPixelValue(x+RAD, y-RAD))
	// + Math.abs(ip_gs.getPixelValue(x-RAD-1, y) - ip_gs.getPixelValue(x+RAD+1, y))
	// + Math.abs(ip_gs.getPixelValue(x-RAD, y+RAD) - ip_gs.getPixelValue(x+RAD, y+RAD));
// s_x /= 3;

// s_y = Math.abs(ip_gs.getPixelValue(x-RAD, y-RAD) - ip_gs.getPixelValue(x-RAD, y+RAD))
	// + Math.abs(ip_gs.getPixelValue(x, y-RAD-1) - ip_gs.getPixelValue(x, y+RAD+1))
	// + Math.abs(ip_gs.getPixelValue(x+RAD, y-RAD) - ip_gs.getPixelValue(x+RAD, y+RAD));
// s_y /= 3;


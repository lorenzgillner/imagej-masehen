import ij.*;
import ij.ImagePlus;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;

public class Template_Matcher implements PlugInFilter {

    public int setup(String arg, ImagePlus imp) {
        return DOES_ALL+ROI_REQUIRED+SUPPORTS_MASKING;
    }

    public void run(ImageProcessor ip) {

        /* Dimensionen des Originalbildes */
        int w = ip.getWidth();
        int h = ip.getHeight();

        /* hole ROI (Template, das gematcht werden soll) */
        Rectangle r = ip.getRoi();

        ImagePlus copy = ImagePlus("Arbeitskopie", ip);
        ImageProcessor copy_ip = copy.getProcessor();

        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
        
        /* binarisiere die Arbeitskopie */
        copy_ip.autoThreshold();
        
        copy.show();
        copy.updateAndDraw();
    }

}


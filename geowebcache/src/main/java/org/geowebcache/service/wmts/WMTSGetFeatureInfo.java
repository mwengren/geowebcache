package org.geowebcache.service.wmts;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.util.ServletUtils;

public class WMTSGetFeatureInfo {
    
    private static Log log = LogFactory.getLog(org.geowebcache.service.wmts.WMTSService.class);
    
    private ConveyorTile convTile;
    
    private String[] values;
    
    int i;
    
    int j;
    
    protected WMTSGetFeatureInfo(ConveyorTile convTile) throws GeoWebCacheException {
        
        String[] keys = { "i", "j" };
        
        values = ServletUtils.selectedStringsFromMap(
                convTile.servletReq.getParameterMap(), 
                convTile.servletReq.getCharacterEncoding(), 
                keys );
        
        try {
            
            i = Integer.parseInt(values[0]);
            j = Integer.parseInt(values[1]);
            
        } catch(NumberFormatException nfe) {
            throw new GeoWebCacheException("Unable to parse i or j : " + values[0] + " " + values[1]);
        }
        
        this.convTile = convTile;
    }
    
    protected void writeResponse() throws GeoWebCacheException {
        TileLayer layer = convTile.getLayer();
        
        WMSLayer wmsLayer = null;

        if (layer instanceof WMSLayer) {
            wmsLayer = (WMSLayer) layer;
        }

        byte[] data = wmsLayer.getFeatureInfo(convTile, i, j);

        convTile.servletResp.setStatus(HttpServletResponse.SC_OK);
        convTile.servletResp.setContentType(convTile.getMimeType().getMimeType());
        convTile.servletResp.setContentLength(data.length);

        try {
            OutputStream os = convTile.servletResp.getOutputStream();
            os.write(data);
            os.flush();
        } catch (IOException ioe) {
            log.debug("Caught IOException" + ioe.getMessage());
        }

    }
}

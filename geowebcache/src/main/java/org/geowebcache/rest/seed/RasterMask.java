/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Gabriel Roldan, OpenGeo, Copyright 2010
 * @author Arne Kepp, OpenGeo, Copyright 2010
 */
package org.geowebcache.rest.seed;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import org.geowebcache.grid.GridSubset;

public class RasterMask {
    /**
     * By zoom level bitmasked images where every pixel represents a tile in the level's
     * {@link GridSubset#getCoverages() grid coverage}.
     */
    private final BufferedImage[] byLevelMasks;

    private final long[][] gridCoverages;

    private final int maxMaskLevel;

    private final int noDataValue;

    /**
     * Creates a RasterMask from the given parameters with default {@code noDataValue == 0}
     * 
     * @param byLevelMasks
     * @param gridCoverages
     * @see #RasterMask(BufferedImage[], long[][], int)
     */
    public RasterMask(BufferedImage[] byLevelMasks, long[][] gridCoverages) {
        this(byLevelMasks, gridCoverages, 0);
    }

    /**
     * Creates a RasterMask based on a set of bitmask images and covered tile grid bounds;
     * <p>
     * The number of zoom levels is determined by the length of the {@code gridCoverages} array. The
     * length of the {@code byLevelMasks} might be lower than the actual zoom levels, meaning the
     * values for any zoom level for which a masking image is not provided will be interpolated from
     * the higher resolution available one.
     * </p>
     * <p>
     * Also, note each bounding box in {@code gridCoverages} may represent a smaller area than its
     * bitmasked image, which represents the whole tile range for the layer at a specific zoom
     * level.
     * </p>
     * 
     * @param byLevelMasks
     * @param gridCoverages
     * @param noDatavalue
     *            raster sample value to be considered as no-data (eg, tile is not set at the pixel
     *            location)
     */
    public RasterMask(BufferedImage[] byLevelMasks, long[][] gridCoverages, final int noDataValue) {
        this.byLevelMasks = byLevelMasks;
        this.gridCoverages = gridCoverages;
        this.maxMaskLevel = byLevelMasks.length - 1;
        this.noDataValue = noDataValue;
    }

    public long[][] getGridCoverages() {
        return gridCoverages;
    }

    public boolean lookup(long[] idx) {
        return lookup(idx[0], idx[1], (int) idx[2]);
    }

    public boolean lookup(long tileX, long tileY, int level) {
        if (level > maxMaskLevel) {
            // downsample
            long[] requestedCoverage = getGridCoverages()[level];

            long[] lastMaskedCoverage = getGridCoverages()[maxMaskLevel];

            double requestedW = 1 + requestedCoverage[2] - requestedCoverage[0];
            double requestedH = 1 + requestedCoverage[3] - requestedCoverage[1];

            double availableW = 1 + lastMaskedCoverage[2] - lastMaskedCoverage[0];
            double availableH = 1 + lastMaskedCoverage[3] - lastMaskedCoverage[1];

            tileX = Math.round(tileX * (availableW / requestedW));
            tileY = Math.round(tileY * (availableH / requestedH));
            tileX = Math.max(Math.min(tileX, lastMaskedCoverage[2]), lastMaskedCoverage[0]);
            tileY = Math.max(Math.min(tileY, lastMaskedCoverage[3]), lastMaskedCoverage[1]);

            level = maxMaskLevel;
        }

        return isTileSet(tileX, tileY, level);
    }

    private boolean isTileSet(long tileX, long tileY, int level) {
        long[] coverage = getGridCoverages()[level];

        if (tileX < coverage[0] || tileX > coverage[2] || tileY < coverage[1]
                || tileY > coverage[3]) {
            return false;
        }

        /*
         * Use getRaster instead of getData(), getData() returns a copy
         */
        final Raster raster = byLevelMasks[level].getRaster();

        // Changing index to top left hand origin
        long rasx = tileX;
        long rasy = (raster.getHeight() - 1) - tileY;

        int sample = raster.getSample((int) rasx, (int) rasy, 0);

        return sample != noDataValue;
    }
}

package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongLatGrid extends Grid {
  private static final Logger logger = LoggerFactory.getLogger(LongLatGrid.class);

  double longStart;
  double latStart;
  double longEnd;
  double latEnd;
  double gridSize;

  private static final double EPS = 1e-8;

  private static void checkValue(double v, double l, double r) {
    if(v < l - EPS || v > r + EPS) {
      throw new IllegalArgumentException("" + v + " not in range [" + l + ", " + r + "]");
    }
  }

  public LongLatGrid(long[] shape, double longStart, double latStart, double gridSize) {
    super(shape);
    if(shape.length < 2) {
      throw new IllegalArgumentException("Dimension must greater than 2");
    }
    this.latStart = latStart;
    this.longStart = longStart;
    this.longEnd = longStart + shape[shape.length - 2] * gridSize;
    this.latEnd = latStart + shape[shape.length - 1] * gridSize;
    this.gridSize = gridSize;

    checkValue(longStart, -180, 180);
    checkValue(latStart, -90, 90);
    checkValue(longEnd, -180, 180);
    checkValue(latEnd, -180, 180);
    logger.info("LongLatGrid: Lon [{}, {}), Lat [{}, {}), Size {}", longStart, longEnd, latStart, latEnd, gridSize);
  }

  /**
   * Get value by longitude and latitude
   * @param preIndex index before longitude and latitude (e.g. month)
   * @param lon longitude
   * @param lat latitude
   * @return if present return value else return nan
   */
  public double queryByLongLat(long[] preIndex, double lon, double lat) {
    checkValue(lon, -180, 180);
    checkValue(lat, -90, 90);
    if(lon < longStart || lon >= longEnd) return Double.NaN;
    if(lat < latStart || lat >= latEnd) return Double.NaN;
    // longStart + gridSize * n <= lon < longStart + gridSize * (n + 1)
    // gridSize * n <= lon - longStart < gridSize * (n + 1)
    // n <= (lon - longStart) / gridSize < n + 1
    // floor((lon - longStart) / gridSize)
    long longIndex = (long)Math.floor((lon - longStart) / gridSize);
    long latIndex = (long)Math.floor((lat - latStart) / gridSize);
    long[] finalIndex = new long[preIndex.length + 2];
    for(int i = 0; i < preIndex.length; i++) finalIndex[i] = preIndex[i];
    finalIndex[preIndex.length] = longIndex;
    finalIndex[preIndex.length + 1] = latIndex;

    return query(finalIndex);
  }
}

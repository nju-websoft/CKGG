package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

public class Grid {
  private static final Logger logger = LoggerFactory.getLogger(Grid.class);

  double[] data;
  long[] shape;
  long[] shapeMul;

  public Grid(long[] shape) {
    this.shape = shape;

    shapeMul = new long[shape.length + 1];
    shapeMul[shape.length] = 1;

    for(int i = shape.length - 1; i >= 0; i--) {
      shapeMul[i] = shapeMul[i + 1] * shape[i];
    }
    data = new double[(int)shapeMul[0]];
  }

  public void load(String inputBinaryPath) throws IOException {
    logger.info("Loading grid from {}", inputBinaryPath);

    BufferedInputStream stream;
    if(inputBinaryPath.endsWith(".gz")) {
      logger.info("Using gzip decompression for file {}", inputBinaryPath);
      stream = new BufferedInputStream(new GZIPInputStream(new FileInputStream(inputBinaryPath)));
    } else {
      stream = new BufferedInputStream(new FileInputStream(inputBinaryPath));
    }

    int offset = 0;
    int size = 32 * 1024 * 1024;
    byte[] bytes = new byte[size];

    int count;
    while ((count = stream.read(bytes, 0, size)) > 0) {
      // logger.info("Count {}", count);
      if(count % 8 != 0) {
        throw new IllegalArgumentException("Misaligned file");
      }
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(data, offset, count / 8);
      offset += count / 8;
    }
    stream.close();

    logger.info("first element {}", data[0]);
    logger.info("length {}", data.length);
    if(offset != shapeMul[0]) {
      logger.error("Expected shape {}, total {}, actual total {}", shape, shapeMul[0], offset);
      throw new IOException();
    }
  }

  public double query(long[] index) {
    long findex = 0;
    for(int i = 0; i < index.length; i++) {
      findex += index[i] * shapeMul[i + 1];
    }
    return data[(int) findex];
  }
}

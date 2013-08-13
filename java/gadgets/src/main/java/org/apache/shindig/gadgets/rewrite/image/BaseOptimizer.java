/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.rewrite.image;

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.sun.imageio.plugins.jpeg.JPEG;
import com.sun.imageio.plugins.jpeg.JPEGImageWriter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

/**
 * Base class for image optimizers
 */
abstract class BaseOptimizer {
  static final Map<String, ImageFormat> FORMAT_NAME_TO_IMAGE_FORMAT = ImmutableMap.of(
      "png", ImageFormat.IMAGE_FORMAT_PNG,
      "gif", ImageFormat.IMAGE_FORMAT_GIF,
      "jpeg", ImageFormat.IMAGE_FORMAT_JPEG);

  final HttpResponseBuilder response;
  final OptimizerConfig config;

  protected ImageOutputter outputter;
  protected byte[] minBytes;
  protected int minLength;
  protected JpegImageUtils.JpegImageParams sourceImageParams;
  int reductionPct;

  public BaseOptimizer(OptimizerConfig config, HttpResponseBuilder response) {
    this(config, response, null);
  }

  public BaseOptimizer(OptimizerConfig config, HttpResponseBuilder response,
                       JpegImageUtils.JpegImageParams sourceImageParams) {
    this.config = config;
    this.response = response;
    this.minLength = response.getContentLength();
    this.sourceImageParams = sourceImageParams;
    this.outputter = getOutputter();
  }

  protected ImageOutputter getOutputter() {
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(getOriginalFormatName());
    if (writers.hasNext()) {
      ImageWriter writer = writers.next();
      ImageWriteParam param = writer.getDefaultWriteParam();
      if (getOriginalFormatName().equals("jpeg")) {
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(config.getJpegCompression());
        if (param instanceof JPEGImageWriteParam) {
          ((JPEGImageWriteParam) param).setOptimizeHuffmanTables(
                config.getJpegHuffmanOptimization());
        }
      }

      JpegImageUtils.SamplingModes samplingMode = JpegImageUtils.SamplingModes.DEFAULT;
      if (config.getJpegRetainSubsampling() && sourceImageParams != null) {
        samplingMode = sourceImageParams.getSamplingMode();
      }
      return new ImageIOOutputter(writer, param, samplingMode);
    }
    return new SanselanOutputter(FORMAT_NAME_TO_IMAGE_FORMAT.get(getOriginalFormatName()));
  }

  /**
   * Write the image using a specified write param
   */
  protected void write(BufferedImage image) throws IOException {
    if (image == null) {
      return;
    }

    byte[] bytes = outputter.toBytes(image);
    if (minLength > bytes.length) {
      minBytes = bytes;
      minLength = minBytes.length;
      reductionPct = ((response.getContentLength() - minLength) * 100) /
          response.getContentLength();
    }
  }

  public void rewrite(BufferedImage image) throws IOException {
    if (outputter == null) {
      return;
    }

    long time = System.currentTimeMillis();
    rewriteImpl(image);
    time = System.currentTimeMillis() - time;
    if (minBytes != null && minBytes.length != 0) {
      StringBuilder rewriteMsg = new StringBuilder(24);
      rewriteMsg.append("c=").append(
          ((minBytes.length * 100) / response.getContentLength()));
      if (!getOutputContentType().equals(getOriginalContentType())) {
        rewriteMsg.append(";o=").append(getOriginalContentType());
      }
      rewriteMsg.append(";t=").append(time);

      // Removing the original 'Etag' header as we have updated the content.
      response.removeHeader("ETag");
      response
          .setHeader("Content-Type", getOutputContentType())
          .setHeader("X-Shindig-Rewrite", rewriteMsg.toString())
          .setResponse(minBytes);
    }
  }

  /**
   * Get the rewritten image if available
   */
  protected final byte[] getRewrittenImage() {
    return minBytes;
  }

  protected abstract void rewriteImpl(BufferedImage image) throws IOException;

  protected abstract String getOutputContentType();

  protected abstract String getOriginalContentType();

  protected abstract String getOriginalFormatName();

  /**
   * Interface to allow for different serialization libraries to be used
   */
  public static interface ImageOutputter {
    byte[] toBytes(BufferedImage image) throws IOException;
  }

  /**
   * Standard ImageIO based image outputter
   */
  public static class ImageIOOutputter implements ImageOutputter {

    ImageWriter writer;
    ByteArrayOutputStream baos;
    ImageWriteParam writeParam;
    JpegImageUtils.SamplingModes jpegSamplingMode;

    public ImageIOOutputter(ImageWriter writer, ImageWriteParam writeParam) {
      this(writer, writeParam, JpegImageUtils.SamplingModes.DEFAULT);
    }

    public ImageIOOutputter(ImageWriter writer, ImageWriteParam writeParam,
                            JpegImageUtils.SamplingModes jpegSamplingMode) {
      this.writer = writer;
      this.writeParam = Objects.firstNonNull(writeParam, writer.getDefaultWriteParam());
      this.jpegSamplingMode = jpegSamplingMode;
    }

    public byte[] toBytes(BufferedImage image) throws IOException {
      if (baos == null) {
        baos = new ByteArrayOutputStream();
      } else {
        baos.reset();
      }
      writer.setOutput(ImageIO.createImageOutputStream(baos));

      // Create a new empty metadata set
      ImageWriteParam metaImageWriteParam = writeParam;
      if (writer instanceof JPEGImageWriter) {
        // There is an issue in the javax code because of which function call
        // writer.getDefaultImageMetadata(new ImageTypeSpecifier(image.getColorModel(),
        //    image.getSampleModel()), writeParam);
        //
        // does buggy processing for compression ratio parameter in ImageWriteParam.
        // Hence passing null as ImageWriteParam here to ignore this processing and
        // passing the ImageWriteParam later in the writer.write() call.
        metaImageWriteParam = null;
      }

      IIOMetadata metadata = writer.getDefaultImageMetadata(
          new ImageTypeSpecifier(image.getColorModel(), image.getSampleModel()),
          metaImageWriteParam);

      if (jpegSamplingMode.getModeValue() > 0 && writer instanceof JPEGImageWriter) {
        setJpegSubsamplingMode(metadata);
      }

      writer.write(null, new IIOImage(image, Collections.<BufferedImage>emptyList(), metadata),
                   metaImageWriteParam == null ? writeParam : null);

      return baos.toByteArray();
    }

    private void setJpegSubsamplingMode(IIOMetadata metadata)
        throws IIOInvalidTreeException {
      // Tweaking the image metadata to override default subsampling(4:2:0) with
      // 4:4:4.
      Node rootNode = metadata.getAsTree(JPEG.nativeImageMetadataFormatName);
      boolean metadataUpdated = false;
      // The top level root node has two children, out of which the second one will
      // contain all the information related to image markers.
      if (rootNode.getLastChild() != null) {
        Node markerNode = rootNode.getLastChild();
        NodeList markers = markerNode.getChildNodes();
        // Search for 'SOF' marker where subsampling information is stored.
        for (int i = 0; i < markers.getLength(); i++) {
          Node node = markers.item(i);
          // 'SOF' marker can have
          //   1 child node if the color representation is greyscale,
          //   3 child nodes if the color representation is YCbCr, and
          //   4 child nodes if the color representation is YCMK.
          // This subsampling applies only to YCbCr.
          if (node.getNodeName().equalsIgnoreCase("sof") && node.hasChildNodes() &&
              node.getChildNodes().getLength() == 3) {
            // In 'SOF' marker, first child corresponds to the luminance channel, and setting
            // the HsamplingFactor and VsamplingFactor to 1, will imply 4:4:4 chroma subsampling.
            NamedNodeMap attrMap = node.getFirstChild().getAttributes();
            int samplingMode = jpegSamplingMode.getModeValue();
            attrMap.getNamedItem("HsamplingFactor").setNodeValue((samplingMode & 0xf) + "");
            attrMap.getNamedItem("VsamplingFactor").setNodeValue(((samplingMode >> 4) & 0xf) + "");
            metadataUpdated = true;
            break;
          }
        }
      }

      // Read the updated metadata from the metadata node tree.
      if (metadataUpdated) {
        metadata.setFromTree(JPEG.nativeImageMetadataFormatName, rootNode);
      }
    }
  }

  /**
   * Sanselan based image outputter
   */
  public static class SanselanOutputter implements ImageOutputter {

    ImageFormat format;
    ByteArrayOutputStream baos;
    public SanselanOutputter(ImageFormat format) {
      this.format = format;
    }

    public byte[] toBytes(BufferedImage image) throws IOException {
      if (baos == null) {
        baos = new ByteArrayOutputStream();
      } else {
        baos.reset();
      }
      try {
        Sanselan.writeImage(image, baos, format, Maps.newHashMap());
        return baos.toByteArray();
      } catch (ImageWriteException iwe) {
        throw new IOException(iwe.getMessage());
      }
    }
  }
}

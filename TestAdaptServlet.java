package com.charan.test;

import com.day.cq.wcm.commons.AbstractImageServlet;
import com.day.cq.wcm.commons.AbstractImageServlet.ImageContext;
import com.day.cq.wcm.foundation.AdaptiveImageHelper;
import com.day.cq.wcm.foundation.AdaptiveImageHelper.Quality;
import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import javax.jcr.RepositoryException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype=true, label="Test image layer Servlet", description="Test image layer")
@Service
@Properties({@Property(name="sling.servlet.resourceTypes", value={""}, propertyPrivate=true), @Property(name="sling.servlet.selectors", value={"crop"}, propertyPrivate=true), @Property(name="sling.servlet.extensions", value={"jpg", "jpeg", "png", "gif"}, propertyPrivate=true)})
public class TestAdaptServlet extends AbstractImageServlet
{
  private static final Logger log = LoggerFactory.getLogger(AdaptiveImageComponentServlet.class);
  private static final long serialVersionUID = 42L;
  private static final String FULL_SIZE_SELECTOR = "full";

  @Property(value={"320", "480", "476", "620"}, label="Supported Widths", description="List of widths this component is permitted to generate.")
  private static final String PROPERTY_SUPPORTED_WIDTHS = "adapt.supported.widths";
  private List<String> supportedWidths;

  protected void activate(ComponentContext componentContext)
  {
    Dictionary properties = componentContext.getProperties();

    this.supportedWidths = new ArrayList();
    String[] supportedWidthsArray = OsgiUtil.toStringArray(properties.get("adapt.supported.widths"));
    if ((supportedWidthsArray != null) && (supportedWidthsArray.length > 0))
      for (String width : supportedWidthsArray)
        this.supportedWidths.add(width);
  }

  protected Layer createLayer(AbstractImageServlet.ImageContext imageContext)
    throws RepositoryException, IOException
  {
    SlingHttpServletRequest request = imageContext.request;
    String[] selectors = request.getRequestPathInfo().getSelectors();

    if ((selectors.length != 3) && (selectors.length != 1)) {
      log.error("Unsupported number of selectors.");
      return null;
    }

    String widthSelector = "full";
    if (selectors.length == 3) {
      widthSelector = selectors[1];
    }

    if (!isDimensionSupported(widthSelector)) {
      log.error("Unsupported width requested: {}.", widthSelector);
      return null;
    }

    Image image = new Image(imageContext.resource);

    if (!image.hasContent()) {
      log.error("The image associated with this page does not have a valid file reference; drawing a placeholder.");

      return null;
    }

    AdaptiveImageHelper adaptiveHelper = new AdaptiveImageHelper();

    if ("full".equals(widthSelector))
    {
      return adaptiveHelper.applyStyleDataToImage(image, imageContext.style);
    }

    return adaptiveHelper.scaleThisImage(image, Integer.parseInt(widthSelector), 0, imageContext.style);
  }

  protected boolean isDimensionSupported(String widthStr)
  {
    Iterator iterator = getSupportedWidthsIterator();
    if ("full".equals(widthStr)) {
      return true;
    }
    int width = Integer.parseInt(widthStr);
    while (iterator.hasNext()) {
      if (width == Integer.parseInt((String)iterator.next())) {
        return true;
      }
    }

    return false;
  }

  protected Iterator<String> getSupportedWidthsIterator()
  {
    return this.supportedWidths.iterator();
  }

  protected void writeLayer(SlingHttpServletRequest request, SlingHttpServletResponse response, AbstractImageServlet.ImageContext context, Layer layer)
    throws IOException, RepositoryException
  {
    String[] selectors = request.getRequestPathInfo().getSelectors();
    double quality;
    double quality;
    if (selectors.length == 3) {
      String imageQualitySelector = selectors[2];
      quality = getRequestedImageQuality(imageQualitySelector);
    }
    else {
      quality = getImageQuality();
    }

    writeLayer(request, response, context, layer, quality);
  }

  private double getRequestedImageQuality(String imageQualitySelector)
  {
    AdaptiveImageHelper.Quality newQuality = AdaptiveImageHelper.getQualityFromString(imageQualitySelector);
    if (newQuality != null) {
      return newQuality.getQualityValue();
    }

    return getImageQuality();
  }

  protected String getImageType()
  {
    return "image/jpeg";
  }
}
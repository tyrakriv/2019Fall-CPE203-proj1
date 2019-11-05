import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import processing.core.PApplet;
import processing.core.PImage;

/*
ImageStore: to ideally keep track of the images used in our virtual world
 */

final class ImageStore
{
   private Map<String, List<PImage>> images;
   private List<PImage> defaultImages;

   private final String OCTO_KEY = "octo";
   private final String OBSTACLE_KEY = "obstacle";
   private final String FISH_KEY = "fish";
   private final String SGRASS_KEY = "seaGrass";
   private final String BGND_KEY = "background";
   private final int PROPERTY_KEY = 0;
   private final String ATLANTIS_KEY = "atlantis";
   private final int OCTO_NUM_PROPERTIES = 7;
   private final int OCTO_ID = 1;
   private final int OCTO_COL = 2;
   private final int OCTO_ROW = 3;
   private final int OCTO_LIMIT = 4;
   private final int OCTO_ACTION_PERIOD = 5;
   private final int OCTO_ANIMATION_PERIOD = 6;

   private final int OBSTACLE_NUM_PROPERTIES = 4;
   private final int OBSTACLE_ID = 1;
   private final int OBSTACLE_COL = 2;
   private final int OBSTACLE_ROW = 3;

   private final int ATLANTIS_NUM_PROPERTIES = 4;
   private final int ATLANTIS_ID = 1;
   private final int ATLANTIS_COL = 2;
   private final int ATLANTIS_ROW = 3;

   private final int FISH_NUM_PROPERTIES = 5;
   private final int FISH_ID = 1;
   private final int FISH_COL = 2;
   private final int FISH_ROW = 3;
   private final int FISH_ACTION_PERIOD = 4;

   private final int SGRASS_NUM_PROPERTIES = 5;
   private final int SGRASS_ID = 1;
   private final int SGRASS_COL = 2;
   private final int SGRASS_ROW = 3;
   private final int SGRASS_ACTION_PERIOD = 4;

   private final int BGND_NUM_PROPERTIES = 4;
   private final int BGND_ID = 1;
   private final int BGND_COL = 2;
   private final int BGND_ROW = 3;

   private final int KEYED_IMAGE_MIN = 5;
   private final int KEYED_RED_IDX = 2;
   private final int KEYED_GREEN_IDX = 3;
   private final int KEYED_BLUE_IDX = 4;
   private final int COLOR_MASK = 0xffffff;

   public ImageStore(PImage defaultImage)
   {
      this.images = new HashMap<>();
      defaultImages = new LinkedList<>();
      defaultImages.add(defaultImage);
   }

   public List<PImage> getImageList(String key)
   {
      return this.images.getOrDefault(key, this.defaultImages);
   }

   private List<PImage> getImages(Map<String, List<PImage>> images, String key)
   {
      List<PImage> imgs = images.get(key);
      if (imgs == null)
      {
         imgs = new LinkedList<>();
         images.put(key, imgs);
      }
      return imgs;
   }

   /*
     Called with color for which alpha should be set and alpha value.
     setAlpha(img, color(255, 255, 255), 0));
   */
   private void setAlpha(PImage img, int maskColor, int alpha)
   {
      int alphaValue = alpha << 24;
      int nonAlpha = maskColor & COLOR_MASK;
      img.format = PApplet.ARGB;
      img.loadPixels();
      for (int i = 0; i < img.pixels.length; i++)
      {
         if ((img.pixels[i] & COLOR_MASK) == nonAlpha)
         {
            img.pixels[i] = alphaValue | nonAlpha;
         }
      }
      img.updatePixels();
   }



   public boolean processLine(String line, WorldModel world)
   {
      String[] properties = line.split("\\s");
      if (properties.length > 0)
      {
         switch (properties[PROPERTY_KEY])
         {
            case BGND_KEY:
               return parseBackground(properties, world);
            case OCTO_KEY:
               return parseOcto(properties, world);
            case OBSTACLE_KEY:
               return parseObstacle(properties, world);
            case FISH_KEY:
               return parseFish(properties, world);
            case ATLANTIS_KEY:
               return parseAtlantis(properties, world);
            case SGRASS_KEY:
               return parseSgrass(properties, world);
         }
      }
      return false;
   }

   public void load(Scanner in, WorldModel world)
   {
      int lineNumber = 0;
      while (in.hasNextLine())
      {
         try
         {
            if (!processLine(in.nextLine(), world))
            {
               System.err.println(String.format("invalid entry on line %d",
                       lineNumber));
            }
         }
         catch (NumberFormatException e)
         {
            System.err.println(String.format("invalid entry on line %d",
                    lineNumber));
         }
         catch (IllegalArgumentException e)
         {
            System.err.println(String.format("issue on line %d: %s",
                    lineNumber, e.getMessage()));
         }
         lineNumber++;
      }
   }


   public void loadImages(Scanner in, PApplet screen)
   {
      int lineNumber = 0;
      while (in.hasNextLine())
      {
         try
         {
            processImageLine(in.nextLine(), screen);
         }
         catch (NumberFormatException e)
         {
            System.out.println(String.format("Image format error on line %d",
                    lineNumber));
         }
         lineNumber++;
      }
   }

   public void processImageLine(String line, PApplet screen)
   {
      String[] attrs = line.split("\\s");
      if (attrs.length >= 2)
      {
         String key = attrs[0];
         PImage img = screen.loadImage(attrs[1]);
         if (img != null && img.width != -1)
         {
            List<PImage> imgs = getImages(images, key);
            imgs.add(img);

            if (attrs.length >= KEYED_IMAGE_MIN)
            {
               int r = Integer.parseInt(attrs[KEYED_RED_IDX]);
               int g = Integer.parseInt(attrs[KEYED_GREEN_IDX]);
               int b = Integer.parseInt(attrs[KEYED_BLUE_IDX]);
               setAlpha(img, screen.color(r, g, b), 0);
            }
         }
      }
   }

   private boolean parseOcto(String [] properties, WorldModel world)
   {
      if (properties.length == OCTO_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[OCTO_COL]),
                 Integer.parseInt(properties[OCTO_ROW]));
         Entity entity = Entity.createOctoNotFull(properties[OCTO_ID],
                 Integer.parseInt(properties[OCTO_LIMIT]),
                 pt,
                 Integer.parseInt(properties[OCTO_ACTION_PERIOD]),
                 Integer.parseInt(properties[OCTO_ANIMATION_PERIOD]),
                 getImageList(OCTO_KEY));
         world.tryAddEntity(entity);
      }
      return properties.length == OCTO_NUM_PROPERTIES;
   }

   private boolean parseObstacle(String [] properties, WorldModel world)
   {
      if (properties.length == OBSTACLE_NUM_PROPERTIES)
      {
         Point pt = new Point(
                 Integer.parseInt(properties[OBSTACLE_COL]),
                 Integer.parseInt(properties[OBSTACLE_ROW]));
         Entity entity = Entity.createObstacle(properties[OBSTACLE_ID],
                 pt, getImageList(OBSTACLE_KEY));
         world.tryAddEntity(entity);
      }
      return properties.length == OBSTACLE_NUM_PROPERTIES;
   }

   private boolean parseFish(String [] properties, WorldModel world)
   {
      if (properties.length == FISH_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[FISH_COL]),
                 Integer.parseInt(properties[FISH_ROW]));
         Entity entity = Entity.createFish(properties[FISH_ID],
                 pt, Integer.parseInt(properties[FISH_ACTION_PERIOD]),
                 getImageList(FISH_KEY));
         world.tryAddEntity(entity);
      }
      return properties.length == FISH_NUM_PROPERTIES;
   }

   private boolean parseBackground(String [] properties, WorldModel world)
   {
      if (properties.length == BGND_NUM_PROPERTIES) {
         Point pt = new Point(Integer.parseInt(properties[BGND_COL]), Integer.parseInt(properties[BGND_ROW]));
         String id = properties[BGND_ID];
         world.setBackground(pt, new Background(id, getImageList(id)));
      }
      return properties.length == BGND_NUM_PROPERTIES;
   }

   private boolean parseAtlantis(String [] properties, WorldModel world)
   {
      if (properties.length == ATLANTIS_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[ATLANTIS_COL]),
                 Integer.parseInt(properties[ATLANTIS_ROW]));
         Entity entity = Entity.createAtlantis(properties[ATLANTIS_ID],
                 pt, getImageList(ATLANTIS_KEY));
         world.tryAddEntity(entity);
      }
      return properties.length == ATLANTIS_NUM_PROPERTIES;
   }

   private boolean parseSgrass(String [] properties, WorldModel world)
   {
      if (properties.length == SGRASS_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[SGRASS_COL]),
                 Integer.parseInt(properties[SGRASS_ROW]));
         Entity entity = Entity.createSgrass(properties[SGRASS_ID],
                 pt,
                 Integer.parseInt(properties[SGRASS_ACTION_PERIOD]),
                 getImageList(SGRASS_KEY));
         world.tryAddEntity(entity);
      }
      return properties.length == SGRASS_NUM_PROPERTIES;
   }
   public void loadImages(String filename, PApplet screen)
   {
      try
      {
         Scanner in = new Scanner(new File(filename));
         this.loadImages(in, screen);
      }
      catch (FileNotFoundException e)
      {
         System.err.println(e.getMessage());
      }
   }
}
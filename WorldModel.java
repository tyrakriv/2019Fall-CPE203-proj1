import processing.core.PImage;

import java.util.*;

/*
WorldModel ideally keeps track of the actual size of our grid world and what is in that world
in terms of entities and background elements
 */

final class WorldModel {
   private int numRows;
   private int numCols;
   private Background background[][];
   private Entity occupancy[][];
   private Set<Entity> entities;
   private final int FISH_REACH = 1;

   public WorldModel(int numRows, int numCols, Background defaultBackground) {
      this.numRows = numRows;
      this.numCols = numCols;
      this.background = new Background[numRows][numCols];
      this.occupancy = new Entity[numRows][numCols];
      this.entities = new HashSet<>();

      for (int row = 0; row < numRows; row++) {
         Arrays.fill(this.background[row], defaultBackground);
      }
   }

   public Set<Entity> getEntities() {
      return entities;
   }

   public int getNumCols() {
      return numCols;
   }

   public int getNumRows() {
      return numRows;
   }

   public Optional<Point> findOpenAround(Point pos) {
      for (int dy = -FISH_REACH; dy <= FISH_REACH; dy++) {
         for (int dx = -FISH_REACH; dx <= FISH_REACH; dx++) {
            Point newPt = new Point(pos.x + dx, pos.y + dy);
            if (withinBounds(newPt) &&
                    !isOccupied(newPt)) {
               return Optional.of(newPt);
            }
         }
      }
      return Optional.empty();
   }

   public boolean withinBounds(Point pos) {
      return pos.y >= 0 && pos.y < this.numRows &&
              pos.x >= 0 && pos.x < this.numCols;
   }

   public boolean isOccupied(Point pos) {
      return withinBounds(pos) &&
              getOccupancyCell(pos) != null;
   }

   private Optional<Entity> nearestEntity(List<Entity> entities, Point pos) {
      List<Entity> entitiesList = new ArrayList<Entity>(entities);

      if (entitiesList.isEmpty()) {
         return Optional.empty();
      } else {
         Entity nearest = entitiesList.get(0);
         int nearestDistance = pos.distanceSquared(nearest.getPosition());

         for (Entity other : entities) {
            int otherDistance = other.getPosition().distanceSquared(pos);

            if (otherDistance < nearestDistance) {
               nearest = other;
               nearestDistance = otherDistance;
            }
         }
         return Optional.of(nearest);
      }
   }


   public Optional<PImage> getBackgroundImage(Point pos) {
      if (withinBounds(pos)) {
         return Optional.of(getBackgroundCell(pos).getCurrentImage());
      } else {
         return Optional.empty();
      }
   }

   public void setBackground(Point pos, Background background) {
      if (withinBounds(pos)) {
         setBackgroundCell(pos, background);
      }
   }

   public Optional<Entity> getOccupant(Point pos) {
      if (isOccupied(pos)) {
         return Optional.of(getOccupancyCell(pos));
      } else {
         return Optional.empty();
      }
   }

   private Background getBackgroundCell(Point pos) {
      return this.background[pos.y][pos.x];
   }

   private void setBackgroundCell(Point pos, Background background) {
      this.background[pos.y][pos.x] = background;
   }

   public Entity getOccupancyCell(Point pos) {
      return this.occupancy[pos.y][pos.x];
   }

   public void setOccupancyCell(Point pos, Entity entity) {
      this.occupancy[pos.y][pos.x] = entity;
   }

   public Optional<Entity> findNearest(Point pos, EntityKind kind) {
      List<Entity> ofType = new LinkedList<>();
      for (Entity entity : this.entities) {
         if (entity.getKind() == kind) {
            ofType.add(entity);
         }
      }
      return nearestEntity(ofType, pos);
   }

   /*
      Assumes that there is no entity currently occupying the
      intended destination cell.
   */
   public void addEntity(Entity entity) {
      if (withinBounds(entity.getPosition())) {
         setOccupancyCell(entity.getPosition(), entity);
         this.entities.add(entity);
      }
   }

   private void moveEntity(Entity entity, Point pos) {
      Point oldPos = entity.getPosition();
      if (withinBounds(pos) && !pos.equals(oldPos)) {
         setOccupancyCell(oldPos, null);
         entity.removeEntityAt(this, pos);
         setOccupancyCell(pos, entity);
         entity.setPosition(pos);
      }
   }

   public boolean moveToFull(Entity octo, Entity target, EventScheduler scheduler) {
      if (octo.getPosition().adjacent(target.getPosition())) {
         return true;
      } else {
         Point nextPos = octo.nextPositionOcto(this, target.getPosition());

         if (!octo.getPosition().equals(nextPos)) {
            Optional<Entity> occupant = getOccupant(nextPos);
            if (occupant.isPresent()) {
               scheduler.unscheduleAllEvents(occupant.get());
            }

            moveEntity(octo, nextPos);
         }
         return false;
      }
   }

   public boolean moveToNotFull(Entity octo, Entity target, EventScheduler scheduler) {
      if (octo.getPosition().adjacent(target.getPosition())) {
         octo.setResourceCount(octo.getResourceCount() + 1);
         target.removeEntity(this, target);
         scheduler.unscheduleAllEvents(target);

         return true;
      } else {
         Point nextPos = octo.nextPositionOcto(this, target.getPosition());

         if (!octo.getPosition().equals(nextPos)) {
            Optional<Entity> occupant = getOccupant(nextPos);
            if (occupant.isPresent()) {
               scheduler.unscheduleAllEvents(occupant.get());
            }

            moveEntity(octo, nextPos);
         }
         return false;
      }
   }

   public boolean moveToCrab(Entity crab, Entity target, EventScheduler scheduler) {
      if (crab.getPosition().adjacent(target.getPosition())) {
         target.removeEntity(this, target);
         scheduler.unscheduleAllEvents(target);
         return true;
      } else {
         Point nextPos = crab.nextPositionCrab(this, target.getPosition());

         if (!crab.getPosition().equals(nextPos)) {
            Optional<Entity> occupant = getOccupant(nextPos);
            if (occupant.isPresent()) {
               scheduler.unscheduleAllEvents(occupant.get());
            }

            moveEntity(crab, nextPos);
         }
         return false;
      }
   }

   public void tryAddEntity(Entity entity) {
      if (isOccupied(entity.getPosition())) {
         // arguably the wrong type of exception, but we are not
         // defining our own exceptions yet
         throw new IllegalArgumentException("position occupied");
      }
      addEntity(entity);
   }


   public void transformFull(Entity entity, EventScheduler scheduler, ImageStore imageStore) {
      Entity octo = entity.createOctoNotFull(entity.getId(), entity.getResourceLimit(),
              entity.getPosition(), entity.getActionPeriod(), entity.getAnimationPeriod(),
              entity.getImages());

      entity.removeEntity(this, entity);
      scheduler.unscheduleAllEvents(entity);

      addEntity(octo);
      scheduler.scheduleActions(octo, this, imageStore);
   }

   public boolean transformNotFull(Entity entity, EventScheduler scheduler, ImageStore imageStore) {
      if (entity.getResourceCount() >= entity.getResourceLimit()) {
         Entity octo = entity.createOctoFull(entity.getId(), entity.getResourceLimit(),
                 entity.getPosition(), entity.getActionPeriod(), entity.getAnimationPeriod(),
                 entity.getImages());

         entity.removeEntity(this, entity);
         scheduler.unscheduleAllEvents(entity);

         addEntity(octo);
         scheduler.scheduleActions(octo, this, imageStore);

         return true;
      }

      return false;
   }
}